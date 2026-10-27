package com.aircon.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aircon.app.AirConApplication
import com.aircon.app.data.repository.WeatherRepository

/**
 * 后台刷新任务。
 *
 * 定期为所有已保存的城市更新天气与空气质量缓存。
 *
 * 注意：后台刷新会消耗数据源的免费额度，间隔由用户在设置中调整（默认 60 分钟）。
 * 对于付费数据源，建议在生产环境改为由服务端定时拉取并推送。
 */
class RefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val application = applicationContext as? AirConApplication
            ?: return Result.failure()
        val repository: WeatherRepository = application.container.weatherRepository

        val cities = runCatching { repository.cities() }.getOrDefault(emptyList())
        if (cities.isEmpty()) return Result.success()

        var allSucceeded = true
        for (city in cities) {
            val result = runCatching { repository.fetch(city, forceRefresh = true) }
            if (result.isFailure) {
                allSucceeded = false
            }
        }
        return if (allSucceeded) Result.success() else Result.retry()
    }
}
