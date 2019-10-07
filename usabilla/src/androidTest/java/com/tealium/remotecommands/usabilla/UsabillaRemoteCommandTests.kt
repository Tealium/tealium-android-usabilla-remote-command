package com.tealium.remotecommands.usabilla

import android.content.Intent
//import android.support.v4.app.DialogFragment
//import android.support.v4.app.Fragment
//import android.support.v4.content.LocalBroadcastManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.FlakyTest
import androidx.test.runner.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.tealium.internal.tagbridge.RemoteCommand
import com.tealium.library.Tealium
import com.usabilla.sdk.ubform.UbConstants
import com.usabilla.sdk.ubform.UsabillaFormCallback
import com.usabilla.sdk.ubform.UsabillaReadyCallback
import com.usabilla.sdk.ubform.sdk.form.FormClient
import io.mockk.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import java.lang.Exception


@RunWith(AndroidJUnit4::class)
class AndroidUsabillaRemoteCommandTests {

    private val instanceName = "test-instance"
    private val accountName = "tealiummobile"
    private val profileName = "mobile"
    private val environmentName = "dev"
    private val passiveIntent: Intent = Intent(UbConstants.INTENT_CLOSE_FORM)
    private val campaignIntent: Intent = Intent(UbConstants.INTENT_CLOSE_CAMPAIGN)

    private lateinit var config: Tealium.Config
    private lateinit var usabillaRemoteCommand: UsabillaRemoteCommand
    private lateinit var usabillaTracker: UsabillaTracker

    @Rule
    @JvmField
    var qaActivity = ActivityTestRule<QAFragmentActivity>(QAFragmentActivity::class.java)

