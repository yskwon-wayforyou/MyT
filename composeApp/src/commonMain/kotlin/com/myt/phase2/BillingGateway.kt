package com.myt.phase2

/**
 * M37 — subscription / billing sandbox stub.
 */
enum class SubscriptionPlan {
    Free,
    Plus,
    Pro,
}

data class SubscriptionStatus(
    val plan: SubscriptionPlan,
    val renewalEpochMs: Long? = null,
    val sandbox: Boolean = true,
)

interface BillingGateway {
    suspend fun currentStatus(): SubscriptionStatus
    suspend fun startCheckout(plan: SubscriptionPlan): Result<Unit>
}

class SandboxBillingGateway : BillingGateway {
    override suspend fun currentStatus(): SubscriptionStatus =
        SubscriptionStatus(plan = SubscriptionPlan.Free, sandbox = true)

    override suspend fun startCheckout(plan: SubscriptionPlan): Result<Unit> =
        Result.success(Unit)
}
