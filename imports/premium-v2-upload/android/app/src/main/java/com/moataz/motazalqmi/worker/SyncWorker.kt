package com.moataz.motazalqmi.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.moataz.motazalqmi.data.repository.BlogRepository
import com.moataz.motazalqmi.domain.model.SyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: BlogRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return when (repository.syncIncremental()) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Error -> if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "motaz_blog_periodic_sync"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
