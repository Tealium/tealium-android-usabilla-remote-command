package com.tealium.example

import android.app.Application
import com.tealium.example.helper.TealiumHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        TealiumHelper.initialize(this)
    }

}