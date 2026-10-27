package com.aircon.app

import android.app.Application
import com.aircon.app.di.AppContainer
import com.aircon.app.work.RefreshScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 应用入口。持有全局依赖容器并调度后台刷新任务。
 */
class AirConApplication : Application() {

    /** 全局依赖容器。 */
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 读取用户设置的刷新间隔后再调度，避免启动时用默认值覆盖用户选择。
        appScope.launch {
            val interval = runCatching {
                container.settingsStore.settings.first().refreshIntervalMinutes
            }.getOrDefault(DEFAULT_REFRESH_INTERVAL_MINUTES)
            RefreshScheduler.schedule(this@AirConApplication, interval)
        }
    }

    private companion object {
        const val DEFAULT_REFRESH_INTERVAL_MINUTES = 60
    }
}
