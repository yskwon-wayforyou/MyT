package com.myt.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIWindowScene

actual class AppInfoPlatform actual constructor(context: Any) {
    actual fun collect(): AppInfo {
        val version = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            ?: "unknown"
        val build = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
            ?: "0"
        return AppInfo(
            appVersion = version,
            buildLabel = build,
            osDescription = "iOS ${UIDevice.currentDevice.systemVersion}",
            deviceDescription = UIDevice.currentDevice.model,
            platformLabel = "iOS",
        )
    }
}

actual class LogExportPlatform actual constructor(context: Any) {
    actual fun shareLogFile(content: String, fileName: String): Result<Unit> = runCatching {
        val path = NSTemporaryDirectory() + fileName
        platform.Foundation.NSString.create(string = content).writeToFile(
            path = path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        val url = NSURL.fileURLWithPath(path)
        val controller = UIActivityViewController(listOf(url), null)
        val root = UIApplication.sharedApplication.connectedScenes
            .let { scenes ->
                (0 until scenes.count()).mapNotNull { scenes.elementAt(it) as? UIWindowScene }
                    .firstOrNull()
                    ?.keyWindow
                    ?.rootViewController
            }
        root?.presentViewController(controller, animated = true, completion = null)
            ?: error("화면을 찾을 수 없어 공유 시트를 열 수 없습니다")
    }
}
