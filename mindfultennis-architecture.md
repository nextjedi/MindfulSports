# MindfulSports — Codebase Architecture Reference

> **Source repo:** `D:\Projects\MindfulSports` — `github.com/nextjedi/MindfulSports`
> **Branch:** `feature/multi-sport`
> **Date captured:** 2026-04-22
> **Purpose:** Living reference for the MindfulSports codebase — one app, 10 racket sports, runtime sport selection.

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Module Breakdown](#module-breakdown)
3. [Sport System](#sport-system)
4. [Domain Layer](#domain-layer)
5. [Data Layer](#data-layer)
6. [Sync Architecture](#sync-architecture)
7. [Dependency Injection (Koin)](#dependency-injection-koin)
8. [UI Layer & Navigation](#ui-layer--navigation)
9. [Android Entry Point](#android-entry-point)
10. [iOS Entry Point](#ios-entry-point)
11. [Build System](#build-system)
12. [Configuration & Secrets](#configuration--secrets)
13. [Key Data Flows](#key-data-flows)

---

## Project Structure

```
MindfulSports/
├── shared/                     ← KMP module: all business logic, UI, sync, DI
│   └── src/
│       ├── commonMain/         ← Shared Kotlin (domain, data, UI, navigation, sport/)
│       ├── androidMain/        ← Android-specific implementations
│       └── iosMain/            ← iOS-specific implementations
├── composeApp/                 ← Android app module (entry point)
├── iosApp/                     ← iOS app module (Swift wrapper)
├── gradle/
│   └── libs.versions.toml      ← Centralized dependency catalog
├── build.gradle.kts            ← Root build file
└── settings.gradle.kts         ← rootProject.name = "MindfulSports"
```

**Core principle:** All business logic, UI, and data handling lives in `shared/`. The `composeApp/` and `iosApp/` modules are thin wrappers — they initialize Koin and hand off to the shared Compose UI. Sport selection is a runtime decision stored in `UserPreferences`; no compile-time sport configuration exists.

---

## Module Breakdown

### shared/ (Kotlin Multiplatform)

`shared/build.gradle.kts`

- **Targets:** `androidTarget`, `iosX64`, `iosArm64`, `iosSimulatorArm64`
- **iOS output:** static framework named `shared`
- **Room DB version:** 3 (`mindful_sports_db`)
- **Schema exports:** `shared/schemas/`

Key source sets:

| Source Set | Purpose |
|---|---|
| `commonMain` | Domain models, use cases, repos, UI screens, navigation, sport system, Koin modules |
| `androidMain` | Room DB builder (with Context), DataStore factory, WorkManager sync scheduler |
| `iosMain` | Room DB builder (NSHomeDirectory), DataStore factory, iOS sync scheduler, KoinHelper, MainViewController |

### composeApp/ (Android)

`composeApp/build.gradle.kts`

- Namespace: `com.ashutosh.mindfultennis` (internal package, not user-visible)
- Application ID: `com.mindful.sports`
- Min SDK 29, Target/Compile SDK 36
- Version: 2.0 (code 18)
- Reads `local.properties` → exposes `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `REVENUECAT_KEY` via `BuildConfig`
- Deep link scheme: `com.mindful.sports`

### iosApp/ (Swift wrapper)

`iosApp/iosApp/iOSApp.swift`

- Reads `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `Info.plist` (injected via xcargs at build time)
- Calls `KoinHelperKt.doInitKoin(supabaseUrl:supabaseAnonKey:)` on launch
- Registers `"com.mindful.sports.sync"` BGTask handler
- Renders `ContentView()` → `ComposeView()` → `MainViewControllerKt.MainViewController()`

---

## Sport System

`shared/src/commonMain/.../sport/`

The sport system is the core addition in MindfulSports over MindfulTennis. It is entirely runtime — no compile-time flags.

### SportConfig (`SportConfig.kt`)

```kotlin
data class SportConfig(
    val sportId: String,          // stable ID: "tennis", "badminton", ...
    val displayName: String,      // "Table Tennis", "Beach Tennis", etc.
    val deepLinkScheme: String,   // legacy field — app now uses "com.mindful.sports"
    val terminology: SportTerminology,
    val scoringRules: ScoringRules,
)
```

### SportTerminology (`SportTerminology.kt`)

Maps the 8 fixed `Aspect` slots to sport-specific display labels:

| Field | Aspect slot | Tennis default |
|---|---|---|
| `forehand` | FOREHAND | "Forehand" |
| `backhand` | BACKHAND | "Backhand" |
| `serve` | SERVE | "Serve" |
| `returnGame` | RETURN | "Return Game" |
| `skill5` | VOLLEY | "Volley" |
| `skill6` | SLICE | "Slice" |
| `movement` | MOVEMENT | "Movement" |
| `mindset` | MINDSET | "Mindset" |

### ScoringRules (`ScoringRules.kt`)

| Field | Purpose |
|---|---|
| `pointsPerUnit` | Points to win a game/rally unit (6 for tennis, 21 for badminton, 11 for pickleball/squash) |
| `winByTwo` | Whether the unit requires a 2-point lead |
| `hasDeuce` | Tennis/padel deuce rule |
| `hasTiebreak` | Set tiebreak |
| `unitsPerSet` | Games per set (6 for tennis) |
| `setsToWin` | Sets needed to win the match |
| `maxSets` | Maximum sets in the match |
| `hasSets` | False for pickleball (no separate sets) |
| `unitLabel` / `setLabel` / `matchLabel` | UI label strings |

### SportRegistry (`SportRegistry.kt`)

Singleton holding all 10 `SportConfig` instances plus:

```kotlin
val all: List<SportConfig>                     // ordered list for SportSelectionScreen
fun fromId(sportId: String): SportConfig       // lookup, falls back to tennis
```

10 sports: `tennis`, `badminton`, `pickleball`, `squash`, `tableTennis`, `padel`, `racquetball`, `platformTennis`, `popTennis`, `beachTennis`.

### GetCurrentSportConfigUseCase (`domain/usecase/GetCurrentSportConfigUseCase.kt`)

```kotlin
class GetCurrentSportConfigUseCase(private val userPreferences: UserPreferences) {
    operator fun invoke(): Flow<SportConfig> =
        userPreferences.selectedSportId.map { id ->
            SportRegistry.fromId(id ?: SportRegistry.tennis.sportId)
        }
}
```

ViewModels that need the current sport inject this use case and collect the flow.

---

## Domain Layer

`shared/src/commonMain/.../domain/`

### Models

| Model | File | Key Fields |
|---|---|---|
| `Session` | `domain/model/Session.kt` | `id`, `userId`, `sportId`, `focusNote`, `startedAt`, `endedAt`, `isActive`, `matchType`, `opponent1Id`, `opponent2Id`, `partnerId`, `overallScore` |
| `Rating` | `domain/model/Rating.kt` | `id`, `sessionId`, `aspect` (Aspect enum), `rating` (1–5) |
| `SetScore` | `domain/model/SetScore.kt` | `id`, `sessionId`, `setNumber`, `userScore`, `opponentScore`, `opponentId` |
| `FocusPoint` | `domain/model/FocusPoint.kt` | `id`, `userId`, `sportId`, `text`, `category`, `createdAt`, `averageScore` |
| `Opponent` | `domain/model/Opponent.kt` | `id`, `userId`, `sportId`, `name`, `createdAt` |
| `Partner` | `domain/model/Partner.kt` | `id`, `userId`, `sportId`, `name`, `createdAt` |
| `Aspect` | `domain/model/Aspect.kt` | Enum: FOREHAND, BACKHAND, SERVE, RETURN, VOLLEY, SLICE, MOVEMENT, MINDSET |
| `MatchType` | `domain/model/MatchType.kt` | Enum: SINGLES, DOUBLES |
| `WinLossRecord` | `domain/model/WinLossRecord.kt` | `wins`, `losses`, `draws`, recent results |
| `PerformanceTrend` | `domain/model/PerformanceTrend.kt` | Chart data point (date + score) |
| `DurationFilter` | `domain/model/DurationFilter.kt` | Enum for dashboard date range |

`sportId` defaults to `"tennis"` in all models for backwards compatibility with pre-v3 data.

### Repository Interfaces

`shared/src/commonMain/.../data/repository/`

| Interface | Key Methods |
|---|---|
| `AuthRepository` | `authState: Flow<AuthState>`, `signInWithGoogle()`, `signInWithApple()`, `signInWithEmail()`, `signUpWithEmail()`, `handleAuthCallback()`, `signOut()`, `getCurrentUserId()` |
| `SessionRepository` | `createSession()`, `updateSession()`, `getSession()`, `observeSession()`, `endSession()`, `saveSelfRatings()`, `savePartnerRatings()`, `saveSetScores()`, `observeSessionsInRange()`, `getSelfRatingsForSessions()` |
| `FocusPointRepository` | `observeAll()`, `create()`, `delete()`, `getAllWithAverageScore()` |
| `OpponentRepository` | `observeAll()`, `create()`, `delete()` |
| `PartnerRepository` | `observeAll()`, `create()`, `delete()` |

### Use Cases

`shared/src/commonMain/.../domain/usecase/`

| Use Case | Signature |
|---|---|
| `StartSessionUseCase` | `invoke(focusNote: String): Result<Session>` |
| `EndSessionUseCase` | `invoke(sessionId: String, notes: String?): Result<Unit>` |
| `CancelSessionUseCase` | Deletes active session without saving |
| `SubmitRatingsUseCase` | `invoke(sessionId, selfRatings, partnerRatings, setScores, notes, matchType, ...): Result<Unit>` |
| `GetPerformanceTrendUseCase` | `invoke(userId, durationFilter): Flow<List<PerformanceTrend>>` |
| `GetWinLossRecordUseCase` | `invoke(userId, durationFilter, opponentIds, mode): Flow<WinLossRecord>` |
| `GetAspectAveragesUseCase` | `invoke(userId, durationFilter, ratingType): Flow<Map<Aspect, Float>>` |
| `GetSessionsUseCase` | Fetches sessions with filters |
| `GetCurrentSportConfigUseCase` | `invoke(): Flow<SportConfig>` — reads `UserPreferences.selectedSportId` |

---

## Data Layer

### Local Database (Room KMP)

`shared/src/commonMain/.../data/local/db/`

**Database class:** `MindfulDatabase.kt` — Version 3, filename `mindful_sports_db`, 7 entities

| Entity | Table | `sport_id` column |
|---|---|---|
| `SessionEntity` | `sessions` | ✅ (default `"tennis"`) |
| `SelfRatingEntity` | `self_ratings` | — |
| `PartnerRatingEntity` | `partner_ratings` | — |
| `SetScoreEntity` | `set_scores` | — |
| `FocusPointEntity` | `focus_points` | ✅ |
| `OpponentEntity` | `opponents` | ✅ |
| `PartnerEntity` | `partners` | ✅ |

All entities have a `sync_status` column (`PENDING` | `SYNCED` | `PENDING_DELETE`).

**Migration:**
- `MIGRATION_2_3`: `ALTER TABLE sessions/focus_points/opponents/partners ADD COLUMN sport_id TEXT NOT NULL DEFAULT 'tennis'`

**DAOs** (`data/local/db/dao/`):

| DAO | Key methods |
|---|---|
| `SessionDao` | `upsert()`, `getById()`, `observeById()`, `observeAllForUser()`, `getActiveSession()`, `observeSessionsInRange()`, `updateSyncStatus()`, `deleteAllForUser()` |
| `SelfRatingDao` | `upsert()`, `getBySyncStatus()`, `getSelfRatingsForSession()`, `getSelfRatingsForSessions()` |
| `PartnerRatingDao` | Same pattern as SelfRatingDao |
| `SetScoreDao` | `upsert()`, `getSetScoresForSession()`, `getSetScoresForSessions()` |
| `FocusPointDao` | `upsert()`, `observeAllForUser()`, `getBySyncStatus()`, `updateSyncStatus()`, `deleteAllForUser()` |
| `OpponentDao` / `PartnerDao` | Same pattern as FocusPointDao |

**Entity mappers:** `data/local/db/EntityMappers.kt` — includes `sportId` in all session/focuspoint/opponent/partner conversions.

**Platform DB builders:**

| Platform | File |
|---|---|
| Android | `shared/src/androidMain/.../data/local/db/DatabaseBuilder.android.kt` — `Room.databaseBuilder()` with Context |
| iOS | `shared/src/iosMain/.../data/local/db/DatabaseBuilder.ios.kt` — `Room.databaseBuilder()` with `BundledSQLiteDriver`, path: `NSHomeDirectory()/Documents/` |

### Remote (Supabase)

`shared/src/commonMain/.../data/remote/`

| Class | Purpose |
|---|---|
| `SupabaseSessionDataSource` | CRUD for sessions, ratings, scores, focus_points, opponents, partners on Supabase |
| `SupabaseUserDataSource` | User profile upsert and account deletion |

**Remote DTOs** (`data/remote/model/`) — all `@Serializable` with `@SerialName` annotations. `SessionDto`, `FocusPointDto`, `OpponentDto`, `PartnerDto` all include `@SerialName("sport_id") val sportId: String = "tennis"` with default for backwards compatibility.

### Repository Implementations

| Impl | Notes |
|---|---|
| `AuthRepositoryImpl` | Wraps Supabase Auth, maps `SessionStatus` → domain `AuthState` |
| `SessionRepositoryImpl` | Full CRUD + ratings, writes PENDING, uses entity mappers |
| `FocusPointRepositoryImpl` | Includes `getAllWithAverageScore()` |
| `OpponentRepositoryImpl` / `PartnerRepositoryImpl` | Standard CRUD |

### DataStore (User Preferences)

`shared/src/commonMain/.../data/local/datastore/UserPreferences.kt`

| Key | Type | Purpose |
|---|---|---|
| `lastSyncTimestamp` | `Long` | Watermark for pull-based sync |
| `cachedUserId` | `String?` | Used by background sync worker |
| `durationFilter` | `String` | Dashboard date range (default: ONE_MONTH) |
| `selectedOpponentIds` | `Set<String>` | W/L filter |
| `aspectDurationFilter` | `String` | Aspect chart range |
| `aspectRatingType` | `String` | Self vs partner rating type |
| `hasCompletedInitialSync` | `Boolean` | Guards one-time full data pull |
| `selectedSportId` | `String?` | Active sport; null until user picks one; cleared on sign-out |

`clearAll()` wipes the entire DataStore — used by sign-out and account deletion. This includes `selectedSportId`, so the next login requires sport re-selection.

Platform builders: `DataStoreFactory.android.kt` / `DataStoreFactory.ios.kt`

---

## Sync Architecture

`shared/src/commonMain/.../data/sync/`

### SyncManager (`SyncManager.kt`)

**Push phase** (ordered to satisfy foreign keys):
1. Ensure user record exists in Supabase
2. Push PENDING focus_points, opponents, partners
3. Push PENDING sessions
4. Push PENDING self_ratings, partner_ratings, set_scores
5. Mark all pushed rows as SYNCED

**Pull phase:**
- Fetch rows updated since `lastSyncTimestamp`
- Last-write-wins (LWW) merge by `updatedAt`
- Upsert into Room

### InitialSyncManager (`InitialSyncManager.kt`)

- Runs once on first login (`hasCompletedInitialSync = false`)
- Pulls **all** data for the user (no timestamp filter)
- Inserts into Room with `syncStatus = SYNCED`

### Background Sync

| Platform | Implementation | Frequency |
|---|---|---|
| Android | `AndroidSyncScheduler` → `WorkManager` → `SyncWorker` | Nightly at 1:30 AM |
| iOS | `IosSyncScheduler` → `BGAppRefreshTask` | Nightly at 1:30 AM |

Both read `cachedUserId` from `UserPreferences` and call `syncManager.sync(userId)`. The iOS handler is registered in `iOSApp.swift` under the task ID `"com.mindful.sports.sync"`.

---

## Dependency Injection (Koin)

`shared/src/commonMain/.../di/CommonModule.kt`

### Binding Summary

```
Supabase Client        — single, created from AppConfig.supabaseUrl + .supabaseAnonKey
                          plugins: Auth (scheme="com.mindful.sports"), Postgrest, Realtime

Room Database          — single, via platform DatabaseBuilder, MIGRATION_2_3 applied
  DAOs                 — single each (SessionDao, SelfRatingDao, FocusPointDao, ...)

DataStore              — single, via platform DataStoreFactory
  UserPreferences      — single

Remote Data Sources    — single
  SupabaseSessionDataSource
  SupabaseUserDataSource

Repositories           — single
  AuthRepository       → AuthRepositoryImpl
  SessionRepository    → SessionRepositoryImpl
  FocusPointRepository → FocusPointRepositoryImpl
  OpponentRepository   → OpponentRepositoryImpl
  PartnerRepository    → PartnerRepositoryImpl

Sync                   — single
  SyncManager
  InitialSyncManager

Use Cases              — factory (new instance per injection)
  StartSessionUseCase, EndSessionUseCase, CancelSessionUseCase, SubmitRatingsUseCase,
  GetPerformanceTrendUseCase, GetWinLossRecordUseCase, GetAspectAveragesUseCase,
  GetSessionsUseCase, GetCurrentSportConfigUseCase

ViewModels             — viewModel scope
  HomeViewModel, LoginViewModel, StartSessionViewModel,
  EndSessionViewModel (parametersOf(sessionId)),
  SessionsListViewModel, SessionDetailViewModel (parametersOf(sessionId)),
  SettingsViewModel, SportSelectionViewModel
```

### Platform Module (expect/actual)

| Platform | File | Extra bindings |
|---|---|---|
| Android | `shared/src/androidMain/.../di/PlatformModule.android.kt` | `DatabaseBuilder(context)`, `DataStore(context)`, `AndroidSyncScheduler` |
| iOS | `shared/src/iosMain/.../di/PlatformModule.ios.kt` | `DatabaseBuilder()` (no context), `DataStore()`, `IosSyncScheduler` |

### AppConfig

`shared/src/commonMain/.../di/AppConfig.kt`

```kotlin
data class AppConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    val deepLinkScheme: String = "com.mindful.sports",
)
```

Injected at startup from `BuildConfig` (Android) or `Info.plist` values (iOS). Sport selection is not part of `AppConfig` — it lives in `UserPreferences`.

---

## UI Layer & Navigation

### Navigation

`shared/src/commonMain/.../navigation/`

**Routes** (`Route.kt`):

```kotlin
sealed class Route(val route: String) {
    data object Login         : Route("login")
    data object SportSelection: Route("sport_selection")
    data object Home          : Route("home")
    data object StartSession  : Route("start_session")
    data class  EndSession(val sessionId: String) : Route("end_session/$sessionId")
    data object Settings      : Route("settings")
    data object SessionsList  : Route("sessions_list")
    data class  SessionDetail(val sessionId: String) : Route("session_detail/$sessionId")
}
```

**NavGraph** (`NavGraph.kt`):

- Receives `isAuthenticated: Boolean` and `hasSportSelected: Boolean` from `App.kt`
- `startDestination` logic:
  - `!isAuthenticated` → Login
  - `!hasSportSelected` → SportSelection
  - else → Home
- `LaunchedEffect(isAuthenticated, hasSportSelected)` guard handles live state changes (session expiry, sport cleared on sign-out)
- ViewModels retrieved via `koinViewModel()` / `koinViewModel { parametersOf(...) }`

**Navigation flow:**

```
Login ─(authenticated)──────────────────────────────── Home
                    └─(no sport chosen)→ SportSelection ─┘
                                                         │
                              Settings ←──────────────── │
                              "Change Sport" → SportSelection
```

### Screens & ViewModels

`shared/src/commonMain/.../ui/`

| Screen | ViewModel | Notes |
|---|---|---|
| `LoginScreen` | `LoginViewModel` | Email/password + Google/Apple OAuth |
| `SportSelectionScreen` | `SportSelectionViewModel` | `LazyVerticalGrid` of 10 sport cards; tapping writes to `UserPreferences.selectedSportId` |
| `HomeScreen` | `HomeViewModel` | Dashboard: trend chart, radar, W/L, focus points |
| `StartSessionScreen` | `StartSessionViewModel` | Session creation |
| `EndSessionScreen` | `EndSessionViewModel` | Ratings + set scores submission |
| `SessionsListScreen` | `SessionsListViewModel` | Scrollable session history |
| `SessionDetailScreen` | `SessionDetailViewModel` | Single session detail view |
| `SettingsScreen` | `SettingsViewModel` | Account info, sync, sign-out, delete account, change sport |

### UI State Pattern

Each screen follows:
- `*UiState` data class — all observable UI state
- `*UiEvent` sealed class — user actions
- ViewModel exposes `StateFlow<*UiState>` and `onEvent(*UiEvent)`

### Shared Components

`shared/src/commonMain/.../ui/components/`
- `ErrorRetryCard.kt`, `LoadingShimmer.kt`, `StarRatingBar.kt`, `SplashScreen.kt`

`shared/src/commonMain/.../ui/home/components/`
- `PerformanceChart.kt`, `RadarChart.kt`, `WinLossCard.kt`, `FocusPointsRow.kt`, `DurationFilterChips.kt`, `AspectPerformanceCard.kt`

`shared/src/commonMain/.../ui/endsession/components/`
- `PartnerRatingSection.kt`, `SetScoreSection.kt`

**Theme:** `shared/src/commonMain/.../ui/theme/MindfulTennisTheme.kt`

---

## Android Entry Point

`composeApp/src/main/kotlin/com/ashutosh/mindfultennis/`

| File | Role |
|---|---|
| `MainActivity.kt` | Extends `ComponentActivity`; handles OAuth deep links in `onNewIntent()`; calls `setContent { App() }` |
| `MindfulTennisApp.kt` | `Application` class; starts Koin with `commonModule + platformModule + AppConfig(supabaseUrl, supabaseAnonKey)` read from `BuildConfig` |

The `Application` class name `MindfulTennisApp` is an internal identifier — the user-visible name `"Mindful Sports"` comes from `strings.xml`.

---

## iOS Entry Point

`iosApp/iosApp/`

| File | Role |
|---|---|
| `iOSApp.swift` | `@main` entry; calls `KoinHelperKt.doInitKoin(supabaseUrl:supabaseAnonKey:)` in `init()`; registers BGTask handler for `"com.mindful.sports.sync"` |
| `ContentView.swift` | Renders `ComposeView()` → `UIViewControllerRepresentable` → `MainViewControllerKt.MainViewController()` |

**KMP iOS helpers:**

| File | Role |
|---|---|
| `shared/src/iosMain/.../KoinHelper.kt` | `fun initKoin(supabaseUrl: String, supabaseAnonKey: String)` — bootstraps Koin for iOS |
| `shared/src/iosMain/.../MainViewController.kt` | `fun MainViewController() = ComposeUIViewController { App() }` |

---

## Build System

`gradle/libs.versions.toml`

| Category | Library | Version |
|---|---|---|
| Language | Kotlin Multiplatform | 2.1.10 |
| UI | Compose Multiplatform | 1.7.1 |
| UI | Compose BOM (Android) | 2025.01.01 |
| DB | Room KMP | 2.7.1 |
| DB | SQLite (bundled driver) | 2.5.0 |
| Backend | Supabase | 3.1.1 |
| Network | Ktor | 3.1.1 (okhttp/Android, darwin/iOS) |
| DI | Koin | 4.0.2 |
| Preferences | DataStore | 1.1.4 |
| Navigation | JetBrains Navigation Compose (KMP) | 2.8.0-alpha10 |
| Async | Coroutines | 1.9.0 |
| Background | WorkManager | 2.10.0 |
| Date/Time | Kotlinx DateTime | 0.6.2 |
| Logging | Kermit | 2.0.5 |

**KSP** (2.1.10-1.0.29) is used for Room annotation processing across all platforms.

---

## Configuration & Secrets

Files not committed to the repo:

| File | Location | Keys |
|---|---|---|
| `local.properties` | `MindfulSports/` | `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `REVENUECAT_KEY_ANDROID` |
| `keystore.properties` | `MindfulSports/` | `storeFile`, `storePassword`, `keyAlias`, `keyPassword` |

iOS reads `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `Info.plist` at runtime. Values are injected via `xcargs` at CI build time, never committed.

Deep link scheme (single, both platforms): `com.mindful.sports`

---

## Key Data Flows

### Sport Selection

```
App.kt
  → collect UserPreferences.selectedSportId (Flow<String?>)
  → hasSportSelected = selectedSportId != null
  → passes hasSportSelected to NavGraph

NavGraph
  → startDestination = SportSelection (when authenticated but no sport)
  → LaunchedEffect guard: navigates to SportSelection if sport cleared

SportSelectionScreen
  → SportSelectionViewModel.onSportSelected(sportId)
      → UserPreferences.setSelectedSportId(sportId)
      → selectedSportId flow emits → hasSportSelected = true
      → NavGraph LaunchedEffect fires → navigate to Home
```

### Session Lifecycle

```
StartSessionScreen
  → StartSessionUseCase
      → check for active session (SessionDao.getActiveSession)
      → create Session(sportId = currentSportId, isActive=true, syncStatus=PENDING)
      → SessionDao.upsert
      → create FocusPoint if note provided → FocusPointDao.upsert
      → navigate back to Home (timer displayed)

EndSessionScreen
  → SubmitRatingsUseCase
      → SelfRatingDao.upsert (syncStatus=PENDING)
      → PartnerRatingDao.upsert (optional)
      → SetScoreDao.upsert (optional)
      → compute overallScore
      → SessionDao.upsert(isActive=false, endedAt=now, overallScore, syncStatus=PENDING)
      → background sync pushes PENDING rows to Supabase
```

### Auth & Initial Sync

```
MindfulTennisApp / iOSApp
  → Koin.startKoin(commonModule + platformModule + AppConfig)
  → SupabaseClient created (deep link scheme = "com.mindful.sports")

App.kt
  → collect AuthRepository.authState (Flow from Supabase sessionStatus)
  → AuthState.Authenticated → isAuthenticated = true → NavGraph shows Home (or SportSelection)
  → AuthState.SessionExpired → snackbar shown, redirected to Login

HomeViewModel (on first auth)
  → UserPreferences.hasCompletedInitialSync == false
  → InitialSyncManager.performInitialSync(userId)
      → fetch ALL sessions/ratings/focusPoints/opponents/partners from Supabase
      → upsert to Room with syncStatus=SYNCED
  → BackgroundSyncScheduler.schedulePeriodic()
```

### Dashboard Analytics

```
HomeViewModel collects:

performanceTrend
  ← GetPerformanceTrendUseCase
      ← SessionDao.observeSessionsInRange (completed, overallScore != null)

aspectAverages
  ← GetAspectAveragesUseCase
      ← SelfRatingDao.getSelfRatingsForSessions
      → group by Aspect, compute averages → Map<Aspect, Float>
      (labels rendered via SportTerminology.labelFor(aspect))

winLoss
  ← GetWinLossRecordUseCase
      ← SessionDao.observeSessionsInRange
      ← SetScoreDao.getSetScoresForSessions
      → compute wins/losses/draws per match, set, or game

focusPoints
  ← FocusPointRepository.observeAll (with average scores across sessions)
```

### Sign Out / Account Deletion

```
SettingsViewModel
  → authRepository.signOut()         (signs out from Supabase server)
  → userPreferences.clearAll()       (clears ALL DataStore keys, including selectedSportId)
  → isSignedOut = true
  → NavGraph routes to Login

On next login:
  → selectedSportId is null → NavGraph routes to SportSelection first
```
