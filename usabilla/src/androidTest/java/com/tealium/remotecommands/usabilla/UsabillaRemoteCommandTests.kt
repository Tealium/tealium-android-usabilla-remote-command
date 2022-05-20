package com.tealium.remotecommands.usabilla

import android.app.Application
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.tealium.remotecommands.RemoteCommand
import com.usabilla.sdk.ubform.UbConstants
import com.usabilla.sdk.ubform.UsabillaFormCallback
import com.usabilla.sdk.ubform.UsabillaReadyCallback
import io.mockk.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.*
import java.lang.Exception

class AndroidUsabillaRemoteCommandTests {

    private val passiveIntent: Intent = Intent(UbConstants.INTENT_CLOSE_FORM)
    private val campaignIntent: Intent = Intent(UbConstants.INTENT_CLOSE_CAMPAIGN)

    private lateinit var usabillaRemoteCommand: UsabillaRemoteCommand
    private lateinit var usabillaInstance: UsabillaInstance

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        usabillaRemoteCommand = spyk(
            UsabillaRemoteCommand(
                app,
                usabillaHttpClient = null,
                usabillaReadyCallback = null,
                autoFragmentManager = false,
                autoFeedbackHandler = false
            ), recordPrivateCalls = true
        ) // no call back supplied.
        usabillaInstance = mockk(relaxed = true)
    }

    @Test
    fun testInitWithoutCallback() {
        try {
            usabillaRemoteCommand.onUsabillaInitialized()
        } catch (ex: Exception) {
            Assert.fail("Method should not throw an exception.")
        }
    }

    @Test
    fun testInitWithCallback() {
        val callback = mockk<UsabillaReadyCallback>()
        every { callback.onUsabillaInitialized() } returns Unit

        usabillaRemoteCommand = spyk(
            UsabillaRemoteCommand(
                app,
                usabillaHttpClient = null,
                usabillaReadyCallback = callback,
                autoFragmentManager = false,
                autoFeedbackHandler = false
            )
        ) // with callback
        verify {
            callback wasNot Called
        }

        usabillaRemoteCommand.onUsabillaInitialized()
        verify {
            callback.onUsabillaInitialized()
        }

        confirmVerified(callback)
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingInitialise() {

        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        payload.put(Keys.COMMAND_NAME, Commands.INITIALIZE)

        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        verify {
            usabillaInstance.initialize(any())
        }

        confirmVerified(usabillaInstance)
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingSetDebug() {

        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        // No Keys.DEBUG_ENABLED supplied - should default to false
        payload.put(Keys.COMMAND_NAME, Commands.DEBUG_ENABLED)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        // With Keys.DEBUG_ENABLED supplied as true
        payload.put(Keys.DEBUG_ENABLED, true)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        // With Keys.DEBUG_ENABLED supplied as true
        payload.put(Keys.DEBUG_ENABLED, false)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        verifySequence {
            usabillaInstance.setDebugEnabled(false)
            usabillaInstance.setDebugEnabled(true)
            usabillaInstance.setDebugEnabled(false)
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingLoadFeedbackForm() {

        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        payload.put(
            Keys.COMMAND_NAME,
            Commands.LOAD_FEEDBACK_FORM
        )
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        // No FormId, wrapper shouldn't be called.
        verify {
            usabillaInstance wasNot Called
        }

        // FormId provided, but no custom form callback
        val formId1 = "some_form_id_1"
        payload.put(Keys.FORM_ID, formId1)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        // FormId provided, with a custom callback
        val formId2 = "some_form_id_2"
        payload.put(Keys.FORM_ID, formId2)
        val usabillaFormCallback = mockk<UsabillaFormCallback>()
        usabillaRemoteCommand.setUsabillaFormCallback(usabillaFormCallback)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        payload.put(Keys.FRAGMENT_ID, 100)
        usabillaRemoteCommand.setUsabillaFormCallback(null)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        verifyOrder {
            usabillaInstance.loadFeedbackForm(formId1, null, -1)
            usabillaInstance.loadFeedbackForm(formId2, usabillaFormCallback, -1)
            usabillaInstance.loadFeedbackForm(formId2, null, 100)
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingPreloadFeedbackForm() {

        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        payload.put(
            Keys.COMMAND_NAME,
            Commands.PRELOAD_FEEDBACK_FORMS
        )
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))
        verify {
            usabillaInstance wasNot Called
        }

        val formIds = JSONArray(mutableListOf("some_form_id_1"))
        payload.put(Keys.FORM_ID, formIds)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        verify {
            usabillaInstance.preloadFeedbackForms(formIds)
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingRemoveCachedForms() {

        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        payload.put(
            Keys.COMMAND_NAME,
            Commands.REMOVE_CACHED_FORMS
        )
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        verify {
            usabillaInstance.removeCachedForms()
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingReset() {

        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        payload.put(Keys.COMMAND_NAME, Commands.RESET)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        verify {
            usabillaInstance.reset()
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingSendEvent() {

        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        payload.put(Keys.COMMAND_NAME, Commands.SEND_EVENT)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        val eventName = "my_event"
        payload.put(Keys.EVENT_NAME, eventName)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))
        verify {
            usabillaInstance.sendEvent(null)
            usabillaInstance.sendEvent(eventName)
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingSetCustomVariables() {

        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        payload.put(
            Keys.COMMAND_NAME,
            Commands.SET_CUSTOM_VARIABLES
        )
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))
        verify {
            usabillaInstance wasNot Called
        }

        val customVariables = JSONObject()
        customVariables.put("my_string", "my_string")
        customVariables.put("my_int", 10)
        customVariables.put("my_bool", false)
        customVariables.put("my_double", 100.0)
        payload.put(Keys.CUSTOM, customVariables)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))
        verify {
            usabillaInstance.setCustomVariables(customVariables)
        }
    }

    @Test
    fun testBroadcastReceiverRegistration() {
        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val broadcastManager =
            LocalBroadcastManager.getInstance(app)
        usabillaRemoteCommand.unregisterBroadcastReceivers()
        broadcastManager.sendBroadcastSync(passiveIntent)
        broadcastManager.sendBroadcastSync(campaignIntent)

        verify {
            usabillaInstance.passiveFeedbackReceiver wasNot Called
            usabillaInstance.campaignFeedbackReceiver wasNot Called
        }

        usabillaRemoteCommand.registerBroadcastReceivers()
        broadcastManager.sendBroadcastSync(passiveIntent)
        broadcastManager.sendBroadcastSync(campaignIntent)

        verify {
            usabillaInstance.passiveFeedbackReceiver.onReceive(any(), any())
            usabillaInstance.campaignFeedbackReceiver.onReceive(any(), any())
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingSetDataMasking() {
        usabillaRemoteCommand.usabillaInstance = usabillaInstance

        val payload = JSONObject()
        // No data mask params provided, no method should be called
        payload.put(Keys.COMMAND_NAME, Commands.SET_DATA_MASKING)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        val maskList = JSONArray().apply {
            put("key1")
            put("key2")
        }
        // No mask char param provided, no method should be called
        payload.put(Keys.MASK_LIST, maskList)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        // Mask Char provided
        payload.put(Keys.MASK_CHAR, "*")
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))
        payload.put(Keys.MASK_CHAR, "%*")
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))
        payload.put(Keys.MASK_CHAR, "% ")
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        // Mask Char provided, but no mask list; no method called
        payload.remove(Keys.MASK_LIST)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response(null, "", "", payload))

        verifySequence {
            usabillaInstance.setDataMasking(maskList, '*')
            usabillaInstance.setDataMasking(maskList, '%')
            usabillaInstance.setDataMasking(maskList, '%')
        }
    }
}