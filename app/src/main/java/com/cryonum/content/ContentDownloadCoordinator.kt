package com.cryonum.content

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ContentDownloadCoordinator(context: Context, private val repository: ContentDownloadRepository) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun enqueue(bundle: ContentBundle, replace: Boolean = false) {
        workManager.enqueueUniqueWork(
            bundle.workName,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request(bundle)
        )
    }

    fun observe(bundle: ContentBundle): LiveData<List<WorkInfo>> = workManager.getWorkInfosForUniqueWorkLiveData(bundle.workName)

    fun cancel(bundle: ContentBundle) {
        workManager.cancelUniqueWork(bundle.workName)
    }

    suspend fun restart(bundle: ContentBundle) = withContext(Dispatchers.IO) {
        workManager.cancelUniqueWork(bundle.workName).result.get()
        repository.clearPartials(bundle)
        enqueue(bundle, replace = true)
    }

    private fun request(bundle: ContentBundle): OneTimeWorkRequest = OneTimeWorkRequestBuilder<ContentDownloadWorker>()
        .setInputData(workDataOf(ContentDownloadWorker.KEY_BUNDLE to bundle.name))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
        .build()
}
