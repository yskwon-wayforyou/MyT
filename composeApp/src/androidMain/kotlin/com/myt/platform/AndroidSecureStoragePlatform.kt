package com.myt.platform

import android.content.Context

actual class SecureStoragePlatform actual constructor(context: Any) {
    private val prefs = (context as Context).applicationContext
        .getSharedPreferences("myt_secure", Context.MODE_PRIVATE)

    actual fun saveToken(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getToken(key: String): String? = prefs.getString(key, null)

    actual fun deleteToken(key: String) {
        prefs.edit().remove(key).apply()
    }
}
