package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SubscriptionRepository
import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.StateFlow

class GetSubscriptionStatusUseCase(
    private val subscriptionRepository: SubscriptionRepository,
) {
    operator fun invoke(): StateFlow<SubscriptionStatus> =
        subscriptionRepository.subscriptionStatus
}
