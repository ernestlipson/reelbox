package dev.reelbox

import android.app.Application
import dev.reelbox.analytics.AppAnalytics
import dev.reelbox.data.Graph

class ReelboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppAnalytics.init(this)
        Graph.init(this)
    }
}
