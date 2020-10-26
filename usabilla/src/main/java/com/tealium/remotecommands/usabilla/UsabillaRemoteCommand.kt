package com.tealium.remotecommands.usabilla

import android.app.Application
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import com.tealium.remotecommands.RemoteCommand
import com.tealium.remotecommands.RemoteCommandContext
import com.usabilla.sdk.ubform.UsabillaFormCallback
import com.usabilla.sdk.ubform.UsabillaReadyCallback
import com.usabilla.sdk.ubform.net.http.UsabillaHttpClient

import org.json.JSONObject
import java.util.*

/**
 * RemoteCommand class for integrating with the Usabilla Android SDK.
 * Events will be tracked automatically for successful and unsuccessful form loads, as well as
 * passive and campaign feedback forms - the instanceName parameter is used for this purpose.
 *
 * By default we keep track of the the current FragmentManager using [android.app.Application.ActivityLifecycleCallbacks]
 * in order to inject the passive feedback form fragment, this can be disabled by setting
 * autoFragmentManager to false.
 * Two [android.content.BroadcastReceiver]s are registered to handle both Passive and Campaign feedback
 * form closures - you can disable this by setting autoFeedbackHandler to false - note that the
 * built in receivers will automatically remove the fragments once the form has been submitted or
 * dismissed.
 */
