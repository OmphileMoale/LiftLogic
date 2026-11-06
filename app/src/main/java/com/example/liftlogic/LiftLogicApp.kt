package com.example.liftlogic

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.liftlogic.util.AppPrefs
import kotlinx.coroutines.launch

class LiftLogicApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Apply the saved light/dark choice before any screen is shown
        AppCompatDelegate.setDefaultNightMode(AppPrefs.getNightMode(this))

        container = AppContainer(this)
        container.notifications.createChannels()

        // First launch: add the built-in exercise library and training programs
        container.appScope.launch { container.seeder.seedIfNeeded() }
    }
}
