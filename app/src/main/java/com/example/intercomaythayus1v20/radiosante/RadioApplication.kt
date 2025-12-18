package fr.maythayus.intercom2sos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import fr.maythayus.intercom2sos.BuildConfig
import timber.log.Timber

@HiltAndroidApp
class RadioApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}