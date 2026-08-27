package com.myt.data.settings

import com.russhwolf.settings.Settings

fun Settings.optionalString(key: String): String? =
    getString(key, "").takeIf { it.isNotEmpty() }
