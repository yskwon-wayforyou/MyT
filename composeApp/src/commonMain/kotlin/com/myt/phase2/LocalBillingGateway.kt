package com.myt.phase2

import com.russhwolf.settings.Settings

/**
 * Local sandbox billing — persists plan without Play Billing (M37 demo).
 */
class LocalBillingGateway(
    private val settings: Settings,
) : BillingGateway {
    override suspend fun currentStatus(): SubscriptionStatus {
        val plan = runCatching {
            SubscriptionPlan.valueOf(settings.getString(KEY_PLAN, SubscriptionPlan.Free.name))
        }.getOrDefault(SubscriptionPlan.Free)
        return SubscriptionStatus(plan = plan, sandbox = true)
    }

    override suspend fun startCheckout(plan: SubscriptionPlan): Result<Unit> {
        settings.putString(KEY_PLAN, plan.name)
        return Result.success(Unit)
    }

    companion object {
        private const val KEY_PLAN = "billing_sandbox_plan_v1"
    }
}

/** Feature flags derived from sandbox plan. */
object SubscriptionFeatures {
    fun watchPreview(plan: SubscriptionPlan): Boolean = plan != SubscriptionPlan.Free
    fun csvExportUnlimited(plan: SubscriptionPlan): Boolean = plan == SubscriptionPlan.Pro
    fun liveCameraDemo(plan: SubscriptionPlan): Boolean = plan != SubscriptionPlan.Free
}
