package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SubscriptionRepository

class RestorePurchasesUseCase(
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        subscriptionRepository.restorePurchases()
}
