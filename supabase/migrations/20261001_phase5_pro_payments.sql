-- WishPlay Supabase Migration: Phase 5 Pro and Payments
-- Tables for entitlements, payments, promo codes, bKash submissions, audit log, and admins

-- 1. Entitlements Table
-- Written only by server (Edge Functions), never by the app session.
CREATE TABLE IF NOT EXISTS public.entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    plan TEXT NOT NULL DEFAULT 'free',                          -- 'free', 'pro'
    source TEXT NOT NULL,                                       -- 'paddle', 'bkash', 'code', 'admin'
    source_ref TEXT,                                            -- Paddle txn ID, promo code, etc.
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ DEFAULT NULL,
    CONSTRAINT unique_active_entitlement UNIQUE (user_id, plan)
);

ALTER TABLE public.entitlements ENABLE ROW LEVEL SECURITY;

-- Users can only read their own entitlements; server writes with service role
CREATE POLICY "Users can view their own entitlements"
    ON public.entitlements
    FOR SELECT
    USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_entitlements_user_id ON public.entitlements(user_id);

-- 2. Admins Table
-- Written by hand (or a bootstrap script) with service role; never from app session.
CREATE TABLE IF NOT EXISTS public.admins (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE
);

ALTER TABLE public.admins ENABLE ROW LEVEL SECURITY;

-- No SELECT policy intentionally -- admins are read only inside Edge Functions via service role.

-- 3. Payments Table (Paddle verified webhooks only)
CREATE TABLE IF NOT EXISTS public.payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paddle_txn_id TEXT UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount_cents INT NOT NULL,
    currency TEXT NOT NULL DEFAULT 'USD',
    status TEXT NOT NULL DEFAULT 'completed',   -- 'completed', 'refunded', 'chargeback'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;

-- Users can see their own payments
CREATE POLICY "Users can view their own payments"
    ON public.payments
    FOR SELECT
    USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_payments_user_id ON public.payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_paddle_txn ON public.payments(paddle_txn_id);

-- 4. Promo Codes Table (server only writes)
CREATE TABLE IF NOT EXISTS public.promo_codes (
    code TEXT PRIMARY KEY,
    type TEXT NOT NULL DEFAULT 'lifetime',          -- 'lifetime', 'discount'
    discount_pct INT NOT NULL DEFAULT 100,          -- 100 = free Pro, 20 = 20% off
    max_uses INT NOT NULL DEFAULT 1,
    uses INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ DEFAULT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.promo_codes ENABLE ROW LEVEL SECURITY;

-- No direct read policy for app sessions — codes are validated server-side.

-- 5. Code Redemptions (server only writes, unique per user+code)
CREATE TABLE IF NOT EXISTS public.code_redemptions (
    code TEXT NOT NULL REFERENCES public.promo_codes(code),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    redeemed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (code, user_id)
);

ALTER TABLE public.code_redemptions ENABLE ROW LEVEL SECURITY;

-- Users can read their own redemptions
CREATE POLICY "Users can view their own redemptions"
    ON public.code_redemptions
    FOR SELECT
    USING (auth.uid() = user_id);

-- 6. bKash Submissions
CREATE TABLE IF NOT EXISTS public.bkash_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trx_id TEXT UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount_bdt INT NOT NULL,
    sender_number TEXT NOT NULL,    -- masked before storage: e.g. 01978***129
    status TEXT NOT NULL DEFAULT 'pending',  -- 'pending', 'approved', 'rejected'
    reviewed_by UUID REFERENCES auth.users(id),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.bkash_submissions ENABLE ROW LEVEL SECURITY;

-- Users can insert their own submissions; read own submissions; server updates status
CREATE POLICY "Users can submit bKash"
    ON public.bkash_submissions
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can view their own bKash submissions"
    ON public.bkash_submissions
    FOR SELECT
    USING (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_bkash_user_id ON public.bkash_submissions(user_id);

-- 7. Audit Log (server only appends)
CREATE TABLE IF NOT EXISTS public.audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor UUID REFERENCES auth.users(id),           -- admin or server
    action TEXT NOT NULL,                           -- e.g. 'approve_bkash', 'create_code', 'revoke_pro'
    target TEXT,                                    -- user_id or code being acted on
    metadata JSONB,
    at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.audit_log ENABLE ROW LEVEL SECURITY;
-- No app session read/write — audit log is admin-only via service role.

-- Seed two demo promo codes (matches ProfileViewModel hardcoded codes)
INSERT INTO public.promo_codes (code, type, discount_pct, max_uses, uses, expires_at, active)
VALUES
    ('WP-GIFT-7K2Q', 'lifetime', 100, 1, 0, NULL, TRUE),
    ('WP-EID20',     'discount', 20,  500, 0, '2027-06-30T00:00:00Z', TRUE)
ON CONFLICT (code) DO NOTHING;
