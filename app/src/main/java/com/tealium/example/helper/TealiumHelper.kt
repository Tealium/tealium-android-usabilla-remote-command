package com.tealium.example.helper

import android.app.Application
import android.util.Log
import android.webkit.WebView
import com.tealium.core.*
import com.tealium.dispatcher.TealiumEvent
import com.tealium.dispatcher.TealiumView

import com.tealium.lifecycle.Lifecycle
import com.tealium.remotecommanddispatcher.RemoteCommands
import com.tealium.remotecommanddispatcher.remoteCommands
import com.tealium.remotecommands.usabilla.UsabillaRemoteCommand
import com.tealium.tagmanagementdispatcher.TagManagement

/**
 * This class abstracts interaction with the Tealium library and simplifies comprehension
 */
object TealiumHelper {

    private val TAG = "TealiumHelper"

    // Identifier for the main Tealium instance
    val TEALIUM_MAIN = "main"
    lateinit var tealium: Tealium

    init {
        Log.i(TAG, " --- START --- ")
    }

    fun initialize(application: Application) {

        Log.i(TAG, "initialize(" + application.javaClass.simpleName + ")")

        WebView.setWebContentsDebuggingEnabled(true)

        val config = TealiumConfig(
            application,
            "tealiummobile",
            "usabilla-tag",
            Environment.DEV,
            modules = mutableSetOf(Modules.Lifecycle),
            dispatchers = mutableSetOf(
                Dispatchers.TagManagement,
                Dispatchers.RemoteCommands
            )
        ).apply {
            useRemoteLibrarySettings = true
        }

        tealium = Tealium.create(TEALIUM_MAIN, config) {
            val urc = UsabillaRemoteCommand(application)

            // Remote Command Tag - requires TiQ
//            remoteCommands?.add(urc)

            // JSON Remote Command - requires local filename or url to remote file
            remoteCommands?.add(urc, filename = "usabilla.json")
        }
    }

    fun trackView(viewName: String, data: Map<String, Any>?) {
        tealium.track(TealiumView(viewName, data))
    }

    fun trackEvent(eventName: String, data: Map<String, Any>?) {
        tealium.track(TealiumEvent(eventName, data))
    }

}// Not instantiatable.