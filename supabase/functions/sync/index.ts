// Supabase Edge Function: sync
// Handles two-way sync for wishlist items with soft deletes and guest merge

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.8";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

interface SyncItem {
  game_id: string;
  igdb_id?: number | null;
  custom_title?: string | null;
  platforms?: Record<string, string>;
  notes?: string | null;
  alert_on?: boolean;
  price_alert_on?: boolean;
  order_index?: number;
  updated_at?: number; // epoch ms
  deleted_at?: number | null; // epoch ms if tombstone
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const authHeader = req.headers.get("Authorization");

    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing Authorization header" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const supabase = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: { user }, error: userError } = await supabase.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const body = await req.json();
    const lastSyncedAt = body.last_synced_at ? new Date(body.last_synced_at) : new Date(0);
    const clientItems: SyncItem[] = body.items || [];

    // 1. Process client mutations (upserts and soft deletes)
    for (const item of clientItems) {
      if (item.deleted_at) {
        // Soft delete on server
        await supabase
          .from("wishlist_items")
          .update({
            deleted_at: new Date(item.deleted_at).toISOString(),
            updated_at: new Date().toISOString(),
          })
          .eq("user_id", user.id)
          .eq("game_id", item.game_id);
      } else {
        // Upsert item
        await supabase
          .from("wishlist_items")
          .upsert({
            user_id: user.id,
            game_id: item.game_id,
            igdb_id: item.igdb_id,
            custom_title: item.custom_title,
            platforms: item.platforms || {},
            notes: item.notes,
            alert_on: item.alert_on ?? true,
            price_alert_on: item.price_alert_on ?? false,
            order_index: item.order_index ?? 0,
            updated_at: item.updated_at ? new Date(item.updated_at).toISOString() : new Date().toISOString(),
            deleted_at: null,
          }, { onConflict: "user_id, game_id" });
      }
    }

    // 2. Fetch server changes since lastSyncedAt
    const { data: serverItems, error: fetchError } = await supabase
      .from("wishlist_items")
      .select("*")
      .eq("user_id", user.id)
      .gt("updated_at", lastSyncedAt.toISOString());

    if (fetchError) {
      throw fetchError;
    }

    const currentTimestamp = Date.now();

    return new Response(
      JSON.stringify({
        synced_at: currentTimestamp,
        items: serverItems || [],
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      }
    );
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
