package com.tealium.remotecommands.usabilla

import android.app.Application
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.fragment.app.FragmentManager
import com.tealium.remotecommands.RemoteCommandContext

import com.usabilla.sdk.ubform.UbConstants

import com.usabilla.sdk.ubform.UsabillaFormCallback
import com.usabilla.sdk.ubform.UsabillaReadyCallback
import org.json.JSONArray
import org.json.JSONObject

internal interface UsabillaCommand : Application.ActivityLifecycleCallbacks, UsabillaReadyCallback {

    // API Methods
    fun initialize(appId: String?)
    fun setDebugEnabled(enabled: Boolean)
    fun sendEvent(event: String?)
    fun updateFragmentManager(fragmentManager: FragmentManager)
    fun setCustomVariables(jsonObject: JSONObject?)
    fun loadFeedbackForm(formId: String, usabillaFormCallback: UsabillaFormCallback?, fragmentId: Int)
    fun preloadFeedbackForms(formIds: JSONArray?)
    fun removeCachedForms()
    fun reset()
    fun dismiss()
    fun setDataMasking(maskList: JSONArray, maskChar: Char)

    // Feedback Receivers
    val campaignFeedbackReceiver: BroadcastReceiver
    val passiveFeedbackReceiver: BroadcastReceiver

    // Pre-built Intent Filters
    companion object {
        val CLOSER_FILTER_PASSIVE = IntentFilter(UbConstants.INTENT_CLOSE_FORM)
        val CLOSER_FILTER_CAMPAIGN = IntentFilter(UbConstants.INTENT_CLOSE_CAMPAIGN)
    }

    fun setCommandContext(remoteCommandContext: RemoteCommandContext)
}
