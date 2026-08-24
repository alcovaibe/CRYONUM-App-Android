package com.cryonum.content

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryonum.managers.PolicyManager

class PolicyUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val acceptedVersion = PolicyManager.getAcceptedVersion(applicationContext).toLong()
        if (acceptedVersion <= 0) return Result.success()

        val dependencies = ContentDependencies.get(applicationContext)
        return try {
            val config = dependencies.policyConfigService.fetch()
            val lastKnown = dependencies.policyUpdateStore.lastKnownVersion()
            val lastNotified = dependencies.policyUpdateStore.lastNotifiedVersion()
            val manifest = dependencies.repository.manifest()
            val shouldNotify = PolicyUpdateEvaluator.shouldNotify(
                acceptedVersion,
                lastKnown,
                lastNotified,
                config,
                manifest
            )
            dependencies.policyUpdateStore.recordKnownVersion(config.versionCode)
            if (shouldNotify && PolicyManager.showPolicyUpdateNotification(
                    applicationContext,
                    config.versionCode.toInt(),
                    config.versionName
                )
            ) {
                dependencies.policyUpdateStore.recordNotifiedVersion(config.versionCode)
            }
            Result.success()
        } catch (e: ContentException) {
            if (e.retryable && runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
    }
}
