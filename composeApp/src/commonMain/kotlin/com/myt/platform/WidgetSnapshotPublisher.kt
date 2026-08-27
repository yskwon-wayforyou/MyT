package com.myt.platform

import com.myt.phase2.WidgetSnapshot

/** Persist last gauge snapshot for the home-screen Glance widget. */
expect object WidgetSnapshotPublisher {
    fun publish(snapshot: WidgetSnapshot)
    fun read(): WidgetSnapshot?
}
