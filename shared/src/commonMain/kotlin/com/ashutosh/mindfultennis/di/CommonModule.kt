package com.ashutosh.mindfultennis.di

import androidx.room.RoomDatabase
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.local.db.MindfulDatabase
import com.ashutosh.mindfultennis.data.remote.SupabaseSessionDataSource
import com.ashutosh.mindfultennis.data.remote.SupabaseUserDataSource
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthRepositoryImpl
import com.ashutosh.mindfultennis.data.repository.FocusPointRepository
import com.ashutosh.mindfultennis.data.repository.FocusPointRepositoryImpl
import com.ashutosh.mindfultennis.data.repository.OpponentRepository
import com.ashutosh.mindfultennis.data.repository.OpponentRepositoryImpl
import com.ashutosh.mindfultennis.data.repository.PartnerRepository
import com.ashutosh.mindfultennis.data.repository.PartnerRepositoryImpl
import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.data.repository.SessionRepositoryImpl
import com.ashutosh.mindfultennis.data.sync.InitialSyncManager
import com.ashutosh.mindfultennis.data.sync.SyncManager
import com.ashutosh.mindfultennis.domain.usecase.CancelSessionUseCase
import com.ashutosh.mindfultennis.domain.usecase.EndSessionUseCase
import com.ashutosh.mindfultennis.domain.usecase.GetAspectAveragesUseCase
import com.ashutosh.mindfultennis.domain.usecase.GetPerformanceTrendUseCase
import com.ashutosh.mindfultennis.domain.usecase.GetSessionsUseCase
import com.ashutosh.mindfultennis.domain.usecase.GetWinLossRecordUseCase
import com.ashutosh.mindfultennis.domain.usecase.StartSessionUseCase
import com.ashutosh.mindfultennis.domain.usecase.SubmitRatingsUseCase
import com.ashutosh.mindfultennis.ui.endsession.EndSessionViewModel
import com.ashutosh.mindfultennis.ui.home.HomeViewModel
import com.ashutosh.mindfultennis.ui.login.LoginViewModel
import com.ashutosh.mindfultennis.ui.sessions.SessionDetailViewModel
import com.ashutosh.mindfultennis.ui.sessions.SessionsListViewModel
import com.ashutosh.mindfultennis.ui.settings.SettingsViewModel
import com.ashutosh.mindfultennis.ui.startsession.StartSessionViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {

    // ── Supabase Client ────────────────────────────────────────────────

    single<SupabaseClient> {
        val config = get<AppConfig>()
        createSupabaseClient(
            supabaseUrl = config.supabaseUrl,
            supabaseKey = config.supabaseAnonKey,
        ) {
            install(Auth) {
                scheme = config.deepLinkScheme
                host = "callback"
            }
            install(Postgrest)
            install(Realtime)
        }
    }

    // ── Room Database ──────────────────────────────────────────────────

    single<MindfulDatabase> {
        get<RoomDatabase.Builder<MindfulDatabase>>()
            .fallbackToDestructiveMigration(true)
            .build()
    }

    // ── DAOs ───────────────────────────────────────────────────────────

    single { get<MindfulDatabase>().sessionDao() }
    single { get<MindfulDatabase>().selfRatingDao() }
    single { get<MindfulDatabase>().partnerRatingDao() }
    single { get<MindfulDatabase>().focusPointDao() }
    single { get<MindfulDatabase>().opponentDao() }
    single { get<MindfulDatabase>().partnerDao() }
    single { get<MindfulDatabase>().setScoreDao() }

    // ── UserPreferences ────────────────────────────────────────────────

    single { UserPreferences(get()) }

    // ── Remote Data Sources ────────────────────────────────────────────

    single { SupabaseSessionDataSource(get()) }
    single { SupabaseUserDataSource(get()) }

    // ── Repositories ───────────────────────────────────────────────────

    single<AuthRepository> { AuthRepositoryImpl(get()) }

    single<SessionRepository> {
        SessionRepositoryImpl(
            sessionDao = get(),
            selfRatingDao = get(),
            partnerRatingDao = get(),
            setScoreDao = get(),
        )
    }

    single<FocusPointRepository> {
        FocusPointRepositoryImpl(
            focusPointDao = get(),
            sessionDao = get(),
        )
    }

    single<OpponentRepository> {
        OpponentRepositoryImpl(
            opponentDao = get(),
        )
    }

    single<PartnerRepository> {
        PartnerRepositoryImpl(
            partnerDao = get(),
        )
    }

    // ── SyncManager ────────────────────────────────────────────────────

    single {
        SyncManager(
            sessionDao = get(),
            selfRatingDao = get(),
            partnerRatingDao = get(),
            setScoreDao = get(),
            focusPointDao = get(),
            opponentDao = get(),
            partnerDao = get(),
            remoteDataSource = get(),
            userDataSource = get(),
            supabaseClient = get(),
            userPreferences = get(),
        )
    }

    // ── InitialSyncManager ──────────────────────────────────────────────

    single {
        InitialSyncManager(
            sessionDao = get(),
            selfRatingDao = get(),
            partnerRatingDao = get(),
            setScoreDao = get(),
            focusPointDao = get(),
            opponentDao = get(),
            partnerDao = get(),
            remoteDataSource = get(),
            userPreferences = get(),
        )
    }

    // ── Use Cases ──────────────────────────────────────────────────────

    factory { StartSessionUseCase(get(), get(), get()) }
    factory { EndSessionUseCase(get()) }
    factory { CancelSessionUseCase(get()) }
    factory { SubmitRatingsUseCase(get()) }
    factory { GetPerformanceTrendUseCase(get()) }
    factory { GetWinLossRecordUseCase(get()) }
    factory { GetAspectAveragesUseCase(get()) }
    factory { GetSessionsUseCase(get()) }

    // ── ViewModels ─────────────────────────────────────────────────────

    viewModel {
        HomeViewModel(
            authRepository = get(),
            sessionRepository = get(),
            opponentRepository = get(),
            userPreferences = get(),
            initialSyncManager = get(),
            syncManager = get(),
            cancelSessionUseCase = get(),
            getPerformanceTrendUseCase = get(),
            getWinLossRecordUseCase = get(),
            getAspectAveragesUseCase = get(),
        )
    }

    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }

    viewModel {
        LoginViewModel(
            authRepository = get(),
        )
    }

    viewModel {
        StartSessionViewModel(
            authRepository = get(),
            focusPointRepository = get(),
            startSessionUseCase = get(),
        )
    }

    viewModel { params ->
        EndSessionViewModel(
            sessionId = params.get(),
            authRepository = get(),
            sessionRepository = get(),
            opponentRepository = get(),
            partnerRepository = get(),
            submitRatingsUseCase = get(),
        )
    }

    viewModel {
        SessionsListViewModel(
            authRepository = get(),
            getSessionsUseCase = get(),
        )
    }

    viewModel { params ->
        SessionDetailViewModel(
            sessionId = params.get(),
            sessionRepository = get(),
            opponentRepository = get(),
            partnerRepository = get(),
        )
    }
}
