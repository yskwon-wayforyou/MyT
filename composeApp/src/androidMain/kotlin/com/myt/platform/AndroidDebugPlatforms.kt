package com.myt.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

actual class AppInfoPlatform actual constructor(context: Any) {
    private val ctx = context as Context

    actual fun collect(): AppInfo {
        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val version = packageInfo.versionName ?: "unknown"
        val build = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }
        return AppInfo(
            appVersion = version,
            buildLabel = build,
            osDescription = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            deviceDescription = "${Build.MANUFACTURER} ${Build.MODEL}",
            platformLabel = "Android",
        )
    }
}

actual class LogExportPlatform actual constructor(context: Any) {
    private val ctx = context as Context

    actual fun shareLogFile(content: String, fileName: String): Result<Unit> = runCatching {
        val dir = File(ctx.cacheDir, "debug_logs").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        shareAttachment(file)
    }

    private fun shareAttachment(file: File) {
        val authority = "${ctx.packageName}.debugfileprovider"
        val uri = FileProvider.getUriForFile(ctx, authority, file)
        val subject = "MyT Debug Log"
        val body = buildString {
            appendLine("MyT 디버그 로그 파일을 첨부했습니다.")
            appendLine("문제 재현 경로·증상을 함께 적어 주시면 분석에 도움이 됩니다.")
            appendLine()
            appendLine("— MyT")
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val gmail = Intent(send).setPackage("com.google.android.gm")
        val pm = ctx.packageManager
        when {
            gmail.resolveActivity(pm) != null ->
                ContextCompat.startActivity(ctx, gmail, null)
            send.resolveActivity(pm) != null -> {
                val chooser = Intent.createChooser(send, "로그 보내기")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ContextCompat.startActivity(ctx, chooser, null)
            }
            else -> error("이메일/공유 앱을 찾을 수 없습니다")
        }
    }
}
