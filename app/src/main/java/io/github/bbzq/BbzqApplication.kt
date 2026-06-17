package io.github.bbzq

import android.app.Application

class BbzqApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ModuleRemotePreferences.init(this)
    }
}
