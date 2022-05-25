package com.tealium.remotecommands.usabilla

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.tealium.remotecommands.RemoteCommandContext

import com.usabilla.sdk.ubform.Usabilla
import com.usabilla.sdk.ubform.UsabillaFormCallback
import com.usabilla.sdk.ubform.UsabillaReadyCallback
import com.usabilla.sdk.ubform.net.http.UsabillaHttpClient
import com.usabilla.sdk.ubform.sdk.entity.FeedbackResult
import com.usabilla.sdk.ubform.sdk.form.FormClient
import org.json.JSONArray
import org.json.JSONObject

import java.util.HashMap
import java.util.LinkedList

internal class UsabillaInstance(
    private val applicationContext: Context,
    private val usabillaHttpClient: UsabillaHttpClient? = null,
    private val usabillaReadyCallback: UsabillaReadyCallback? = null,
    private var remoteCommandContext: RemoteCommandContext? = null
) : UsabillaCommand {

    private var currentActivity: Activity? = null
    private var usabillaInitialized = false

    override fun initialize(appId: String?) {
        Usabilla.initialize(applicationContext, appId, usabillaHttpClient, this)
    }

    override fun setDebugEnabled(enabled: Boolean) {
        Usabilla.debugEnabled = enabled
    }

    override fun sendEvent(event: String?) {
        event?.let {
            if (it.isNotBlank()) {
                Usabilla.sendEvent(applicationContext, it)
            }
        }
    }

    override fun updateFragmentManager(fragmentManager: FragmentManager) {
        Usabilla.updateFragmentManager(fragmentManager)
    }

    override fun reset() {
        Usabilla.resetCampaignData(applicationContext)
    }

    override fun setCustomVariables(jsonObject: JSONObject?) {
        jsonObject?.run {
            val customVariables = HashMap<String, Any>()
            this.keys().forEach { key ->
                customVariables[key] = jsonObject.optString(key)
            }

            Usabilla.customVariables = customVariables
        }
    }

    override fun loadFeedbackForm(
        formId: String,
        usabillaFormCallback: UsabillaFormCallback?,
        fragmentId: Int
    ) {
        Usabilla.loadFeedbackForm(
            formId,
            null,
            null,
            usabillaFormCallback ?: getDefaultFormCallback(fragmentId)
        )
    }

    override fun preloadFeedbackForms(formIds: JSONArray?) {
        formIds?.run {
            val formIdList = LinkedList<String>()
            for (i in 0 until formIds.length()) {
                if (!formIds.isNull(i)) {
                    formIdList.add(formIds.optString(i))
                }
            }

            Usabilla.preloadFeedbackForms(formIdList)
        }
    }

    override fun removeCachedForms() {
        Usabilla.removeCachedForms()
    }

    override fun dismiss() {
        Usabilla.dismiss(applicationContext)
    }

    override fun setDataMasking(maskList: JSONArray, maskChar: Char) {
        val list = mutableListOf<String>()
        for (i in 0 until maskList.length()) {
            try {
                maskList.getString(i)?.let {
                    list.add(it)
                }
            } catch (ignored: Exception) { }
        }
        Usabilla.setDataMasking(list, maskChar)
    }

    override val passiveFeedbackReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Remove the Fragment
                removePassiveFeedbackFragment()

                val result =
                    intent.getParcelableExtra<FeedbackResult>(FeedbackResult.INTENT_FEEDBACK_RESULT)
                result?.let {
                    trackFeedbackResult(Events.USABILLA_FORM_CLOSED, it)
                }
            }
        }
    }

    override val campaignFeedbackReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val result =
                    intent.getParcelableExtra<FeedbackResult>(FeedbackResult.INTENT_FEEDBACK_RESULT_CAMPAIGN)
                result?.let {
                    trackFeedbackResult(Events.USABILLA_FORM_CLOSED, it)
                }
            }
        }
    }

    /**
     * Adds the provided fragment to the current activity using the given fragment id and the fragment
     * name [.FRAGMENT_TAG_NAME]
     * @param fragment - Fragment to inject into the current Activity
     * @param fragmentId - Id of the view to inject the Fragment into.
     */
    fun addPassiveFeedbackFragment(fragment: Fragment?, fragmentId: Int) {
        fragment?.let { frag ->
            fragmentManager?.apply {
                this.beginTransaction()
                    .replace(fragmentId, frag, UsabillaConstants.FRAGMENT_TAG_NAME)
                    .commit()
            }
        }
    }

    /**
     * Removes the Passive Feedback Fragment using the [.FRAGMENT_TAG_NAME]
     * as the lookup.
     */
    fun removePassiveFeedbackFragment() {
        fragmentManager?.apply {
            this.findFragmentByTag(UsabillaConstants.FRAGMENT_TAG_NAME)?.let {
                this.beginTransaction()
                    .remove(it)
                    .commit()
            }
        }
    }

    /**
     * Creates the default implementation of UsabillaFormCallback. It will automatically add the
     * Passive Feedback to the fragmentId if provided. It will also track an event to the Tealium
     * instance with the results of the feedback.
     * @param fragmentId
     * @return
     */
    fun getDefaultFormCallback(fragmentId: Int): UsabillaFormCallback {
        return object : UsabillaFormCallback {

            override fun formLoadSuccess(form: FormClient) {
                addPassiveFeedbackFragment(form.fragment, fragmentId)
                track(Events.USABILLA_FORM_LOADED, null)
            }

            override fun formLoadFail() {
                track(Events.USABILLA_FORM_LOAD_ERROR, null)
            }

            override fun mainButtonTextUpdated(text: String) {

            }
        }
    }

    /**
     * Sends an event to the Tealium instance with the feedback results stored in the following keys:
     * [Events.USABILLA_ABANDONED_PAGE_INDEX]
     * [Events.USABILLA_RATING]
     * [Events.USABILLA_SENT]
     * @param eventName
     * @param feedback
     */
    private fun trackFeedbackResult(eventName: String, feedback: FeedbackResult) {
        val data = HashMap<String, Any>()
        data[Keys.USABILLA_ABANDONED_PAGE_INDEX] = feedback.abandonedPageIndex
        data[Keys.USABILLA_RATING] = feedback.rating
        data[Keys.USABILLA_SENT] = feedback.isSent

        track(eventName, data)
    }

    /**
     * Sends an event to the Tealium instance with the name [.instanceName].
     * @param eventName
     * @param data
     */
    private fun track(eventName: String, data: Map<String, Any>?) {
        remoteCommandContext?.track(eventName, data ?: mapOf<String, Any>())
    }

    fun setContext(context: RemoteCommandContext) {
        remoteCommandContext = context
    }

    override fun onUsabillaInitialized() {
        usabillaInitialized = true
        fragmentManager?.let {
            updateFragmentManager(it)
        }

        usabillaReadyCallback?.onUsabillaInitialized()
    }

    override fun setCommandContext(remoteCommandContext: RemoteCommandContext) {
        this.remoteCommandContext = remoteCommandContext
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (usabillaInitialized) {
            // Setting the fragment manager before initialisation will cause an error.
            fragmentManager?.let {
                updateFragmentManager(it)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Attempts to get the current FragmentManager from the current activity, if the current activity
     * is an instance of a FragmentActivity.
     * @return The Fragment Manager from the current Activity, else null
     */
    internal var fragmentManager: FragmentManager? = null
        get() = if (currentActivity is FragmentActivity)
            (currentActivity as FragmentActivity).supportFragmentManager
        else
            null
        private set
}
