package com.myt.platform

import android.content.Context
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

fun initializeOsmdroid(context: Context) {
    Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
    Configuration.getInstance().userAgentValue = "${context.packageName}/1.0"
}