class UsabillaRemoteCommand @JvmOverloads constructor(
    private val application: Application,
    private val usabillaHttpClient: UsabillaHttpClient? = null,
    private val usabillaReadyCallback: UsabillaReadyCallback? = null,
    autoFragmentManager: Boolean = true,
    autoFeedbackHandler: Boolean = true,
    commandId: String = DEFAULT_COMMAND_ID,
    description: String = DEFAULT_COMMAND_DESC
) : RemoteCommand(commandId, description), UsabillaReadyCallback {

    private var _usabillaFormCallback: UsabillaFormCallback? = null
    internal var usabillaInstance: UsabillaCommand = UsabillaInstance(
        application,
        usabillaHttpClient,
        usabillaReadyCallback
    )

    init {
        if (autoFragmentManager) {
            registerLifecycleCallbacks()
        }
        if (autoFeedbackHandler) {
            registerBroadcastReceivers()
        }
    }

    /**
     * This method receives the payload provided by the respective RemoteCommand tag. It is
     * responsible for executing defined commands in [UsabillaConstants.Keys.COMMAND_NAME], and
     * extracting expected data from the JSON payload for each of those commands and passing onto
     * the relevant Usabilla SDK methods.
     */
    @Throws(Exception::class)
    public override fun onInvoke(response: RemoteCommand.Response) {
        val payload = response.requestPayload
        val commandList = splitCommands(payload)
        parseCommands(commandList, payload)
        response.send()
    }

    internal fun splitCommands(payload: JSONObject): Array<String> {
        val commandString = payload.optString(Keys.COMMAND_NAME, "")
        return commandString.split(UsabillaConstants.SEPARATOR).map {
            it.trim().toLowerCase(Locale.ROOT)
        }.toTypedArray()
    }

    internal fun parseCommands(commands: Array<String>, payload: JSONObject) {
        commands.forEach { command ->
            try {
                when (command) {
                    Commands.INITIALIZE -> {
                        usabillaInstance.initialize(
                            payload.optString(Keys.APP_ID, null)
                        )
                    }
                    Commands.DEBUG_ENABLED ->
                        // default to false
                        usabillaInstance.setDebugEnabled(
                            payload.optBoolean(
                                Keys.DEBUG_ENABLED,
                                false
                            )
                        )
                    Commands.DISPLAY_CAMPAIGN -> {
                        // not relevant to Android.
                    }
                    Commands.DISMISS_AUTOMATICALLY -> {
                        usabillaInstance.dismiss()
                    }
                    Commands.LOAD_FEEDBACK_FORM -> {
                        val formId = payload.optString(Keys.FORM_ID, null)
                        formId?.let {
                            usabillaInstance.loadFeedbackForm(
                                it,
                                _usabillaFormCallback,
                                payload.optInt(Keys.FRAGMENT_ID, -1)
                            )
                        }
                    }
                    Commands.PRELOAD_FEEDBACK_FORMS -> {
                        val formIds = payload.optJSONArray(Keys.FORM_ID)
                        formIds?.let {
                            usabillaInstance.preloadFeedbackForms(it)
                        }
                    }
                    Commands.REMOVE_CACHED_FORMS -> {
                        usabillaInstance.removeCachedForms()
                    }
                    Commands.RESET -> {
                        usabillaInstance.reset()
                    }
                    Commands.SEND_EVENT -> {
                        usabillaInstance.sendEvent(payload.optString(Keys.EVENT_NAME, null))
                    }
                    Commands.SET_CUSTOM_VARIABLES -> {
                        val customVars = payload.optJSONObject(Keys.CUSTOM)
                        customVars?.let {
                            usabillaInstance.setCustomVariables(it)
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.w(UsabillaConstants.TAG, "Error processing command: $command", ex)
            }
        }
    }

    /**
     * Tealium provides a default UsabillaFormCallback that will attach the Passive Feedback Form to
     * the relevant fragment id if provided in the payload at the key [Keys.FRAGMENT_ID]
     * @param usabillaFormCallback
     */
    fun setUsabillaFormCallback(usabillaFormCallback: UsabillaFormCallback?) {
        _usabillaFormCallback = usabillaFormCallback
    }

    /**
     * Lifecycle Callbacks are required to automatically keep track of the current Fragment Manager.
     * If you have unregistered them using [.unregisterLifecycleCallbacks]
     * then you can re-register them here.
     */
    fun registerLifecycleCallbacks() {
        application.registerActivityLifecycleCallbacks(usabillaInstance)
    }

    /**
     * Lifecycle Callbacks are required to automatically keep track of the current Fragment Manager.
     * If you wish to unregister them then call this method. You can re-register them using
     * [.registerLifecycleCallbacks]
     */
    fun unregisterLifecycleCallbacks() {
        application.unregisterActivityLifecycleCallbacks(usabillaInstance)
    }

    /**
     * Tealium provides a default BroadcastReceiver the closing of both Passive and Campaign feedback
     * forms. If you have unregistered them using [.unregisterBroadcastReceivers]
     * then you can re-register them using this method.
     */
    fun registerBroadcastReceivers() {
        LocalBroadcastManager.getInstance(application.applicationContext).registerReceiver(
            usabillaInstance.passiveFeedbackReceiver,
            UsabillaCommand.CLOSER_FILTER_PASSIVE
        )
        LocalBroadcastManager.getInstance(application.applicationContext).registerReceiver(
            usabillaInstance.campaignFeedbackReceiver,
            UsabillaCommand.CLOSER_FILTER_CAMPAIGN
        )
    }

    /**
     * Tealium provides a default BroadcastReceiver the closing of both Passive and Campaign feedback
     * forms. If you wish to unregister them then use this method. You can re-register them using
     * [.registerBroadcastReceivers]
     */
    fun unregisterBroadcastReceivers() {
        LocalBroadcastManager.getInstance(application.applicationContext)
            .unregisterReceiver(usabillaInstance.passiveFeedbackReceiver)
        LocalBroadcastManager.getInstance(application.applicationContext)
            .unregisterReceiver(usabillaInstance.campaignFeedbackReceiver)
    }

    override fun setContext(context: RemoteCommandContext?) {
        context?.let {
            usabillaInstance.setCommandContext(it)
        }
    }

    /**
     * Triggered once the Usabilla SDK has completed initialising. If initializing outside of Tealium
     * then be sure to call this method after creating the RemoteCommand.
     */
    override fun onUsabillaInitialized() {
        usabillaInstance.onUsabillaInitialized()
    }

    companion object {
        const val DEFAULT_COMMAND_ID = "usabilla"
        const val DEFAULT_COMMAND_DESC = "Tealium-Usabilla Remote Command"
    }
}
