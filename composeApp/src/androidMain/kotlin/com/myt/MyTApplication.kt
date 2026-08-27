package com.myt

import android.app.Application
import com.myt.debug.DebugLogger
import com.myt.di.initKoin
import com.myt.domain.usecase.CrashIssueSyncUseCase
import com.myt.platform.CrashReporterPlatform
import com.myt.platform.initializeOsmdroid
import org.koin.android.ext.koin.androidContext

class MyTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        initKoin {
            androidContext(this@MyTApplication)
        }
        initializeOsmdroid(this)
        runCatching {
            org.koin.core.context.GlobalContext.get().get<CrashReporterPlatform>().install()
        }
        runCatching {
            org.koin.core.context.GlobalContext.get().get<CrashIssueSyncUseCase>().start()
        }
        runCatching {
            org.koin.core.context.GlobalContext.get().get<DebugLogger>()
                .i("App", "MyT application started")
        }
    }

    companion object {
        lateinit var instance: MyTApplication
            private set

        val isInitialized: Boolean
            get() = ::instance.isInitialized
    }
}
