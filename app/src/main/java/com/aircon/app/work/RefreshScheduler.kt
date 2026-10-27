package com.aircon.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 后台刷新任务调度。
 */
object RefreshScheduler {

    private const val WORK_NAME = "aircon-periodic-refresh"

    /** WorkManager 周期任务允许的最小间隔（分钟）。 */
    private const val MIN_INTERVAL_MINUTES = 15

    /**
     * 调度周期性的后台刷新。
     *
     * @param context 应用上下文
     * @param intervalMinutes 刷新间隔（分钟），低于 15 分钟时按 15 分钟处理
     */
    fun schedule(context: Context, intervalMinutes: Int = 60) {
        val interval = intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES)
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(interval.toLong(), TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // 用户修改间隔后需要立即生效，因此用 UPDATE 而非 KEEP。
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * 取消后台刷新。
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
