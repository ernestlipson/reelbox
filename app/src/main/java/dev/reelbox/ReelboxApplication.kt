package dev.reelbox

import android.app.Application
import dev.reelbox.data.Graph

class ReelboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}
