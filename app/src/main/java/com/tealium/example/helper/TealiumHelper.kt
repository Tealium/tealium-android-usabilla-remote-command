package com.tealium.example.helper

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.webkit.WebView

import com.tealium.internal.data.Dispatch
import com.tealium.internal.listeners.WebViewLoadedListener
import com.tealium.internal.tagbridge.RemoteCommand
import com.tealium.library.DispatchValidator
import com.tealium.library.Tealium

import com.tealium.lifecycle.LifeCycle
import com.tealium.remotecommands.usabilla.UsabillaRemoteCommand

/**
 * This class abstracts interaction with the Tealium library and simplifies comprehension
 */
object TealiumHelper {

    private val TAG = "TealiumHelper"

    private val KEY_TEALIUM_INIT_COUNT = "tealium_init_count"
    private val KEY_TEALIUM_INITIALIZED = "tealium_initialized"


    // Identifier for the main Tealium instance
    val TEALIUM_MAIN = "main"

    init {
        Log.i(TAG, " --- START --- ")
    }

    fun initialize(application: Application) {

        Log.i(TAG, "initialize(" + application.javaClass.simpleName + ")")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }


        val config = Tealium.Config.create(application, "tealiummobile", "usabilla-tag", "dev")

        // (OPTIONAL) Get the WebView with UTag loaded
        config.eventListeners.add(createWebViewLoadedListener())

        LifeCycle.setupInstance(TEALIUM_MAIN, config, true)

        // (OPTIONAL) Control how the library treats views/links
        config.dispatchValidators.add(createDispatchValidator())

        val urc = UsabillaRemoteCommand(TEALIUM_MAIN, config)

        val instance = Tealium.createInstance(TEALIUM_MAIN, config)


        // (OPTIONAL) Enhanced integrations
        instance.addRemoteCommand(createLoggerRemoteCommand())

        // (OPTIONAL) Use tealium.getDataSources().getPersistentDataSources() to set/modify lifetime values
        val sp = instance.dataSources.persistentDataSources
        sp.edit().putInt(KEY_TEALIUM_INIT_COUNT, sp.getInt(KEY_TEALIUM_INIT_COUNT, 0) + 1).apply()

        // (OPTIONAL) Use tealium.getDataSources().getVolatileDataSources() to set/modify runtime only values
        instance.dataSources.volatileDataSources[KEY_TEALIUM_INITIALIZED] =
            System.currentTimeMillis()

        instance.addRemoteCommand(urc)

    }

    fun trackView(viewName: String, data: Map<String, *>) {
        val instance = Tealium.getInstance(TEALIUM_MAIN)

        // Instance can be remotely destroyed through publish settings
        if (instance != null) {
            instance.trackView(viewName, data)
        } else {
            Log.w(TAG, "Tried to call trackView on null Tealium Instance")
        }
    }

    fun trackEvent(eventName: String, data: Map<String, *>) {
        val instance = Tealium.getInstance(TEALIUM_MAIN)

        // Instance can be remotely destroyed through publish settings
        if (instance != null) {
            instance.trackEvent(eventName, data)
        } else {
            Log.w(TAG, "Tried to call trackView on null Tealium Instance")
        }
    }

    private fun createWebViewLoadedListener(): WebViewLoadedListener {
        return object : WebViewLoadedListener {
            override fun onWebViewLoad(webView: WebView, success: Boolean) {
                Log.d(
                    TAG, "WebView " + webView +
                            if (success) " loaded successfully" else "failed to load"
                )

                Log.d(TAG, webView.originalUrl)
            }

            override fun toString(): String {
                return "LoggingWebViewLoadListener"
            }
        }
    }

    private fun createDispatchValidator(): DispatchValidator {
        return object : DispatchValidator() {
            protected override fun shouldDrop(dispatch: Dispatch?): Boolean {

                // Drop any desired dispatches here by returning true. (Never queued nor sent)
                return super.shouldDrop(dispatch)
            }

            protected override fun shouldQueue(dispatch: Dispatch?, shouldQueue: Boolean): Boolean {

                return super.shouldQueue(dispatch, shouldQueue)
            }

            override fun toString(): String {
                return "CustomDispatchValidator"
            }
        }
    }

    private fun createLoggerRemoteCommand(): RemoteCommand {
        return object : RemoteCommand("logger", "Logs dispatches") {
            @Throws(Exception::class)
            protected override fun onInvoke(response: RemoteCommand.Response) {
                val message = response.getRequestPayload()
                    .optString("message", "no_message")
                Log.i(TAG, "RemoteCommand Message: $message")
            }

            override fun toString(): String {
                return "LoggerRemoteCommand"
            }
        }
    }
}// Not instantiatable.
