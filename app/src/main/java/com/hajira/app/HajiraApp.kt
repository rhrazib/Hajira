package com.hajira.app

import android.app.Application
import com.hajira.app.di.AppContainer

class HajiraApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
