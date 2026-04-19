-- ============================================================
-- Mindful Tennis — Subscriptions Table
-- Multi-sport ready: sport_id scopes each subscription row.
-- Apply via: Supabase Dashboard → SQL Editor, or MCP migration.
-- ============================================================

CREATE TABLE IF NOT EXISTS subscriptions (
    id                  TEXT PRIMARY KEY DEFAULT (extensions.uuid_generate_v4())::TEXT,
    user_id             TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sport_id            TEXT NOT NULL DEFAULT 'tennis',
    rc_customer_id      TEXT,
    status              TEXT NOT NULL CHECK (status IN (
                            'trial', 'active', 'expired', 'cancelled', 'grace_period'
                        )),
    plan                TEXT CHECK (plan IN ('monthly', 'quarterly', 'annual', 'lifetime')),
    is_trial            BOOLEAN NOT NULL DEFAULT FALSE,
    trial_ends_at       BIGINT,                          -- epoch ms
    current_period_end  BIGINT,                          -- epoch ms
    store               TEXT CHECK (store IN ('app_store', 'play_store')),
    created_at          BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT),
    updated_at          BIGINT NOT NULL DEFAULT ((EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT),
    UNIQUE(user_id, sport_id, store)
);

ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;

-- Users can only read/write their own subscription rows.
-- sport_id isolation at the DB level is enforced by the edge function on writes;
-- JWT-claim-based sport_id filtering can be layered on when multi-sport ships.
CREATE POLICY "Users can read own subscription"
    ON subscriptions FOR ALL
    USING (auth.uid()::TEXT = user_id);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_sport
    ON subscriptions(user_id, sport_id, status);
