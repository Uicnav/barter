package com.barter.core.di

import com.barter.core.data.FakeBarterRepository
import com.barter.core.domain.repo.BarterRepository
import com.barter.core.domain.usecase.*
import com.barter.core.presentation.vm.*
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

object AppDI {
    var koin: Koin? = null

    fun init(): Koin {
        if (koin != null) return koin!!

        val appModule = module {
            single<BarterRepository> { FakeBarterRepository() }

            // Use cases
            factory { LoadDiscoveryUseCase(get()) }
            factory { SwipeUseCase(get()) }
            factory { LoadMatchesUseCase(get()) }
            factory { SendMessageUseCase(get()) }
            factory { ProposeDealUseCase(get()) }
            factory { UpdateDealStatusUseCase(get()) }
            factory { LoginUseCase(get()) }
            factory { RegisterUseCase(get()) }
            factory { LogoutUseCase(get()) }
            factory { UpdateInterestsUseCase(get()) }
            factory { CreateListingUseCase(get()) }
            factory { UpdateListingUseCase(get()) }
            factory { DeleteListingUseCase(get()) }
            factory { GetListingByIdUseCase(get()) }
            factory { LoadMyListingsUseCase(get()) }
            factory { ToggleListingVisibilityUseCase(get()) }
            factory { RenewListingUseCase(get()) }
            factory { UpdateAvailabilityUseCase(get()) }
            factory { SearchListingsUseCase(get()) }
            factory { TopUpBalanceUseCase(get()) }
            factory { GetBalanceUseCase(get()) }
            factory { LoadEnrichedMatchesUseCase(get()) }
            factory { LoadProfileStatsUseCase(get()) }
            factory { GetUnreadCountsUseCase(get()) }
            factory { LoadNotificationsUseCase(get()) }
            factory { GetUnreadNotificationCountUseCase(get()) }
            factory { MarkNotificationReadUseCase(get()) }

            // ViewModels
            single { AuthViewModel(get(), get(), get(), get()) }
            factory { DiscoveryViewModel(get(), get(), get()) }
            factory { MatchesViewModel(get()) }
            factory { ChatViewModel(get()) }
            factory { DealViewModel(get(), get()) }
            factory { InterestsViewModel(get(), get()) }
            factory { CreateListingViewModel(get()) }
            factory { MyListingsViewModel(get(), get(), get(), get(), get()) }
            factory { EditListingViewModel(get(), get(), get(), get(), get()) }
            factory { ListingDetailViewModel(get(), get()) }
            factory { BrowseViewModel(get(), get()) }
            factory { ProfileStatsViewModel(get()) }
            factory { NotificationsViewModel(get(), get()) }
            single { BadgeViewModel(get(), get()) }
        }

        koin = startKoin { modules(appModule) }.koin
        return koin!!
    }

    inline fun <reified T : Any> get(): T = requireNotNull(koin) { "Call AppDI.init() first" }.get()
}
