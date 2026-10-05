-- WishPlay Supabase Migration: Phase 2 game_cache
-- Table for caching 24h game data fetched from IGDB & Steam

CREATE TABLE IF NOT EXISTS public.game_cache (
    igdb_id BIGINT PRIMARY KEY,
    title TEXT NOT NULL,
    data JSONB NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE public.game_cache ENABLE ROW LEVEL SECURITY;

-- Read policy: Anyone with anon or authenticated key can read cached games
CREATE POLICY "Public read cached games"
    ON public.game_cache
    FOR SELECT
    USING (true);

-- No public write policy: Only Edge Functions with service_role can INSERT/UPDATE/DELETE
CREATE INDEX IF NOT EXISTS idx_game_cache_fetched_at ON public.game_cache(fetched_at);
