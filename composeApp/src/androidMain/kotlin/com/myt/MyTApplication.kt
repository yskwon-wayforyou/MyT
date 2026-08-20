package com.myt

import android.app.Application
import com.myt.di.initKoin
import org.koin.android.ext.koin.androidContext

class MyTApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        initKoin {
            androidContext(this@MyTApplication)
        }
    }

    companion object {
        lateinit var instance: MyTApplication
            private set
    }
}
