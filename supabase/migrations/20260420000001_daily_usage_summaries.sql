-- Migration: daily_usage_summaries
-- Purpose: One row per user per calendar day — a pre-aggregated digest of activity.
--          Powers WAU/MAU dashboards, streak tracking, and engagement nudges without
--          querying the raw sessions table on every dashboard load.
--
-- Populated by the nightly WorkManager / BGTaskScheduler task.
-- Can also be recomputed server-side via a pg_cron job if client reporting is unreliable.

create table if not exists public.daily_usage_summaries (
    user_id             uuid    not null references auth.users(id) on delete cascade,
    date                date    not null,
    sessions_recorded   int     not null default 0,
    total_minutes       int     not null default 0,
    sync_successes      int     not null default 0,
    sync_failures       int     not null default 0,
    primary key (user_id, date)
);

alter table public.daily_usage_summaries enable row level security;

create policy "Users can read own daily summaries"
    on public.daily_usage_summaries for select
    using (auth.uid() = user_id);

create policy "Users can upsert own daily summaries"
    on public.daily_usage_summaries for insert
    with check (auth.uid() = user_id);

create policy "Users can update own daily summaries"
    on public.daily_usage_summaries for update
    using (auth.uid() = user_id);

-- Index for WAU/MAU queries: "users active in last 7 days"
create index daily_usage_date_idx on public.daily_usage_summaries (date desc, user_id);


-- ── Dashboard queries (run in Supabase SQL editor) ──────────────────────────
--
-- Weekly Active Users (WAU):
--   SELECT COUNT(DISTINCT user_id) AS wau
--   FROM daily_usage_summaries
--   WHERE date >= CURRENT_DATE - INTERVAL '7 days';
--
-- Sync health by day (last 14 days):
--   SELECT date,
--          SUM(sync_successes) AS successes,
--          SUM(sync_failures)  AS failures,
--          ROUND(100.0 * SUM(sync_successes) /
--                NULLIF(SUM(sync_successes + sync_failures), 0), 2) AS success_rate_pct
--   FROM daily_usage_summaries
--   GROUP BY date ORDER BY date DESC LIMIT 14;
--
-- At-risk users (≥ 3 consecutive days with sync failures):
--   SELECT user_id, COUNT(*) AS failure_days
--   FROM daily_usage_summaries
--   WHERE sync_failures > 0 AND date >= CURRENT_DATE - INTERVAL '7 days'
--   GROUP BY user_id HAVING COUNT(*) >= 3;
