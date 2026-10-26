-- =====================================================================
-- PriceScope  |  Migration 0001  |  Core schema
-- Package: com.pricescope.app
--
-- Rules followed:
--  * Additive only: CREATE ... IF NOT EXISTS / CREATE OR REPLACE.
--    No DROP TABLE, no TRUNCATE, no data deletion.
--    (Only RLS policies are dropped and recreated so the file can be re-run.)
--  * No invented prices, products, stores or countries are inserted.
--    The only seed rows are subscription plans and plan limits.
--  * Clients (anon / authenticated) can never write catalog, offers,
--    subscriptions, payment events or alert logs. Those are written only by
--    the server (service_role: Edge Functions / ingestion jobs).
--  * Future changes go in NEW files: 0002_*.sql, 0003_*.sql ...
--
-- Run: Supabase Dashboard > SQL Editor > paste whole file > Run.
-- =====================================================================

begin;

-- ---------------------------------------------------------------------
-- 0. Extensions and helpers
-- ---------------------------------------------------------------------
create extension if not exists pg_trgm with schema extensions;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = public
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

-- ---------------------------------------------------------------------
-- 1. Reference data (read-only for the app)
-- ---------------------------------------------------------------------

-- The app lists all ISO countries from the device. A row here only records
-- the data-coverage status of a country. No row = coverage not available yet.
create table if not exists public.countries (
  code            char(2) primary key,
  name_en         text not null,
  currency_code   char(3) not null,
  coverage_status text not null default 'soon'
                  check (coverage_status in ('full','limited','soon')),
  is_enabled      boolean not null default true,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);

create table if not exists public.currencies (
  code        char(3) primary key,
  name_en     text not null,
  minor_units smallint not null default 2 check (minor_units between 0 and 4),
  is_enabled  boolean not null default true,
  created_at  timestamptz not null default now()
);

-- Real exchange rates only, written by a server job. Never hard-coded.
create table if not exists public.exchange_rates (
  id             bigint generated always as identity primary key,
  base_currency  char(3) not null,
  quote_currency char(3) not null,
  rate           numeric(20,10) not null check (rate > 0),
  source         text not null,
  fetched_at     timestamptz not null,
  unique (base_currency, quote_currency, fetched_at)
);
create index if not exists exchange_rates_pair_idx
  on public.exchange_rates (base_currency, quote_currency, fetched_at desc);

