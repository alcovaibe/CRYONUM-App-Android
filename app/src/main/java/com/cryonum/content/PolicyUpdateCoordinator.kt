package com.cryonum.content

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class PolicyUpdateCoordinator(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<PolicyUpdateWorker>(7, TimeUnit.DAYS, 6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(delayUntilNextSundayMorning(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun checkNow() {
        val request = OneTimeWorkRequestBuilder<PolicyUpdateWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "icy-policy-update-weekly-v2"
        private const val IMMEDIATE_WORK_NAME = "icy-policy-update-immediate"

        internal fun delayUntilNextSundayMorning(now: ZonedDateTime = ZonedDateTime.now()): Long {
            var next = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
            if (!next.isAfter(now)) next = next.plusWeeks(1)
            return Duration.between(now, next).toMillis().coerceAtLeast(0)
        }
    }
}
