# Payment Integration — Status

Last updated: 2026-04-19

---

## Overall Progress

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Store & RevenueCat Setup | ✅ Done |
| 2 | Supabase Webhook | ✅ Done |
| 3 | SDK Integration (Android/iOS) | ✅ Done |
| 4 | Paywall UI | ✅ Done |
| 5 | Testing | ⬜ Not Started |

---

## Phase 1 — Store & RevenueCat Setup

**Status: ✅ Done**

- [x] Create RevenueCat account + organization
- [x] Create RevenueCat project for MindfulTennis → obtain API keys (Android + iOS)
- [x] App Store Connect: subscription group + 3 auto-renewing subscriptions + lifetime non-consumable + 3-day intro trial
- [x] Google Play Console: subscription + 3 base plans with 3-day trial + lifetime in-app product
- [x] Link both store apps to RevenueCat project
- [x] Create entitlements in RevenueCat: `premium_monthly`, `premium_quarterly`, `premium_annual`, `premium_lifetime`
- [x] Create an Offering with all 4 packages
- [x] Register webhook URL in RevenueCat → Integrations → Webhooks

**Product IDs (Tennis):**

| Plan | Product ID |
|------|-----------|
| Monthly | `mindful_tennis_monthly` |
| Quarterly | `mindful_tennis_quarterly` |
| Annual | `mindful_tennis_annual` |
| Lifetime | `mindful_tennis_lifetime` |

---

## Phase 2 — Supabase Webhook

**Status: ✅ Done**

- [x] `subscriptions` table created with multi-sport schema
  - `sport_id TEXT NOT NULL DEFAULT 'tennis'`
  - `UNIQUE(user_id, sport_id, store)`
  - RLS enabled — users read own rows only
  - Index on `(user_id, sport_id, status)`
- [x] Edge function `rc-webhook` deployed
  - URL: `https://ffdybinqhqsytaomitbr.supabase.co/functions/v1/rc-webhook`
  - JWT verification disabled (uses `REVENUECAT_WEBHOOK_SECRET` header instead)
  - Handles: `INITIAL_PURCHASE`, `TRIAL_STARTED`, `TRIAL_CONVERTED`, `RENEWAL`, `CANCELLATION`, `EXPIRATION`, `BILLING_ISSUE`, `UNCANCELLATION`
  - Parses `sport_id` from product ID (`mindful_tennis_monthly` → `tennis`)
  - Upserts via `user_id, sport_id, store` conflict key
- [x] `REVENUECAT_WEBHOOK_SECRET` set in Supabase Edge Function secrets
- [x] Webhook verified — RevenueCat test event received, 200 OK in 303ms

**Files:**
- `supabase_subscriptions.sql` — migration source
- `supabase/functions/rc-webhook/index.ts` — edge function

---

## Phase 3 — SDK Integration

**Status: ✅ Done**

- [x] Add RevenueCat KMP dependency to `gradle/libs.versions.toml` (`purchases-kmp` + `purchases-kmp-ui` v1.8.0)
- [x] Add to `shared/build.gradle.kts` commonMain dependencies
- [x] Initialize `Purchases` in Android `Application.onCreate()` with `BuildConfig.REVENUECAT_KEY`
- [x] Initialize `Purchases` in iOS via `PurchasesHelperKt.initPurchases()` called from `iOSApp.swift`
- [x] Add `REVENUECAT_KEY` to `Info.plist` (iOS) + `Secrets.xcconfig.template` (iOS) + `BuildConfig` (Android)
- [x] User identification: `SubscriptionRepositoryImpl` observes `authState` → calls `Purchases.logIn/logOut` automatically
- [x] Implement `SubscriptionRepository` (interface + `SubscriptionRepositoryImpl` in `commonMain`)
  - `subscriptionStatus: StateFlow<SubscriptionStatus>` — live entitlement state
  - `getCurrentOffering()` — fetches RC offering for Paywall
  - `restorePurchases()` — delegates to RC restore
  - `identifyUser()` / `resetUser()` — RC user identity
- [x] Implement `GetSubscriptionStatusUseCase`
- [x] Implement `RestorePurchasesUseCase`
- [x] Entitlement mapping: `premium_lifetime > premium_annual > premium_quarterly > premium_monthly`
  - Trial → `SubscriptionStatus.Trial(endsAt)`
  - Active → `SubscriptionStatus.Active`
  - None → `SubscriptionStatus.None`

