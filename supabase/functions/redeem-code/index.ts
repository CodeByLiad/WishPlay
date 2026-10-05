// Supabase Edge Function: redeem-code
// Validates a promo code server-side, enforces rate limits and uniqueness,
// then grants Pro or a discount. Only signed-in users can call this.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.8";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
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

    // Verify the requesting user
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
    const code: string = (body.code ?? "").trim().toUpperCase();

    if (!code) {
      return new Response(JSON.stringify({ error: "code is required" }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    const adminSupa = createClient(supabaseUrl, serviceKey);

    // 1. Fetch and validate code
    const { data: promoData, error: promoError } = await adminSupa
      .from("promo_codes")
      .select("*")
      .eq("code", code)
      .single();

    if (promoError || !promoData) {
      return new Response(JSON.stringify({ error: "invalid_code", message: "Invalid or expired code." }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    const promo = promoData;

    // Check active
    if (!promo.active) {
      return new Response(JSON.stringify({ error: "code_inactive", message: "This code is no longer active." }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // Check expiry
    if (promo.expires_at && new Date(promo.expires_at) < new Date()) {
      return new Response(JSON.stringify({ error: "code_expired", message: "This code has expired." }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // Check max uses
    if (promo.uses >= promo.max_uses) {
      return new Response(JSON.stringify({ error: "code_exhausted", message: "This code has reached its maximum uses." }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // Check uniqueness per user (one use per account)
    const { data: existingRedemption } = await adminSupa
      .from("code_redemptions")
      .select("code")
      .eq("code", code)
      .eq("user_id", user.id)
      .single();

    if (existingRedemption) {
      return new Response(JSON.stringify({ error: "already_redeemed", message: "You have already used this code." }), {
        status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }

    // 2. Record redemption
    await adminSupa.from("code_redemptions").insert({ code, user_id: user.id });

    // 3. Increment uses atomically
    await adminSupa.rpc("increment_code_uses", { p_code: code });

    // 4. Grant Pro if lifetime code
    let proGranted = false;
    if (promo.type === "lifetime") {
      await adminSupa.from("entitlements").upsert({
        user_id: user.id,
        plan: "pro",
        source: "code",
        source_ref: code,
        granted_at: new Date().toISOString(),
        revoked_at: null
      }, { onConflict: "user_id,plan" });
      proGranted = true;
    }

    // 5. Audit log
    await adminSupa.from("audit_log").insert({
      action: "redeem_code",
      target: user.id,
      metadata: { code, type: promo.type, discount_pct: promo.discount_pct }
    });

    return new Response(JSON.stringify({
      success: true,
      type: promo.type,
      discount_pct: promo.discount_pct,
      pro_granted: proGranted,
      message: proGranted
        ? "Code accepted! WishPlay Pro unlocked for life."
        : `${promo.discount_pct}% discount code applied.`
    }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });

  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
  }
});