    @Before
    fun setUp() {

        config = Tealium.Config.create(qaActivity.activity.application, accountName, profileName, environmentName)
        usabillaRemoteCommand = spyk(UsabillaRemoteCommand(instanceName,
                config,
                usabillaHttpClient = null,
                usabillaReadyCallback = null,
                autoFragmentManager = false,
                autoFeedbackHandler = false), recordPrivateCalls = true) // no call back supplied.
        usabillaTracker = mockk(relaxed = true)
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

        usabillaRemoteCommand = spyk(UsabillaRemoteCommand(instanceName,
                config,
                usabillaHttpClient = null,
                usabillaReadyCallback = callback,
                autoFragmentManager = false,
                autoFeedbackHandler = false)) // with callback
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

        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val payload = JSONObject()
        payload.put(UsabillaConstants.Keys.COMMAND_NAME, UsabillaConstants.Commands.INITIALIZE)

        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        verify {
            usabillaTracker.initialize(any())
        }

        confirmVerified(usabillaTracker)
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingSetDebug() {

        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val payload = JSONObject()
        // No Keys.DEBUG_ENABLED supplied - should default to false
        payload.put(UsabillaConstants.Keys.COMMAND_NAME, UsabillaConstants.Commands.DEBUG_ENABLED)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        // With Keys.DEBUG_ENABLED supplied as true
        payload.put(UsabillaConstants.Keys.DEBUG_ENABLED, true)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        // With Keys.DEBUG_ENABLED supplied as true
        payload.put(UsabillaConstants.Keys.DEBUG_ENABLED, false)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        verifySequence {
            usabillaTracker.setDebugEnabled(false)
            usabillaTracker.setDebugEnabled(true)
            usabillaTracker.setDebugEnabled(false)
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingLoadFeedbackForm() {

        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val payload = JSONObject()
        payload.put(UsabillaConstants.Keys.COMMAND_NAME, UsabillaConstants.Commands.LOAD_FEEDBACK_FORM)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        // No FormId, wrapper shouldn't be called.
        verify {
            usabillaTracker wasNot Called
        }

        // FormId provided, but no custom form callback
        val formId1 = "some_form_id_1"
        payload.put(UsabillaConstants.Keys.FORM_ID, formId1)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        // FormId provided, with a custom callback
        val formId2 = "some_form_id_2"
        payload.put(UsabillaConstants.Keys.FORM_ID, formId2)
        val usabillaFormCallback = mockk<UsabillaFormCallback>()
        usabillaRemoteCommand.setUsabillaFormCallback(usabillaFormCallback)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        payload.put(UsabillaConstants.Keys.FRAGMENT_ID, 100)
        usabillaRemoteCommand.setUsabillaFormCallback(null)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        verifyOrder {
            usabillaTracker.loadFeedbackForm(formId1, null, -1)
            usabillaTracker.loadFeedbackForm(formId2, usabillaFormCallback, -1)
            usabillaTracker.loadFeedbackForm(formId2, null, 100)
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingPreloadFeedbackForm() {

        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val payload = JSONObject()
        payload.put(UsabillaConstants.Keys.COMMAND_NAME, UsabillaConstants.Commands.PRELOAD_FEEDBACK_FORMS)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))
        verify {
            usabillaTracker wasNot Called
        }

        val formIds = JSONArray(mutableListOf("some_form_id_1"))
        payload.put(UsabillaConstants.Keys.FORM_ID, formIds)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        verify {
            usabillaTracker.preloadFeedbackForms(formIds)
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingRemoveCachedForms() {

        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val payload = JSONObject()
        payload.put(UsabillaConstants.Keys.COMMAND_NAME, UsabillaConstants.Commands.REMOVE_CACHED_FORMS)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        verify {
            usabillaTracker.removeCachedForms()
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingReset() {

        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val payload = JSONObject()
        payload.put(UsabillaConstants.Keys.COMMAND_NAME, UsabillaConstants.Commands.RESET)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        verify {
            usabillaTracker.reset()
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingSendEvent() {

        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val payload = JSONObject()
        payload.put(UsabillaConstants.Keys.COMMAND_NAME, UsabillaConstants.Commands.SEND_EVENT)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))

        val eventName = "my_event"
        payload.put(UsabillaConstants.Keys.EVENT_NAME, eventName)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))
        verify {
            usabillaTracker.sendEvent(null)
            usabillaTracker.sendEvent(eventName)
        }
    }

    @Throws(JSONException::class)
    @Test
    fun testOnInvokeRoutingSetCustomVariables() {

        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val payload = JSONObject()
        payload.put(UsabillaConstants.Keys.COMMAND_NAME, UsabillaConstants.Commands.SET_CUSTOM_VARIABLES)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))
        verify {
            usabillaTracker wasNot Called
        }

        val customVariables = JSONObject()
        customVariables.put("my_string", "my_string")
        customVariables.put("my_int", 10)
        customVariables.put("my_bool", false)
        customVariables.put("my_double", 100.0)
        payload.put(UsabillaConstants.Keys.CUSTOM, customVariables)
        usabillaRemoteCommand.onInvoke(RemoteCommand.Response("", "", payload))
        verify {
            usabillaTracker.setCustomVariables(customVariables)
        }
    }

    @Test
    fun testBroadcastReceiverRegistration() {
        usabillaRemoteCommand.usabillaTrackable = usabillaTracker

        val broadcastManager = LocalBroadcastManager.getInstance(qaActivity.activity.applicationContext)
        usabillaRemoteCommand.unregisterBroadcastReceivers()
        broadcastManager.sendBroadcastSync(passiveIntent)
        broadcastManager.sendBroadcastSync(campaignIntent)

        verify {
            usabillaTracker.passiveFeedbackReceiver wasNot Called
            usabillaTracker.campaignFeedbackReceiver wasNot Called
        }

        usabillaRemoteCommand.registerBroadcastReceivers()
        broadcastManager.sendBroadcastSync(passiveIntent)
        broadcastManager.sendBroadcastSync(campaignIntent)

        verify {
            usabillaTracker.passiveFeedbackReceiver.onReceive(any(), any())
            usabillaTracker.campaignFeedbackReceiver.onReceive(any(), any())
        }
    }

    @Test
    fun testLifecycleRegistration() {
        // Lifecycle registration disabled in tests by default.
        Assert.assertNull((usabillaRemoteCommand.usabillaTrackable as UsabillaTracker).fragmentManager)
        usabillaRemoteCommand.registerLifecycleCallbacks()

        // Non Fragment activity launched;
        Espresso.onView(withId(R.id.btn_next_activity)).perform(ViewActions.click())
        Assert.assertNull((usabillaRemoteCommand.usabillaTrackable as UsabillaTracker).fragmentManager)

        // Fragment activity should now be back in focus
        Espresso.pressBack()
        Assert.assertEquals((usabillaRemoteCommand.usabillaTrackable as UsabillaTracker).fragmentManager, qaActivity.activity.supportFragmentManager)
    }

    @Test
    @FlakyTest
    fun testPassiveFragmentAddRemove() {
        val wrapper = spyk(UsabillaTracker(instanceName,
                qaActivity.activity.applicationContext,
                null,
                null), recordPrivateCalls = true)

        wrapper.onActivityStarted(qaActivity.activity)
        wrapper.addPassiveFeedbackFragment(Fragment(), R.id.frame_fragment)

        Thread.sleep(200) // fragmentManager commit() is asynchronous
        Assert.assertNotNull(qaActivity.activity.supportFragmentManager.findFragmentByTag(UsabillaConstants.FRAGMENT_TAG_NAME))

        wrapper.removePassiveFeedbackFragment()
        Thread.sleep(200)
        Assert.assertNull(qaActivity.activity.supportFragmentManager.findFragmentByTag(UsabillaConstants.FRAGMENT_TAG_NAME))

        val callback = wrapper.getDefaultFormCallback(R.id.frame_fragment)
        val formClient = mockk<FormClient>(relaxed = true)
        val fragment = DialogFragment()
        every { formClient.fragment } returns fragment
        callback.formLoadSuccess(formClient)
        callback.formLoadFail()

        verify {
            wrapper.addPassiveFeedbackFragment(fragment, R.id.frame_fragment)
            wrapper["track"](UsabillaConstants.Events.USABILLA_FORM_LOADED, any<Map<String, Any>>())
            wrapper["track"](UsabillaConstants.Events.USABILLA_FORM_LOAD_ERROR, any<Map<String, Any>>())
        }
    }
}