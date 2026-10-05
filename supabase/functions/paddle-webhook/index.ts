// Supabase Edge Function: paddle-webhook
// Verifies Paddle signature, grants/revokes Pro entitlements, logs payments.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.8";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, paddle-signature",
};

// Expected: $2.99 USD or within ±$0.01 tolerance
const EXPECTED_AMOUNT_CENTS = 299;
const EXPECTED_CURRENCY = "USD";

async function verifyPaddleSignature(
  rawBody: string,
  signatureHeader: string,
  webhookSecret: string
): Promise<boolean> {
  // Paddle-Signature header: ts=<timestamp>;h1=<hex_hmac>
  const parts = Object.fromEntries(signatureHeader.split(";").map(p => p.split("=")));
  const ts = parts["ts"];
  const h1 = parts["h1"];
  if (!ts || !h1) return false;

  // Signed payload: "<timestamp>:<rawBody>"
  const signedPayload = `${ts}:${rawBody}`;
  const keyData = new TextEncoder().encode(webhookSecret);
  const msgData = new TextEncoder().encode(signedPayload);

  const cryptoKey = await crypto.subtle.importKey(
    "raw", keyData, { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
  );
  const sig = await crypto.subtle.sign("HMAC", cryptoKey, msgData);
  const hexSig = Array.from(new Uint8Array(sig)).map(b => b.toString(16).padStart(2, "0")).join("");

  return hexSig === h1;
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const webhookSecret = Deno.env.get("PADDLE_WEBHOOK_SECRET") ?? "";
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
    const adminSupa = createClient(supabaseUrl, serviceKey);

    const rawBody = await req.text();
    const signatureHeader = req.headers.get("Paddle-Signature") ?? "";

    // Verify signature
    const valid = await verifyPaddleSignature(rawBody, signatureHeader, webhookSecret);
    if (!valid) {
      await adminSupa.from("audit_log").insert({
        action: "paddle_signature_fail",
        metadata: { header: signatureHeader.slice(0, 40) }
      });
      return new Response(JSON.stringify({ error: "Invalid signature" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const event = JSON.parse(rawBody);
    const eventType: string = event.event_type ?? "";

    // Extract transaction-level data
    const txnData = event.data ?? {};
    const txnId: string = txnData.id ?? "";
    const customUserId: string = txnData.custom_data?.user_id ?? "";
    const currency: string = txnData.currency_code ?? "";
    const amountCents: number = Math.round(parseFloat(txnData.details?.totals?.grand_total ?? "0") * 100);

    if (!txnId || !customUserId) {
      return new Response(JSON.stringify({ error: "Missing transaction data" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (eventType === "transaction.completed") {
      // Validate amount and currency
      if (currency !== EXPECTED_CURRENCY || amountCents < EXPECTED_AMOUNT_CENTS - 1) {
        await adminSupa.from("audit_log").insert({
          action: "paddle_amount_mismatch",
          target: customUserId,
          metadata: { currency, amountCents, txnId }
        });
        return new Response(JSON.stringify({ error: "Amount or currency mismatch" }), {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }

      // Idempotency: skip if txnId already processed
      const { data: existing } = await adminSupa
        .from("payments")
        .select("id")
        .eq("paddle_txn_id", txnId)
        .single();

      if (existing) {
        return new Response(JSON.stringify({ status: "already_processed" }), {
          headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
      }

      // Record payment
      await adminSupa.from("payments").insert({
        paddle_txn_id: txnId,
        user_id: customUserId,
        amount_cents: amountCents,
        currency: EXPECTED_CURRENCY,
        status: "completed"
      });

      // Grant Pro entitlement
      await adminSupa.from("entitlements").upsert({
        user_id: customUserId,
        plan: "pro",
        source: "paddle",
        source_ref: txnId,
        granted_at: new Date().toISOString(),
        revoked_at: null
      }, { onConflict: "user_id,plan" });

      await adminSupa.from("audit_log").insert({
        action: "grant_pro_paddle",
        target: customUserId,
        metadata: { txnId, amountCents, currency }
      });

    } else if (eventType === "transaction.refunded" || eventType === "transaction.chargeback") {
      // Revoke Pro
      await adminSupa.from("entitlements")
        .update({ revoked_at: new Date().toISOString() })
        .eq("user_id", customUserId)
        .eq("plan", "pro")
        .eq("source_ref", txnId);

      await adminSupa.from("payments")
        .update({ status: eventType === "transaction.refunded" ? "refunded" : "chargeback" })
        .eq("paddle_txn_id", txnId);

      await adminSupa.from("audit_log").insert({
        action: eventType === "transaction.refunded" ? "revoke_pro_refund" : "revoke_pro_chargeback",
        target: customUserId,
        metadata: { txnId }
      });
    }

    return new Response(JSON.stringify({ received: true }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" }
    });

  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
