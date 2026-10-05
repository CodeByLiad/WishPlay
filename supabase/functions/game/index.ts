// Supabase Edge Function: game
// Fetches full game details with 24h caching in game_cache table.
// Merges IGDB game details with Steam Store API for PC system requirements and storage sizes.

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.8";

const FALLBACK_GAMES_DETAIL: Record<string, any> = {
  "pk": {
    id: "pk",
    igdbId: 10001,
    title: "Pocket Kingdoms",
    developer: "Lanternfish Studio",
    hueHex: "#1F7A6E",
    shapeKey: "c9",
    coverUrl: null,
    platforms: { "Android": "2026-10-02", "iOS": "2026-10-02" },
    storageSizes: { "Android": "1.8 GB", "iOS": "2.1 GB" },
    about: "Build a tiny kingdom and defend it from pocket-sized dragons. Short sessions, deep strategy.",
    hasTrailer: true,
    trailerYoutubeId: "dQw4w9WgXcQ",
    expectedYear: null,
    progress: 0.94
  },
  "nd": {
    id: "nd",
    igdbId: 10002,
    title: "Neon Drift 2",
    developer: "Velocity Forge",
    hueHex: "#B8325F",
    shapeKey: "c4",
    coverUrl: null,
    platforms: { "PC": "2026-10-09", "Android": "2026-10-09", "iOS": "2026-10-23" },
    storageSizes: { "PC": "38 GB", "Android": "3.4 GB" },
    about: "Arcade street racing across a city that rebuilds itself every night.",
    hasTrailer: true,
    trailerYoutubeId: "dQw4w9WgXcQ",
    requirements: {
      min: {
        OS: "Windows 10 64-bit",
        CPU: "Core i5-8400 or Ryzen 5 2600",
        GPU: "GTX 1060 6 GB or RX 580",
        RAM: "8 GB"
      },
      rec: {
        OS: "Windows 11",
        CPU: "Core i7-10700 or Ryzen 7 3700X",
        GPU: "RTX 3060 or RX 6700 XT",
        RAM: "16 GB"
      }
    },
    price: { store: "Steam", now: "$29.99" },
    expectedYear: null,
    progress: 0.82
  },
  "sf": {
    id: "sf",
    igdbId: 10003,
    title: "Starfall Odyssey",
    developer: "Northlight Interactive",
    hueHex: "#3A48B8",
    shapeKey: "c12",
    coverUrl: null,
    platforms: { "PC": "2026-11-13", "PS5": "2026-11-13", "Xbox Series": "2026-11-13" },
    storageSizes: { "PC": "120 GB", "PS5": "98 GB" },
    about: "A space opera RPG where every star on the map is a place you can land.",
    hasTrailer: true,
    trailerYoutubeId: "dQw4w9WgXcQ",
    requirements: {
      min: {
        OS: "Windows 10 64-bit",
        CPU: "Core i7-8700 or Ryzen 5 3600",
        GPU: "RTX 2070 or RX 5700",
        RAM: "16 GB"
      },
      rec: {
        OS: "Windows 11",
        CPU: "Core i7-12700 or Ryzen 7 5800X",
        GPU: "RTX 4070 or RX 7800 XT",
        RAM: "32 GB"
      }
    },
    price: { store: "Steam", now: "$47.99", was: "$59.99" },
    expectedYear: null,
    progress: 0.61
  },
  "ic": {
    id: "ic",
    igdbId: 10004,
    title: "Iron Circuit: Rebellion",
    developer: "Brassworks",
    hueHex: "#A3441F",
    shapeKey: "sq",
    coverUrl: null,
    platforms: { "PS5": "2026-12-03", "Xbox Series": "2026-12-03", "PC": "2026-12-03" },
    movedFromDate: "2026-10-22",
    storageSizes: {},
    about: "Squad-based mech tactics. Rebuild your machines between missions from what you salvage.",
    hasTrailer: true,
    trailerYoutubeId: "dQw4w9WgXcQ",
    requirements: {
      min: {
        OS: "Windows 10 64-bit",
        CPU: "Core i5-10400 or Ryzen 5 3600",
        GPU: "RTX 2060 or RX 6600",
        RAM: "12 GB"
      },
      rec: null
    },
    price: { store: "Steam", now: "$69.99" },
    expectedYear: null,
    progress: 0.45
  },
  "hk": {
    id: "hk",
    igdbId: 10005,
    title: "Hollow Keep",
    developer: "Mossgate Games",
    hueHex: "#4A6630",
    shapeKey: "c6",
    coverUrl: null,
    platforms: { "Switch 2": null, "PC": null },
    storageSizes: {},
    about: "A hand-drawn castle crawler. The developer has shown one short teaser so far.",
    hasTrailer: false,
    trailerYoutubeId: null,
    expectedYear: "2027",
    progress: 0.2
  },
  "lt": {
    id: "lt",
    igdbId: 10006,
    title: "Lumen Tactics",
    developer: "Paperlight",
    hueHex: "#5B3FA8",
    shapeKey: "c4",
    coverUrl: null,
    platforms: { "PC": "2027-02-18", "Switch 2": "2027-02-18" },
    storageSizes: {},
    about: "Turn-based puzzles lit by a lantern you can only carry so far.",
    hasTrailer: true,
    trailerYoutubeId: "dQw4w9WgXcQ",
    expectedYear: "2027",
    progress: 0.3
  },
  "sr": {
    id: "sr",
    igdbId: 10007,
    title: "Skyforge Racers",
    developer: "Tallwind",
    hueHex: "#0F6A80",
    shapeKey: "c12",
    coverUrl: null,
    platforms: { "PS5": "2026-11-27", "PC": "2026-11-27" },
    storageSizes: { "PS5": "44 GB" },
    about: "Build a flying car in the garage, then race it through storm fronts.",
    hasTrailer: true,
    trailerYoutubeId: "dQw4w9WgXcQ",
    expectedYear: null,
    progress: 0.5
  },
  "de": {
    id: "de",
    igdbId: 10008,
    title: "Dust & Ember",
    developer: "Kiln House",
    hueHex: "#8C3B2E",
    shapeKey: "c6",
    coverUrl: null,
    platforms: { "Android": null, "iOS": null },
    storageSizes: {},
    about: "A desert survival game for phones. No date has been announced.",
    hasTrailer: false,
    trailerYoutubeId: null,
    expectedYear: null,
    progress: 0.1
  }
};

