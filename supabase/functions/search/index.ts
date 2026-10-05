// Supabase Edge Function: search
// Secure proxy for IGDB search & popular upcoming titles.
// Secrets held on server: TWITCH_CLIENT_ID, TWITCH_CLIENT_SECRET, SUPABASE_SERVICE_ROLE_KEY

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";

interface CachedSearch {
  timestamp: number;
  data: unknown;
}

const searchCache = new Map<string, CachedSearch>();
const CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour search cache

let twitchToken: { token: string; expiresAt: number } | null = null;

async function getTwitchToken(): Promise<string | null> {
  const clientId = Deno.env.get("TWITCH_CLIENT_ID");
  const clientSecret = Deno.env.get("TWITCH_CLIENT_SECRET");
  if (!clientId || !clientSecret) return null;

  if (twitchToken && Date.now() < twitchToken.expiresAt - 60000) {
    return twitchToken.token;
  }

  try {
    const res = await fetch(
      `https://id.twitch.tv/oauth2/token?client_id=${clientId}&client_secret=${clientSecret}&grant_type=client_credentials`,
      { method: "POST" }
    );
    if (!res.ok) return null;
    const json = await res.json();
    twitchToken = {
      token: json.access_token,
      expiresAt: Date.now() + (json.expires_in * 1000),
    };
    return twitchToken.token;
  } catch (err) {
    console.error("Failed to fetch Twitch token", err);
    return null;
  }
}

// Fallback catalog for development and testing
const FALLBACK_GAMES = [
  {
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
  {
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
  {
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
  {
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
  {
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
  {
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
  {
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
  {
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
];

const PLATFORM_ID_MAP: Record<string, number[]> = {
  "PC": [6],
  "PS5": [167],
  "Xbox Series": [169],
  "Switch 2": [130, 48],
  "Android": [34],
  "iOS": [39]
};

serve(async (req: Request) => {
  const url = new URL(req.url);
  const q = url.searchParams.get("q")?.trim() || "";
  const platform = url.searchParams.get("platform")?.trim() || "All";
  const limit = Math.min(parseInt(url.searchParams.get("limit") || "20"), 50);

  const cacheKey = `${q.toLowerCase()}:${platform}:${limit}`;
  const cached = searchCache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL_MS) {
    return new Response(JSON.stringify(cached.data), {
      headers: { "Content-Type": "application/json", "X-Cache": "HIT" }
    });
  }

  const clientId = Deno.env.get("TWITCH_CLIENT_ID");
  const token = await getTwitchToken();

  if (clientId && token) {
    try {
      let queryBody = `fields id, name, summary, storyline, cover.url, cover.image_id, first_release_date, platforms.name, platforms.id, release_dates.date, release_dates.human, release_dates.platform, release_dates.y, involved_companies.company.name, involved_companies.developer, videos.video_id, external_games.category, external_games.uid; limit ${limit};`;

      if (q) {
        queryBody += ` search "${q.replace(/"/g, '')}";`;
      } else {
        queryBody += ` where rating_count > 5 & first_release_date != null; sort first_release_date desc;`;
      }

      const igdbRes = await fetch("https://api.igdb.com/v4/games", {
        method: "POST",
        headers: {
          "Client-ID": clientId,
          "Authorization": `Bearer ${token}`,
          "Accept": "application/json"
        },
        body: queryBody
      });

      if (igdbRes.ok) {
        const rawGames = await igdbRes.json();
        const mapped = rawGames.map((g: any) => {
          const platformsMap: Record<string, string | null> = {};
          if (Array.isArray(g.release_dates)) {
            for (const rd of g.release_dates) {
              const pName = rd.platform === 6 ? "PC" :
                rd.platform === 167 ? "PS5" :
                rd.platform === 169 ? "Xbox Series" :
                (rd.platform === 130 || rd.platform === 48) ? "Switch 2" :
                rd.platform === 34 ? "Android" :
                rd.platform === 39 ? "iOS" : null;
              if (pName && !platformsMap[pName]) {
                platformsMap[pName] = rd.date ? new Date(rd.date * 1000).toISOString().split("T")[0] : null;
              }
            }
          }

          const devComp = g.involved_companies?.find((ic: any) => ic.developer);
          const dev = devComp?.company?.name || null;
          const cover = g.cover?.image_id ? `https://images.igdb.com/igdb/image/upload/t_cover_big/${g.cover.image_id}.jpg` : null;

          return {
            id: `igdb_${g.id}`,
            igdbId: g.id,
            title: g.name,
            developer: dev,
            hueHex: "#1F7A6E",
            shapeKey: "c9",
            coverUrl: cover,
            platforms: platformsMap,
            about: g.summary || g.storyline || null,
            hasTrailer: Array.isArray(g.videos) && g.videos.length > 0,
            trailerYoutubeId: g.videos?.[0]?.video_id || null,
            expectedYear: null,
            progress: 0.5
          };
        });

        const filtered = platform === "All" ? mapped : mapped.filter((m: any) => m.platforms.hasOwnProperty(platform));
        const result = { results: filtered };
        searchCache.set(cacheKey, { timestamp: Date.now(), data: result });
        return new Response(JSON.stringify(result), {
          headers: { "Content-Type": "application/json" }
        });
      }
    } catch (e) {
      console.error("IGDB search call failed, falling back to local dataset", e);
    }
  }

  // Fallback search execution:
  let pool = FALLBACK_GAMES;
  if (platform !== "All") {
    pool = pool.filter(g => g.platforms.hasOwnProperty(platform));
  }
  if (q) {
    const qLower = q.toLowerCase();
    pool = pool.filter(g => g.title.toLowerCase().includes(qLower));
  }
  const result = { results: pool.slice(0, limit) };
  searchCache.set(cacheKey, { timestamp: Date.now(), data: result });

  return new Response(JSON.stringify(result), {
    headers: { "Content-Type": "application/json", "X-Mode": "fallback" }
  });
});
