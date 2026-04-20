-- Migration: sync_log
-- Purpose: Record every background sync attempt for operational monitoring.
--          Query this table to answer: "Is sync healthy? Who is failing? How fast is sync?"
--
-- Written by SyncWorker (Android) and BackgroundSyncRunner (iOS) after each sync run.
-- Read via Supabase dashboard SQL editor or a future admin panel.

create table if not exists public.sync_log (
    id              uuid        primary key default gen_random_uuid(),
    user_id         uuid        not null references auth.users(id) on delete cascade,
    started_at      timestamptz not null,
    finished_at     timestamptz,
    duration_ms     int         generated always as (
                        extract(epoch from (finished_at - started_at)) * 1000
                    ) stored,
    success         boolean     not null default false,
    records_pushed  int         not null default 0,
    records_pulled  int         not null default 0,
    error_message   text,
    platform        text        not null check (platform in ('android', 'ios')),
    app_version     text        not null default '',
    attempt_number  smallint    not null default 1
);

-- Users should only see their own sync log (RLS mirrors the sessions table pattern)
alter table public.sync_log enable row level security;

create policy "Users can read own sync_log"
    on public.sync_log for select
    using (auth.uid() = user_id);

create policy "Users can insert own sync_log"
    on public.sync_log for insert
    with check (auth.uid() = user_id);

-- Index for the dashboard queries: "sync health by day" and "at-risk users"
create index sync_log_user_started_idx on public.sync_log (user_id, started_at desc);
create index sync_log_success_started_idx on public.sync_log (success, started_at desc);
