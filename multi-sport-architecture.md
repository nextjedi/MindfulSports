# Multi-Sport Racket App — Architecture & Build Pipeline Analysis

> **Branch:** `feature/multi-sport`
> **Date:** 2026-04-22
> **Status:** Implemented — single-app architecture
>
> **Architecture pivot (2026-04-22):** Both Google Play and Apple App Store prohibit publishing
> multiple near-identical apps for different sports. The original Phase 1 design (10 product
> flavors → 10 store listings) was replaced with a **single app** (`com.mindful.sports`) with
> runtime sport selection stored in `UserPreferences`. Sport is chosen by the user after login
> via `SportSelectionScreen` and can be changed at any time from Settings.

---

## Table of Contents

1. [The 10 Sports](#the-10-sports)
2. [Architecture Overview](#architecture-overview)
3. [Payment Platform Analysis](#payment-platform-analysis)
4. [Payment Isolation Strategy](#payment-isolation-strategy)
5. [Azure DevOps CI/CD Pipeline](#azure-devops-cicd-pipeline)
6. [Fastlane Setup](#fastlane-setup)
7. [Database Strategy](#database-strategy)
8. [Build Configuration](#build-configuration)
9. [Implementation Sequence](#implementation-sequence)

---

## The 10 Sports

| # | Sport | App Name | Scoring System | Unique Skill Dimensions |
|---|-------|----------|----------------|------------------------|
| 1 | Tennis | Mindful Tennis | 6-game sets, deuce, tiebreak | Volley, Slice |
| 2 | Badminton | Mindful Badminton | Rally to 21, best of 3 | Smash, Clear, Drop Shot |
| 3 | Pickleball | Mindful Pickleball | 11 pts, win by 2 | Dink, Third-Shot Drop |
| 4 | Squash | Mindful Squash | 11 pts, best of 5 | Drop Shot, Boast |
| 5 | Table Tennis | Mindful Table Tennis | 11 pts, best of 7 | Loop/Topspin, Block |
| 6 | Padel | Mindful Padel | Same as tennis | Bandeja, Víbora |
| 7 | Racquetball | Mindful Racquetball | 15 pts, 2 games + tiebreak | Kill Shot, Ceiling Ball |
| 8 | Platform Tennis | Mindful Platform Tennis | Like tennis, screen play | Screen Rally, Lob |
| 9 | Pop Tennis | Mindful Pop Tennis | Like tennis, no-ad option | Overhead, Touch Volley |
| 10 | Beach Tennis | Mindful Beach Tennis | Like tennis, no-bounce | Sand Movement, Smash |

---

## Architecture Overview

```
ONE CODEBASE → ONE APP (com.mindful.sports)
├── shared/                        ← KMP: all business logic, repos, sync, DI
│   └── commonMain/
│       ├── sport/                 ← SportConfig, SportRegistry, SportTerminology, ScoringRules
│       │   └── 10 pre-defined SportConfig objects (tennis, badminton, ...)
│       ├── domain/model/          ← models with sport_id field
│       ├── domain/usecase/        ← GetCurrentSportConfigUseCase (Flow<SportConfig>)
│       ├── data/local/datastore/  ← UserPreferences.selectedSportId (DataStore)
│       └── ui/sportselection/     ← SportSelectionScreen + SportSelectionViewModel
├── composeApp/                    ← Android: single app, applicationId = com.mindful.sports
├── iosApp/                        ← iOS: single scheme, bundle ID = com.mindful.sports
├── fastlane/                      ← Single-app lanes (build + setup_signing)
│   ├── Fastfile
│   ├── Appfile                    ← app_identifier = com.mindful.sports
│   └── Matchfile
└── azure-pipelines/               ← Single-build pipelines (no matrix strategy)
    ├── android-pipeline.yml
    ├── ios-pipeline.yml
    └── templates/
        ├── android-build.yml
        └── ios-build.yml
```

**Core principle:** One app, one store listing, runtime sport selection. The user picks their sport after login; `UserPreferences.selectedSportId` drives `GetCurrentSportConfigUseCase`, which returns a `Flow<SportConfig>` consumed by ViewModels. Changing sport (via Settings → Change Sport) navigates back to `SportSelectionScreen` and updates the preference immediately.

**Navigation flow:**
```
Login → (authenticated) → SportSelection (if no sport chosen) → Home
                       ↑                                          ↓
               Settings → Change Sport ←────────────────── Settings
```

---

## Payment Platform Analysis

### The Candidates

Four serious options exist for in-app subscription management in a KMP app:

---

### Option 1: RevenueCat ⭐ Recommended

**What it is:** A dedicated mobile subscription management platform that sits between your app and the App Store / Play Store billing APIs. It handles purchase validation, receipt verification, entitlement logic, and analytics — across both platforms from one SDK.

**What it offers:**

| Capability | Detail |
|-----------|--------|
| **Cross-platform SDK** | Kotlin Multiplatform-compatible via their Android + iOS SDKs. One abstraction layer wraps StoreKit 2 (iOS) and Google Play Billing (Android) |
| **Entitlements** | Named access levels (`premium_monthly`, `premium_annual`) that your app checks — completely decoupled from product IDs. Product IDs can change; entitlement checks never do |
| **Server-side receipt validation** | RevenueCat's servers validate receipts with Apple/Google. You never validate receipts in your app code — eliminates an entire class of fraud |
| **Webhook events** | Fires events on subscription started, renewed, cancelled, billing retry, refunded — lets your Supabase backend react to subscription state changes |
| **Customer portal** | RevenueCat dashboard shows every subscriber, their status, transaction history, and lifetime value — without you building any of this |
| **Paywalls SDK** | Pre-built paywall UI that can be A/B tested and updated remotely without app releases |
| **Offerings** | Remote configuration of which products to show — change pricing experiments without a code deploy |
| **Free tier** | $0 up to $2,500 MTR (monthly tracked revenue) — enough to validate each sport before paying |
| **Per-app isolation** | Each app is a separate RevenueCat "Project". Subscriptions are completely isolated by default |
| **Analytics** | Churn, MRR, LTV, conversion rates broken down per app, per entitlement, per country |
| **Restore purchases** | One-line API call handles the "Restore Purchases" requirement from both stores |

**Pricing:** Free up to $2.5k MTR, then 1% of revenue. For 10 apps, each app's revenue is counted separately against its own MTR threshold.

**KMP Integration:**

```kotlin
// shared/commonMain — define entitlements
object Entitlements {
    const val PREMIUM_MONTHLY  = "premium_monthly"
    const val PREMIUM_ANNUAL   = "premium_annual"
    const val PREMIUM_LIFETIME = "premium_lifetime"
}

// The RevenueCat SDK is initialized per-platform with the sport-specific key.
// Platform check is done in androidMain/iosMain actual implementations.
```

---

### Option 2: Google Play Billing + StoreKit 2 (Direct)

**What it is:** Use both stores' native billing APIs directly, with no intermediary.

**Pros:**
- Zero ongoing cost (no revenue share)
- Full control over the billing flow

**Cons:**
- Must write and maintain two completely separate billing implementations (StoreKit 2 for iOS, Google Play Billing for Android)
- Server-side receipt validation is your responsibility — if you skip it, purchases can be spoofed
- No unified dashboard — you check Play Console and App Store Connect separately
- No webhook events — you must poll or use platform-specific server notifications (both have different formats and auth mechanisms)
- For 10 apps, you manage 20 separate billing integrations
- Entitlement logic lives in your code — if a receipt expires or is refunded, you find out via polling

**Verdict:** Viable for a single app. For 10 apps with cross-platform builds, the maintenance burden multiplies fast.

---

### Option 3: Adapty

**What it is:** A RevenueCat competitor with a similar feature set.

**Pros:**
- KMP SDK available (Adapty published a Kotlin Multiplatform SDK in 2024)
- Remote paywalls with A/B testing
- Free tier: up to $1k MTR

**Cons:**
- Smaller community and ecosystem than RevenueCat
- Webhook events less mature
- Analytics dashboard less detailed
- RevenueCat has longer track record and larger user base for debugging edge cases

**Verdict:** A credible alternative if RevenueCat pricing becomes a concern at scale. Worth reconsidering if you reach $50k+ MTR per app.

---

### Option 4: Glassfy

**What it is:** A newer subscription SDK, positioned as a lighter alternative to RevenueCat.

**Pros:**
- KMP support
- Per-app pricing model (flat fee rather than % of revenue above threshold)

**Cons:**
- Smallest community of the four
- Less documentation for KMP-specific integration
- Webhook ecosystem less developed

**Verdict:** Only consider if you have a strong revenue projection that makes RevenueCat's % model expensive.

---

### Payment Platform Decision Matrix

| Criteria | RevenueCat | Direct (Stores) | Adapty | Glassfy |
|----------|-----------|----------------|--------|---------|
| KMP support | ✅ | ⚠️ Manual | ✅ | ✅ |
| Server validation | ✅ Auto | ❌ DIY | ✅ Auto | ✅ Auto |
| Cross-platform dashboard | ✅ | ❌ | ✅ | ✅ |
| Webhook events | ✅ Rich | ⚠️ Basic | ✅ | ✅ |
| Remote paywalls | ✅ | ❌ | ✅ | ✅ |
| Free tier | $2.5k MTR | $0 | $1k MTR | $500 MTR |
| Scaling cost | 1% of revenue | $0 | 1.5% | Flat fee |
| Per-app isolation | ✅ Projects | ✅ Accounts | ✅ Projects | ✅ |
| Community / docs | ⭐⭐⭐⭐⭐ | N/A | ⭐⭐⭐ | ⭐⭐ |

**Recommendation: RevenueCat.** The free tier covers validation of all 10 apps. The KMP integration is well-documented. Server-side receipt validation is non-negotiable for a subscription business — doing it yourself adds weeks of infrastructure work that RevenueCat eliminates.

---

## Payment Isolation Strategy

### The Model (Single App)

Since there is one app with one store listing, there is **one RevenueCat project** and **one set of subscription products**. A user subscribes to "Mindful Sports Premium" — this grants access to all sports within the app.

```
RevenueCat Organization
└── Project: MindfulSports  → API key: rc_sports_xxx
    ├── Entitlement: premium_monthly
    ├── Entitlement: premium_annual
    └── Entitlement: premium_lifetime
```

Sport-scoped subscription data is still tracked in Supabase with a `sport_id` column for analytics (which sport drives the most premium conversions), but the entitlement check is universal — premium is premium.

### Supabase Subscription State

```sql
CREATE TABLE subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sport_id    TEXT NOT NULL,          -- tracks which sport context the purchase happened in
    status      TEXT NOT NULL CHECK (status IN ('active', 'expired', 'cancelled', 'grace_period')),
    product_id  TEXT NOT NULL,          -- e.g. "mindful_sports_premium_monthly"
    platform    TEXT NOT NULL CHECK (platform IN ('ios', 'android')),
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, platform)
);
```

### Client-Side Entitlement Check

```kotlin
// shared/commonMain
class PremiumRepository(private val revenueCat: RevenueCatClient) {
    suspend fun isPremium(userId: String): Boolean {
        return revenueCat.getCustomerInfo(userId)
            .entitlements[Entitlements.PREMIUM_MONTHLY]
            ?.isActive == true
    }
}
```

One RevenueCat project, one initialization with the single app's API key.

---

## Azure DevOps CI/CD Pipeline

### Why Azure DevOps Instead of GitHub Actions

| Factor | GitHub Actions | Azure DevOps |
|--------|---------------|--------------|
| Integration with Azure services | Good | Native |
| macOS Hosted Agents | Available (limited minutes on free) | Available (Microsoft-hosted or self-hosted) |
| Variable Groups & Key Vault | Secrets only | Full Azure Key Vault integration for 10 sets of credentials |
| Pipeline templates | Reusable workflows | YAML templates + template repositories |
| Parallel jobs | Limited on free tier | Configurable parallelism |
| Artifact management | GitHub Packages | Azure Artifacts (better for large APKs/IPAs) |
| Audit logging | Basic | Enterprise-grade |

For 10 apps × 2 platforms = 20 pipelines, Azure DevOps Variable Groups backed by Azure Key Vault is significantly cleaner than managing 200+ GitHub secrets.

---

### Credential Storage — Azure Key Vault

```
Azure Key Vault: mindful-sports-kv
├── supabase-url
├── supabase-anon-key
├── revenuecat-android-key
├── revenuecat-ios-key
├── keystore-password
├── keystore-alias
├── keystore-file-base64
└── match-certificates-password
```

One Azure DevOps Variable Group links to the Key Vault. Pipelines reference the group — no secrets in YAML files. (Reduced from 80 secrets across 10 sports to 8 secrets for one app.)

---

### Pipeline Structure

```
azure-pipelines/
├── android-pipeline.yml          ← triggers all 10 Android builds
├── ios-pipeline.yml              ← triggers all 10 iOS builds
└── templates/
    ├── android-sport-build.yml   ← reusable template for one Android sport
    └── ios-sport-build.yml       ← reusable template for one iOS sport
```

---

### `azure-pipelines/templates/android-build.yml`

```yaml
# Reusable template — build and publish the Mindful Sports AAB to Play Store.
steps:
  - task: AzureKeyVault@2
    displayName: 'Load secrets from Key Vault'
    inputs:
      azureSubscription: 'MindfulSports-ServiceConnection'
      KeyVaultName: 'mindful-sports-kv'
      SecretsFilter: >
        supabase-url,
        supabase-anon-key,
        revenuecat-android-key,
        keystore-password,
        keystore-alias,
        keystore-file-base64

  - bash: echo "$(keystore-file-base64)" | base64 --decode > keystore.jks
    displayName: 'Decode keystore'

  - bash: |
      cat >> local.properties << EOF
      SUPABASE_URL=$(supabase-url)
      SUPABASE_ANON_KEY=$(supabase-anon-key)
      REVENUECAT_KEY_ANDROID=$(revenuecat-android-key)
      EOF
    displayName: 'Write credentials to local.properties'

  - task: Gradle@3
    displayName: 'Build Release AAB'
    inputs:
      gradleWrapperFile: 'gradlew'
      tasks: ':composeApp:bundleRelease'
      options: >
        -Pandroid.injected.signing.store.file=$(System.DefaultWorkingDirectory)/keystore.jks
        -Pandroid.injected.signing.store.password=$(keystore-password)
        -Pandroid.injected.signing.key.alias=$(keystore-alias)
        -Pandroid.injected.signing.key.password=$(keystore-password)
      javaHomeOption: 'JDKVersion'
      jdkVersionOption: '17'

  - task: GooglePlayRelease@4
    displayName: 'Upload to Play Store internal track'
    inputs:
      serviceAccountJsonPlainText: '$(play-store-service-account-json)'
      applicationId: 'com.mindful.sports'
      action: 'SingleBundle'
      bundleFile: 'composeApp/build/outputs/bundle/release/*.aab'
      track: 'internal'
```

---

### `azure-pipelines/android-pipeline.yml`

```yaml
trigger:
  branches:
    include:
      - main
  paths:
    include:
      - composeApp/**
      - shared/**

pool:
  vmImage: 'ubuntu-latest'

steps:
  - template: templates/android-build.yml
```

---

## Fastlane Setup

Fastlane handles code signing (via `match`), building, and App Store Connect uploads. Azure DevOps calls Fastlane — Azure handles scheduling and secrets injection, Fastlane handles the Apple toolchain.

### Why Fastlane

| Capability | Detail |
|-----------|--------|
| `match` | Syncs code signing certificates and provisioning profiles across machines via a git repo or Azure Blob Storage. One command sets up any CI agent |
| `gym` | Wraps `xcodebuild archive` with sane defaults, better error output, and automatic IPA export |
| `deliver` / `pilot` | Uploads to App Store Connect / TestFlight without touching the web UI |
| `supply` | Uploads Android AABs to Play Store (alternative to the Google Play DevOps task) |
| `spaceship` | Underlying API client — lets you automate App Store Connect tasks (create listings, manage testers) |
| Multi-platform | One `Fastfile` handles both iOS and Android lanes |

### Directory Structure

```
fastlane/
├── Fastfile           ← lane definitions
├── Appfile            ← app identifiers (overridden per lane)
├── Matchfile          ← code signing config
└── Pluginfile         ← fastlane plugins
```

---

### `fastlane/Matchfile`

```ruby
# Certificates and profiles stored in Azure Blob Storage
# (alternative to a private git repo — works better in Azure ecosystem)
storage_mode("azure_storage")
azure_storage_account(ENV["AZURE_STORAGE_ACCOUNT"])
azure_storage_access_key(ENV["AZURE_STORAGE_ACCESS_KEY"])
azure_storage_container("fastlane-match-certs")

type("appstore")
readonly(true)    # CI only reads, never generates new certs
```

---

### `fastlane/Fastfile`

```ruby
default_platform(:ios)

BUNDLE_ID  = "com.mindful.sports"
APP_NAME   = "Mindful Sports"
IOS_SCHEME = "iosApp"

platform :ios do
  lane :build do |options|
    match(type: "appstore", app_identifier: BUNDLE_ID, readonly: true)
    gym(
      scheme:           IOS_SCHEME,
      configuration:    "Release",
      export_method:    "app-store",
      output_directory: "build/ios",
      xcargs: "SUPABASE_URL=#{options[:supabase_url]} SUPABASE_ANON_KEY=#{options[:supabase_key]}",
    )
    pilot(app_identifier: BUNDLE_ID, skip_waiting_for_build_processing: true)
  end

  lane :setup_signing do
    match(type: "appstore", app_identifier: BUNDLE_ID, readonly: false)
  end
end

platform :android do
  lane :build do |options|
    gradle(
      task: "bundle", build_type: "Release", project_dir: "./", flags: "--no-daemon",
      properties: {
        "android.injected.signing.store.file"     => options[:keystore_path],
        "android.injected.signing.store.password" => options[:keystore_password],
        "android.injected.signing.key.alias"      => options[:key_alias],
        "android.injected.signing.key.password"   => options[:key_password],
      }
    )
    supply(package_name: BUNDLE_ID, aab: "composeApp/build/outputs/bundle/release/*.aab", track: "internal")
  end
end
```

---

### Code Signing Strategy (match)

| Approach | For |
|----------|-----|
| `match` with Azure Blob Storage | Stores all 10 distribution certificates + 10 provisioning profiles in one container |
| `readonly: true` on CI | CI agents only download — never modify certs |
| `readonly: false` locally | A designated team member regenerates certs when they expire |
| Separate `match` call per bundle ID | Each of the 10 apps has its own provisioning profile |

This replaces manual Xcode-managed signing and eliminates "certificate expired" build failures.

---

## Database Strategy

### Single Supabase Project — Sport-Scoped Data

All 10 apps share **one Supabase project**:
- One `users` table — sign in once, identity works across all sports
- All sport data is tagged with `sport_id` column
- RLS policies enforce isolation — the badminton app never reads tennis data

### Schema Additions

```sql
-- Add sport_id discriminator to all sport-specific tables
ALTER TABLE sessions       ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis';
ALTER TABLE focus_points   ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis';
ALTER TABLE opponents      ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis';
ALTER TABLE partners       ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis';

-- Subscription state (driven by RevenueCat webhooks)
CREATE TABLE subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sport_id    TEXT NOT NULL,
    status      TEXT NOT NULL CHECK (status IN ('active', 'expired', 'cancelled', 'grace_period')),
    product_id  TEXT NOT NULL,
    platform    TEXT NOT NULL CHECK (platform IN ('ios', 'android')),
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, sport_id, platform)
);

-- Performance indexes
CREATE INDEX idx_sessions_sport    ON sessions(user_id, sport_id, updated_at);
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id, sport_id, status);

-- RLS
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "own_sport_subscriptions" ON subscriptions
    FOR ALL USING (
        auth.uid() = user_id AND
        sport_id = (auth.jwt() ->> 'sport_id')
    );
```

### JWT Custom Claim — Sport Context

When a user authenticates in any sport app, the Supabase session is enriched with the sport context:

```kotlin
// AuthRepositoryImpl — called right after any sign-in
supabase.auth.updateUser {
    data = buildJsonObject {
        put("sport_id", sportConfig.sportId)
    }
}
```

The JWT then carries `sport_id`, and RLS policies filter automatically at the database level.

---

## Build Configuration

### `local.properties` (never committed)

```properties
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJ...
REVENUECAT_KEY_ANDROID=goog_xxx
```

iOS credentials are injected at build time via xcconfig variables (`$(SUPABASE_URL)`, `$(SUPABASE_ANON_KEY)`) set in the CI pipeline via `xcargs`.

### Subscription Product IDs (Play Store / App Store)

| Entitlement | Product ID |
|------------|-----------|
| `premium_monthly` | `mindful_sports_premium_monthly` |
| `premium_annual` | `mindful_sports_premium_annual` |
| `premium_lifetime` | `mindful_sports_premium_lifetime` |

One RevenueCat project, one set of products. Entitlement check is always `Entitlements.PREMIUM_MONTHLY` etc. regardless of which sport is selected.

---

## Implementation Sequence

| Phase | Scope | Status | What gets done |
|-------|-------|--------|----------------|
| **1** | SportConfig system | ✅ Done | `SportConfig`, `ScoringRules`, `SportTerminology`, `SportRegistry` in shared module. 10 sport configs defined. |
| **2** | DB migration | ✅ Done | `sport_id` column added to Room entities via `MIGRATION_2_3`. Supabase schema updated with `sport_id` columns. |
| **3** | Runtime sport selection | ✅ Done | `UserPreferences.selectedSportId`, `GetCurrentSportConfigUseCase`, `SportSelectionScreen`, navigation guard in `NavGraph`, "Change Sport" in Settings. |
| **4** | Single-app config | ✅ Done | `applicationId = "com.mindful.sports"`, single bundle ID, `AppConfig` without sport, `KoinHelper` 2-param. |
| **5** | CI/CD simplification | ✅ Done | `Fastfile` single lanes, Azure pipelines without matrix strategy, 8 Key Vault secrets (was 80). |
| **6** | RevenueCat | Pending | Create one RevenueCat project for `com.mindful.sports`. Add KMP SDK. `PremiumRepository`. Paywall UI. |
| **7** | Supabase webhook | Pending | Edge Function to receive RevenueCat webhooks, write to `subscriptions` table. |
| **8** | Sport assets | Pending | Icons, splash illustrations, and accent colors for each of the 10 sports. |
| **9** | Store listing | Pending | Create one Play Store + one App Store listing. Submit for review. |

---

## Key Decisions Summary

| Decision | Choice | Reason |
|----------|--------|--------|
| App model | Single app (`com.mindful.sports`) | Store policies prohibit multiple near-identical apps per sport |
| Sport selection | Runtime via `UserPreferences.selectedSportId` | User can switch sports; persists across sessions |
| Payment platform | RevenueCat | Server-side validation, KMP support, free tier for validation |
| Subscription scope | One RevenueCat project, sport-agnostic entitlements | Single app = single listing = single subscription |
| CI/CD platform | Azure DevOps | Native Azure Key Vault integration |
| Secret management | Azure Key Vault + Variable Groups | One vault, 8 secrets, no secrets in YAML |
| Build automation | Fastlane (called by Azure DevOps) | Handles Apple toolchain complexity |
| Code signing | Fastlane match + Azure Blob Storage | Eliminates certificate drift across machines |
| Database | One Supabase project + `sport_id` discriminator | Shared identity, sport-scoped data, one backend |
| Rating aspects | Fixed 8 slots, label from `SportTerminology` | No schema change, UI-layer localization only |
