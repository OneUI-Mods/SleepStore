package dev.ryuya.sleepstore

import android.app.Application
import com.google.android.material.color.DynamicColors

class SleepStoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FrameworkServiceConnection.register(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