-- Where price data comes from. Stays empty until a real, permitted source exists.
create table if not exists public.data_sources (
  id         uuid primary key default gen_random_uuid(),
  code       text not null unique,
  name       text not null,
  kind       text not null
             check (kind in ('official_api','affiliate','feed','partner','manual_test')),
  terms_url  text,
  status     text not null default 'planned'
             check (status in ('planned','active','disabled')),
  is_test    boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.stores (
  id           uuid primary key default gen_random_uuid(),
  name         text not null,
  country_code char(2),
  website_url  text,
  source_id    uuid references public.data_sources(id) on delete set null,
  is_test      boolean not null default false,
  created_at   timestamptz not null default now(),
  unique (source_id, name)
);

-- ---------------------------------------------------------------------
-- 2. Products and offers (written only by the server)
-- ---------------------------------------------------------------------
create table if not exists public.products (
  id         uuid primary key default gen_random_uuid(),
  name       text not null,
  brand      text,
  model      text,
  mpn        text,
  gtin       text,
  category   text,
  specs      jsonb not null default '{}'::jsonb,
  image_url  text,
  is_test    boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists products_name_trgm_idx
  on public.products using gin (name extensions.gin_trgm_ops);
create index if not exists products_brand_trgm_idx
  on public.products using gin (brand extensions.gin_trgm_ops);
create index if not exists products_model_trgm_idx
  on public.products using gin (model extensions.gin_trgm_ops);
create index if not exists products_gtin_idx on public.products (gtin) where gtin is not null;
create index if not exists products_mpn_idx  on public.products (mpn)  where mpn  is not null;

-- NULL means "unknown", never zero. Amounts are in the offer currency.
create table if not exists public.offers (
  id               uuid primary key default gen_random_uuid(),
  product_id       uuid not null references public.products(id) on delete cascade,
  store_id         uuid not null references public.stores(id) on delete cascade,
  source_id        uuid not null references public.data_sources(id) on delete restrict,
  source_offer_ref text,
  condition        text not null default 'new'
                   check (condition in ('new','used','refurbished')),
  price            numeric(14,2) not null check (price >= 0),
  currency         char(3) not null,
  shipping_cost    numeric(14,2) check (shipping_cost is null or shipping_cost >= 0),
  tax_amount       numeric(14,2) check (tax_amount is null or tax_amount >= 0),
  duties_amount    numeric(14,2) check (duties_amount is null or duties_amount >= 0),
  costs_estimated  boolean not null default false,
  ships_to         char(2)[],
  delivery_days_min smallint,
  delivery_days_max smallint,
  availability     text not null default 'unknown'
                   check (availability in ('in_stock','out_of_stock','unknown')),
  product_url      text not null,
  match_status     text not null default 'uncertain'
                   check (match_status in ('exact','similar','uncertain')),
  status           text not null default 'active'
                   check (status in ('active','stale','removed')),
  is_test          boolean not null default false,
  fetched_at       timestamptz not null,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);
create unique index if not exists offers_source_ref_uidx
  on public.offers (source_id, source_offer_ref) where source_offer_ref is not null;
create index if not exists offers_product_idx  on public.offers (product_id, status);
create index if not exists offers_store_idx    on public.offers (store_id);
create index if not exists offers_fetched_idx  on public.offers (fetched_at desc);
create index if not exists offers_ships_to_idx on public.offers using gin (ships_to);

-- Raw history, recorded automatically when a real offer is first stored or
-- its price changes. No back-filled or invented history.
create table if not exists public.price_history (
  id            bigint generated always as identity primary key,
  offer_id      uuid not null references public.offers(id) on delete cascade,
  price         numeric(14,2) not null,
  currency      char(3) not null,
  shipping_cost numeric(14,2),
  recorded_at   timestamptz not null default now()
);
create index if not exists price_history_offer_idx
  on public.price_history (offer_id, recorded_at desc);

-- Daily summary used after the raw-history retention window (default 90 days).
create table if not exists public.price_history_daily (
  offer_id  uuid not null references public.offers(id) on delete cascade,
  day       date not null,
  currency  char(3) not null,
  min_price numeric(14,2) not null,
  max_price numeric(14,2) not null,
  samples   integer not null default 1,
  primary key (offer_id, day, currency)
);

create or replace function public.offers_record_history()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if tg_op = 'INSERT'
     or new.price is distinct from old.price
     or new.currency is distinct from old.currency
     or new.shipping_cost is distinct from old.shipping_cost then
    insert into public.price_history (offer_id, price, currency, shipping_cost, recorded_at)
    values (new.id, new.price, new.currency, new.shipping_cost, new.fetched_at);
  end if;
  return new;
end;
$$;

create or replace trigger trg_offers_history
  after insert or update on public.offers
  for each row execute function public.offers_record_history();

-- Retention helper. NOT scheduled automatically. Call it from a server job
-- (Edge Function with service_role) when you decide to. Summarises raw rows
-- older than the window into price_history_daily, then removes those raw rows.
create or replace function public.prune_price_history(retain_days integer default 90)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  removed bigint;
begin
  if retain_days < 30 then
    raise exception 'retain_days must be at least 30';
  end if;

  insert into public.price_history_daily (offer_id, day, currency, min_price, max_price, samples)
  select offer_id, recorded_at::date, currency, min(price), max(price), count(*)
  from public.price_history
  where recorded_at < now() - make_interval(days => retain_days)
  group by offer_id, recorded_at::date, currency
  on conflict (offer_id, day, currency) do update
    set min_price = least(public.price_history_daily.min_price, excluded.min_price),
        max_price = greatest(public.price_history_daily.max_price, excluded.max_price),
        samples   = public.price_history_daily.samples + excluded.samples;

  with d as (
    delete from public.price_history
    where recorded_at < now() - make_interval(days => retain_days)
    returning 1
  )
  select count(*) into removed from d;

  return removed;
end;
$$;

-- ---------------------------------------------------------------------
-- 3. Subscription plans and limits
-- ---------------------------------------------------------------------
create table if not exists public.plan_limits (
  tier                  text primary key check (tier in ('free','plus','pro')),
  max_tracked_products  integer check (max_tracked_products is null or max_tracked_products >= 0),
  ads_enabled           boolean not null,
  price_alerts_enabled  boolean not null,
  history_days          integer check (history_days is null or history_days >= 0),
  updated_at            timestamptz not null default now()
);
comment on column public.plan_limits.max_tracked_products is
  'Distinct products across favorites + watchlist. NULL = no limit set yet (unlimited). Update when decided.';
comment on column public.plan_limits.history_days is
  'NULL = not decided yet.';

create table if not exists public.plans (
  code                    text primary key,
  tier                    text not null check (tier in ('free','plus','pro')),
  billing_period          text not null check (billing_period in ('none','week','month','year')),
  price_usd_reference     numeric(10,2),
  google_play_product_id  text,
  google_play_base_plan_id text,
  is_active               boolean not null default true,
  sort_order              integer not null default 0,
  created_at              timestamptz not null default now()
);
comment on column public.plans.price_usd_reference is
  'Proposed reference price only. The real price shown to users comes from Google Play / the payment provider.';

insert into public.plan_limits (tier, max_tracked_products, ads_enabled, price_alerts_enabled, history_days)
values
  ('free', 3,    true,  false, null),
  ('plus', null, false, true,  null),
  ('pro',  null, false, true,  null)
on conflict (tier) do nothing;

insert into public.plans (code, tier, billing_period, price_usd_reference, sort_order)
values
  ('free',         'free', 'none',  0,     0),
  ('plus_weekly',  'plus', 'week',  1.49,  10),
  ('plus_monthly', 'plus', 'month', 4.99,  11),
  ('plus_yearly',  'plus', 'year',  39.99, 12),
  ('pro_monthly',  'pro',  'month', 9.99,  20),
  ('pro_yearly',   'pro',  'year',  79.99, 21)
on conflict (code) do nothing;

-- ---------------------------------------------------------------------
-- 4. Users
-- ---------------------------------------------------------------------
create table if not exists public.profiles (
  user_id          uuid primary key references auth.users(id) on delete cascade,
  display_name     text,
  country_code     char(2),
  display_currency char(3),
  language         text check (language is null or language in ('en','zh','hi','es','ar','fr')),
  theme            text not null default 'system' check (theme in ('light','dark','system')),
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (user_id) values (new.id) on conflict (user_id) do nothing;
  return new;
end;
$$;

create or replace trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Profiles for users who signed up before this migration (safe: inserts only).
insert into public.profiles (user_id)
select id from auth.users
on conflict (user_id) do nothing;

create table if not exists public.favorites (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  product_id uuid not null references public.products(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (user_id, product_id)
);
create index if not exists favorites_user_idx on public.favorites (user_id);

create table if not exists public.watchlist (
  id               uuid primary key default gen_random_uuid(),
  user_id          uuid not null references auth.users(id) on delete cascade,
  product_id       uuid not null references public.products(id) on delete cascade,
  target_price     numeric(14,2) check (target_price is null or target_price > 0),
  target_currency  char(3),
  include_shipping boolean not null default false,
  alert_enabled    boolean not null default false,
  last_notified_at timestamptz,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  unique (user_id, product_id),
  check (target_price is null or target_currency is not null)
);
create index if not exists watchlist_user_idx on public.watchlist (user_id);
create index if not exists watchlist_alert_idx
  on public.watchlist (product_id) where alert_enabled;

-- Log of alerts the server has triggered. Written only by the server.
create table if not exists public.price_alerts (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references auth.users(id) on delete cascade,
  watchlist_id uuid references public.watchlist(id) on delete set null,
  offer_id     uuid references public.offers(id) on delete set null,
  price        numeric(14,2) not null,
  currency     char(3) not null,
  triggered_at timestamptz not null default now(),
  delivered_at timestamptz,
  read_at      timestamptz
);
create index if not exists price_alerts_user_idx
  on public.price_alerts (user_id, triggered_at desc);

create table if not exists public.search_history (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references auth.users(id) on delete cascade,
  query        text not null,
  country_code char(2),
  created_at   timestamptz not null default now()
);
create index if not exists search_history_user_idx
  on public.search_history (user_id, created_at desc);

create table if not exists public.saved_searches (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  name       text not null,
  query      text not null,
  filters    jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists saved_searches_user_idx on public.saved_searches (user_id);

create table if not exists public.push_tokens (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  token      text not null unique,
  platform   text not null default 'android',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists push_tokens_user_idx on public.push_tokens (user_id);

create table if not exists public.problem_reports (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid references auth.users(id) on delete set null,
  type        text not null
              check (type in ('wrong_price','wrong_product','shipping','app_bug','payment','other')),
  details     text not null check (char_length(details) between 1 and 4000),
  app_version text,
  status      text not null default 'open' check (status in ('open','reviewing','closed')),
  created_at  timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- 5. Subscriptions and payment events (server-written, never client-written)
-- ---------------------------------------------------------------------
-- user_id is SET NULL on account deletion so accounting records are kept.
create table if not exists public.subscriptions (
  id                       uuid primary key default gen_random_uuid(),
  user_id                  uuid references auth.users(id) on delete set null,
  plan_code                text not null references public.plans(code),
  tier                     text not null check (tier in ('plus','pro')),
  provider                 text not null
                           check (provider in ('google_play','paypal','stripe','paddle','manual')),
  status                   text not null
                           check (status in ('pending','active','grace_period','on_hold','paused','canceled','expired','refunded')),
  provider_customer_id     text,
  provider_subscription_id text,
  provider_purchase_token  text,
  current_period_start     timestamptz,
  current_period_end       timestamptz,
  auto_renew               boolean not null default false,
  cancel_at_period_end     boolean not null default false,
  canceled_at              timestamptz,
  last_verified_at         timestamptz,
  created_at               timestamptz not null default now(),
  updated_at               timestamptz not null default now()
);
create unique index if not exists subscriptions_provider_sub_uidx
  on public.subscriptions (provider, provider_subscription_id)
  where provider_subscription_id is not null;
create index if not exists subscriptions_user_idx on public.subscriptions (user_id, status);

-- Idempotency: the same provider event can never be processed twice.
create table if not exists public.payment_events (
  id                uuid primary key default gen_random_uuid(),
  provider          text not null,
  provider_event_id text not null,
  event_type        text not null,
  subscription_id   uuid references public.subscriptions(id) on delete set null,
  user_id           uuid,
  payload           jsonb not null default '{}'::jsonb,
  status            text not null default 'received'
                    check (status in ('received','processed','failed','ignored')),
  error             text,
  received_at       timestamptz not null default now(),
  processed_at      timestamptz,
  unique (provider, provider_event_id)
);

-- ---------------------------------------------------------------------
-- 6. Entitlements and limit enforcement (done in the database, not the UI)
-- ---------------------------------------------------------------------
create or replace function public.entitlements_for(uid uuid)
returns table (
  tier text,
  max_tracked_products integer,
  ads_enabled boolean,
  price_alerts_enabled boolean,
  history_days integer
)
language sql
stable
security definer
set search_path = public
as $$
  with t as (
    select coalesce(
      (select s.tier
         from public.subscriptions s
        where s.user_id = uid
          and s.status in ('active','grace_period')
          and (s.current_period_end is null or s.current_period_end > now())
        order by case s.tier when 'pro' then 1 else 2 end
        limit 1),
      'free') as tier
  )
  select l.tier, l.max_tracked_products, l.ads_enabled, l.price_alerts_enabled, l.history_days
  from public.plan_limits l
  join t on t.tier = l.tier;
$$;

revoke all on function public.entitlements_for(uuid) from public, anon, authenticated;
grant execute on function public.entitlements_for(uuid) to service_role;

create or replace function public.my_entitlements()
returns table (
  tier text,
  max_tracked_products integer,
  ads_enabled boolean,
  price_alerts_enabled boolean,
  history_days integer
)
language sql
stable
security definer
set search_path = public
as $$
  select * from public.entitlements_for((select auth.uid()));
$$;

revoke all on function public.my_entitlements() from public, anon;
grant execute on function public.my_entitlements() to authenticated, service_role;

create or replace function public.enforce_tracking_limit()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  lim     integer;
  cnt     integer;
  already boolean;
begin
  perform pg_advisory_xact_lock(hashtext(new.user_id::text));

  select e.max_tracked_products into lim from public.entitlements_for(new.user_id) e;
  if lim is null then
    return new;
  end if;

  select exists (
    select 1 from public.favorites where user_id = new.user_id and product_id = new.product_id
    union all
    select 1 from public.watchlist where user_id = new.user_id and product_id = new.product_id
  ) into already;
  if already then
    return new;
  end if;

  select count(*) into cnt from (
    select product_id from public.favorites where user_id = new.user_id
    union
    select product_id from public.watchlist where user_id = new.user_id
  ) x;

  if cnt >= lim then
    raise exception 'tracking_limit_reached' using errcode = 'P0001', hint = 'upgrade_required';
  end if;
  return new;
end;
$$;

create or replace trigger trg_favorites_limit
  before insert on public.favorites
  for each row execute function public.enforce_tracking_limit();

create or replace trigger trg_watchlist_limit
  before insert on public.watchlist
  for each row execute function public.enforce_tracking_limit();

create or replace function public.enforce_alert_entitlement()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  allowed boolean;
begin
  if new.alert_enabled
     and (tg_op = 'INSERT' or old.alert_enabled is distinct from new.alert_enabled) then
    select e.price_alerts_enabled into allowed from public.entitlements_for(new.user_id) e;
    if not coalesce(allowed, false) then
      raise exception 'alerts_not_available' using errcode = 'P0001', hint = 'upgrade_required';
    end if;
  end if;
  return new;
end;
$$;

create or replace trigger trg_watchlist_alert_entitlement
  before insert or update on public.watchlist
  for each row execute function public.enforce_alert_entitlement();

-- ---------------------------------------------------------------------
-- 7. updated_at triggers
-- ---------------------------------------------------------------------
create or replace trigger trg_countries_updated     before update on public.countries     for each row execute function public.set_updated_at();
create or replace trigger trg_products_updated      before update on public.products      for each row execute function public.set_updated_at();
create or replace trigger trg_offers_updated        before update on public.offers        for each row execute function public.set_updated_at();
create or replace trigger trg_plan_limits_updated   before update on public.plan_limits   for each row execute function public.set_updated_at();
create or replace trigger trg_profiles_updated      before update on public.profiles      for each row execute function public.set_updated_at();
create or replace trigger trg_watchlist_updated     before update on public.watchlist     for each row execute function public.set_updated_at();
create or replace trigger trg_push_tokens_updated   before update on public.push_tokens   for each row execute function public.set_updated_at();
create or replace trigger trg_subscriptions_updated before update on public.subscriptions for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------
-- 8. Row Level Security
-- ---------------------------------------------------------------------
alter table public.countries           enable row level security;
alter table public.currencies          enable row level security;
alter table public.exchange_rates      enable row level security;
alter table public.data_sources        enable row level security;
alter table public.stores              enable row level security;
alter table public.products            enable row level security;
alter table public.offers              enable row level security;
alter table public.price_history       enable row level security;
alter table public.price_history_daily enable row level security;
alter table public.plan_limits         enable row level security;
alter table public.plans               enable row level security;
alter table public.profiles            enable row level security;
alter table public.favorites           enable row level security;
alter table public.watchlist           enable row level security;
alter table public.price_alerts        enable row level security;
alter table public.search_history      enable row level security;
alter table public.saved_searches      enable row level security;
alter table public.push_tokens         enable row level security;
alter table public.problem_reports     enable row level security;
alter table public.subscriptions       enable row level security;
alter table public.payment_events      enable row level security;

-- 8a. Public reference data: read-only for everyone
do $$
declare t text;
begin
  foreach t in array array['countries','currencies','exchange_rates','plan_limits'] loop
    execute format('drop policy if exists %I on public.%I', t || '_read', t);
    execute format('create policy %I on public.%I for select to anon, authenticated using (true)', t || '_read', t);
  end loop;
end $$;

drop policy if exists plans_read on public.plans;
create policy plans_read on public.plans
  for select to anon, authenticated using (is_active);

-- 8b. Catalog: read-only, test rows are never visible to the app
drop policy if exists data_sources_read on public.data_sources;
create policy data_sources_read on public.data_sources
  for select to anon, authenticated using (not is_test);

drop policy if exists stores_read on public.stores;
create policy stores_read on public.stores
  for select to anon, authenticated using (not is_test);

drop policy if exists products_read on public.products;
create policy products_read on public.products
  for select to anon, authenticated using (not is_test);

drop policy if exists offers_read on public.offers;
create policy offers_read on public.offers
  for select to anon, authenticated using (not is_test and status <> 'removed');

drop policy if exists price_history_read on public.price_history;
create policy price_history_read on public.price_history
  for select to anon, authenticated
  using (exists (select 1 from public.offers o where o.id = offer_id and not o.is_test));

drop policy if exists price_history_daily_read on public.price_history_daily;
create policy price_history_daily_read on public.price_history_daily
  for select to anon, authenticated
  using (exists (select 1 from public.offers o where o.id = offer_id and not o.is_test));

-- 8c. User-owned data: only the owner
do $$
declare t text;
begin
  foreach t in array array['favorites','watchlist','search_history','saved_searches','push_tokens'] loop
    execute format('drop policy if exists %I on public.%I', t || '_own', t);
    execute format(
      'create policy %I on public.%I for all to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()))',
      t || '_own', t);
  end loop;
end $$;

drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own on public.profiles
  for select to authenticated using (user_id = (select auth.uid()));
drop policy if exists profiles_insert_own on public.profiles;
create policy profiles_insert_own on public.profiles
  for insert to authenticated with check (user_id = (select auth.uid()));
drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles
  for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

drop policy if exists price_alerts_select_own on public.price_alerts;
create policy price_alerts_select_own on public.price_alerts
  for select to authenticated using (user_id = (select auth.uid()));

drop policy if exists problem_reports_select_own on public.problem_reports;
create policy problem_reports_select_own on public.problem_reports
  for select to authenticated using (user_id = (select auth.uid()));
drop policy if exists problem_reports_insert_own on public.problem_reports;
create policy problem_reports_insert_own on public.problem_reports
  for insert to authenticated with check (user_id = (select auth.uid()));

drop policy if exists subscriptions_select_own on public.subscriptions;
create policy subscriptions_select_own on public.subscriptions
  for select to authenticated using (user_id = (select auth.uid()));
-- payment_events: RLS on, no policies, no grants => unreachable from the app.

-- ---------------------------------------------------------------------
-- 9. Privileges (defence in depth on top of RLS)
-- ---------------------------------------------------------------------
revoke insert, update, delete, truncate on table
  public.countries, public.currencies, public.exchange_rates, public.data_sources,
  public.stores, public.products, public.offers, public.price_history,
  public.price_history_daily, public.plan_limits, public.plans
  from anon, authenticated;

revoke all on table
  public.profiles, public.favorites, public.watchlist, public.price_alerts,
  public.search_history, public.saved_searches, public.push_tokens, public.problem_reports
  from anon;

revoke insert, update, delete, truncate on table public.price_alerts from authenticated;
revoke update, delete, truncate on table public.problem_reports from authenticated;
revoke delete, truncate on table public.profiles from authenticated;

-- Subscriptions: the app may read only non-secret columns of its own rows.
revoke all on table public.subscriptions from anon, authenticated;
grant select (
  id, user_id, plan_code, tier, provider, status,
  current_period_start, current_period_end,
  auto_renew, cancel_at_period_end, canceled_at,
  last_verified_at, created_at, updated_at
) on public.subscriptions to authenticated;

revoke all on table public.payment_events from anon, authenticated;

revoke all on function public.prune_price_history(integer) from public, anon, authenticated;
grant execute on function public.prune_price_history(integer) to service_role;

commit;

-- =====================================================================
-- After running, verify:
--   select tier, max_tracked_products, ads_enabled, price_alerts_enabled from public.plan_limits;
--   select code, tier, billing_period from public.plans order by sort_order;
-- Expected: 3 rows in plan_limits, 6 rows in plans, and no rows in any
-- catalog table (products, offers, stores, data_sources).
-- =====================================================================
