// Supabase Edge Function: bkash
// Handles bKash Pro purchase submission from the app.
// Validates input, masks sender number, and creates a pending submission.
// Admin reviews via the admin panel and approves/rejects separately.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.8";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const BKASH_AMOUNT_BDT = 349;
const BKASH_RECIPIENT = Deno.env.get("BKASH_NUMBER") ?? "01978900129";

function maskNumber(num: string): string {
  if (num.length < 6) return num;
  return num.slice(0, 5) + "***" + num.slice(-3);
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  // GET /bkash → return recipient number and amount (never hardcoded in APK)
  if (req.method === "GET") {
    return new Response(JSON.stringify({
      recipient: BKASH_RECIPIENT,
      amount_bdt: BKASH_AMOUNT_BDT
    }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } }
    });
    const { data: { user }, error: userError } = await userClient.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    const body = await req.json();
    const trxId: string = (body.trx_id ?? "").trim().toUpperCase();
    const senderNumber: string = (body.sender_number ?? "").trim();
    const amountBdt: number = parseInt(body.amount_bdt ?? "0", 10);

    // Validation
    if (!trxId || trxId.length < 6 || trxId.length > 30) {
      return new Response(JSON.stringify({ error: "Invalid TrxID." }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }
    if (!senderNumber.match(/^01[3-9]\d{8}$/)) {
      return new Response(JSON.stringify({ error: "Invalid Bangladesh mobile number." }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }
    if (amountBdt !== BKASH_AMOUNT_BDT) {
      return new Response(JSON.stringify({ error: `Amount must be ${BKASH_AMOUNT_BDT} BDT.` }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    const adminSupa = createClient(supabaseUrl, serviceKey);

    // Check TrxID uniqueness (prevent replay)
    const { data: existingTrx } = await adminSupa
      .from("bkash_submissions")
      .select("id")
      .eq("trx_id", trxId)
      .single();

    if (existingTrx) {
      return new Response(JSON.stringify({ error: "duplicate_trx", message: "This TrxID has already been submitted." }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // Rate limit: max 3 pending submissions per user
    const { count } = await adminSupa
      .from("bkash_submissions")
      .select("id", { count: "exact" })
      .eq("user_id", user.id)
      .eq("status", "pending");

    if ((count ?? 0) >= 3) {
      return new Response(JSON.stringify({
        error: "rate_limit",
        message: "You have too many pending submissions. Please wait for them to be reviewed."
      }), {
        status: 429, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // Insert submission with masked number
    await adminSupa.from("bkash_submissions").insert({
      trx_id: trxId,
      user_id: user.id,
      amount_bdt: amountBdt,
      sender_number: maskNumber(senderNumber),
      status: "pending"
    });

    await adminSupa.from("audit_log").insert({
      action: "bkash_submit",
      target: user.id,
      metadata: { trxId, amountBdt }
    });

    return new Response(JSON.stringify({
      success: true,
      message: "Submission received. Your payment will be reviewed within 24 hours. You will be notified once Pro is activated."
    }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });

  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }
});