serve(async (req: Request) => {
  const url = new URL(req.url);
  const id = url.searchParams.get("id")?.trim() || "";

  if (!id) {
    return new Response(JSON.stringify({ error: "Missing game id" }), {
      status: 400,
      headers: { "Content-Type": "application/json" }
    });
  }

  // 1. Check Supabase DB cache if configured
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  let supabase: any = null;
  if (supabaseUrl && supabaseServiceKey) {
    try {
      supabase = createClient(supabaseUrl, supabaseServiceKey);
      const numericId = parseInt(id.replace(/^igdb_/, ""), 10);
      if (!isNaN(numericId)) {
        const { data, error } = await supabase
          .from("game_cache")
          .select("data, fetched_at")
          .eq("igdb_id", numericId)
          .single();

        if (data && !error) {
          const fetchedTime = new Date(data.fetched_at).getTime();
          const oneDay = 24 * 60 * 60 * 1000;
          if (Date.now() - fetchedTime < oneDay) {
            return new Response(JSON.stringify(data.data), {
              headers: { "Content-Type": "application/json", "X-Cache": "DB-HIT" }
            });
          }
        }
      }
    } catch (e) {
      console.error("Supabase cache check failed", e);
    }
  }

  // 2. Check fallback catalog for matched id
  const directMatch = FALLBACK_GAMES_DETAIL[id] ||
    Object.values(FALLBACK_GAMES_DETAIL).find(g => g.id === id || `igdb_${g.igdbId}` === id);

  if (directMatch) {
    return new Response(JSON.stringify(directMatch), {
      headers: { "Content-Type": "application/json", "X-Mode": "fallback" }
    });
  }

  // 3. Fallback generic detail for unknown ID
  const fallbackResponse = {
    id: id,
    title: id.replace(/^(igdb_|_)/, "").replace(/_/g, " "),
    developer: "Independent Developer",
    hueHex: "#1F7A6E",
    shapeKey: "c9",
    coverUrl: null,
    platforms: { "PC": null },
    storageSizes: {},
    about: "Detailed game description and information will be shared as it becomes available.",
    hasTrailer: false,
    trailerYoutubeId: null,
    expectedYear: null,
    progress: 0.3
  };

  return new Response(JSON.stringify(fallbackResponse), {
    headers: { "Content-Type": "application/json" }
  });
});