**Still required before first run (manual):**
- [ ] Add `REVENUECAT_KEY_ANDROID = <key>` to `local.properties`
- [ ] Add `REVENUECAT_KEY = <key>` to `iosApp/Configuration/Secrets.xcconfig`

**Files created/modified:**
- `gradle/libs.versions.toml` — RC version + library entries
- `shared/build.gradle.kts` — RC in commonMain
- `composeApp/build.gradle.kts` — `REVENUECAT_KEY` BuildConfig field
- `composeApp/.../MindfulTennisApp.kt` — RC init before Koin
- `shared/src/iosMain/.../PurchasesHelper.kt` — iOS RC init function
- `iosApp/iOSApp.swift` — calls `PurchasesHelperKt.initPurchases()`
- `iosApp/Info.plist` — `REVENUECAT_KEY` entry
- `iosApp/Configuration/Secrets.xcconfig.template` — `REVENUECAT_KEY` placeholder
- `shared/.../domain/model/SubscriptionStatus.kt` — sealed interface + `hasPremiumAccess` ext
- `shared/.../data/repository/SubscriptionRepository.kt` — interface
- `shared/.../data/repository/SubscriptionRepositoryImpl.kt` — impl
- `shared/.../domain/usecase/GetSubscriptionStatusUseCase.kt`
- `shared/.../domain/usecase/RestorePurchasesUseCase.kt`
- `shared/.../di/CommonModule.kt` — registered SubscriptionRepository, use cases, viewmodels
- `shared/.../navigation/Route.kt` — added `Paywall` + `SubscriptionManagement` routes
- `shared/.../navigation/NavGraph.kt` — wired both routes + updated Settings nav

---

## Phase 4 — Paywall UI

**Status: ✅ Done**

- [x] Added `paywall` + `subscription_management` routes to `NavGraph.kt`
- [x] `PaywallScreen` — uses RC's prebuilt `PaywallView` composable (`purchases-kmp-ui`)
  - Falls back to loading/error state when offering unavailable
- [x] `SubscriptionManagementScreen` — shows plan status, trial end date, Upgrade and Restore buttons
- [x] Settings screen shows subscription status row → navigates to `SubscriptionManagementScreen`
- [x] `SubscriptionManagementScreen` → "Upgrade to Premium" navigates to `PaywallScreen`

**Pending (phase 5 gate):**
- [ ] Wire premium gate in feature screens: collect `GetSubscriptionStatusUseCase` → navigate to Paywall if not `hasPremiumAccess`

**Files:**
- `shared/.../ui/paywall/PaywallScreen.kt` + `PaywallViewModel.kt` + `PaywallUiState.kt`
- `shared/.../ui/subscription/SubscriptionManagementScreen.kt` + `SubscriptionManagementViewModel.kt` + `SubscriptionManagementUiState.kt`
- `shared/.../ui/settings/SettingsScreen.kt` + `SettingsUiState.kt` + `SettingsViewModel.kt` — subscription section added

---

## Phase 5 — Testing

**Status: ⬜ Not Started**

- [ ] Android: Google Play sandbox test accounts
- [ ] iOS: StoreKit sandbox accounts (Xcode → Settings → Accounts)
- [ ] Test full lifecycle: trial start → running → conversion → renewal → cancellation → expiration → restore
- [ ] Verify Supabase `subscriptions` table updated correctly for each event
- [ ] Confirm tennis purchase does NOT unlock any other sport (isolation test)
- [ ] Test offline entitlement cache (RC caches locally — app works without network)

---

## Secrets & Keys Reference

| Secret | Where | Notes |
|--------|-------|-------|
| `REVENUECAT_WEBHOOK_SECRET` | Supabase Edge Function secrets | Matches RevenueCat webhook Authorization header |
| `REVENUECAT_KEY` (Android) | `local.properties` → `BuildConfig` | Sport-specific RC Android API key |
| `REVENUECAT_KEY` (iOS) | `iosApp/Configuration/Secrets.xcconfig` | Sport-specific RC iOS API key |
| Supabase URL + anon key | `local.properties` → `BuildConfig` | Already configured |

---

## Webhook Event → Status Mapping

| RevenueCat Event | `subscriptions.status` |
|-----------------|------------------------|
| `TRIAL_STARTED` | `trial` |
| `INITIAL_PURCHASE` | `active` |
| `TRIAL_CONVERTED` | `active` |
| `RENEWAL` | `active` |
| `UNCANCELLATION` | `active` |
| `CANCELLATION` | `cancelled` |
| `EXPIRATION` | `expired` |
| `BILLING_ISSUE` | `grace_period` |
