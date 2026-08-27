package com.myt.platform

import com.myt.phase2.WidgetSnapshot

actual object WidgetSnapshotPublisher {
    actual fun publish(snapshot: WidgetSnapshot) = Unit
    actual fun read(): WidgetSnapshot? = null
}
