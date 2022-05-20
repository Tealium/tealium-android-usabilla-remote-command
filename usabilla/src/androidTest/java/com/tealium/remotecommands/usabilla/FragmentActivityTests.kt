package com.tealium.remotecommands.usabilla

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.FlakyTest
import androidx.test.rule.ActivityTestRule
import com.tealium.remotecommands.RemoteCommandContext
import com.usabilla.sdk.ubform.sdk.form.FormClient
import io.mockk.*
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FragmentActivityTests {

    @Rule
    @JvmField
    var qaActivity = ActivityTestRule<QAFragmentActivity>(QAFragmentActivity::class.java)

    private lateinit var usabillaRemoteCommand: UsabillaRemoteCommand
    private lateinit var usabillaInstance: UsabillaInstance

    @Before
    fun setUp() {
        usabillaRemoteCommand = spyk(
            UsabillaRemoteCommand(
                qaActivity.activity.application,
                usabillaHttpClient = null,
                usabillaReadyCallback = null,
                autoFragmentManager = false,
                autoFeedbackHandler = false
            ), recordPrivateCalls = true
        ) // no call back supplied.
        usabillaInstance = mockk(relaxed = true)
    }

    @Test
    fun testLifecycleRegistration() {
        // Lifecycle registration disabled in tests by default.
        Assert.assertNull((usabillaRemoteCommand.usabillaInstance as UsabillaInstance).fragmentManager)
        usabillaRemoteCommand.registerLifecycleCallbacks()

        // Non Fragment activity launched;
        Espresso.onView(ViewMatchers.withId(R.id.btn_next_activity)).perform(ViewActions.click())
        Assert.assertNull((usabillaRemoteCommand.usabillaInstance as UsabillaInstance).fragmentManager)

        // Fragment activity should now be back in focus
        Espresso.pressBack()
        Assert.assertEquals(
            (usabillaRemoteCommand.usabillaInstance as UsabillaInstance).fragmentManager,
            qaActivity.activity.supportFragmentManager
        )
    }

    @Test
    @FlakyTest
    fun testPassiveFragmentAddRemove() {
        val mockRemoteCommandContext: RemoteCommandContext = mockk()
        every { mockRemoteCommandContext.track(any(), any()) } just Runs

        val wrapper = spyk(
            UsabillaInstance(
                qaActivity.activity.applicationContext,
                null,
                null,
                mockRemoteCommandContext
            ), recordPrivateCalls = true
        )

        wrapper.onActivityStarted(qaActivity.activity)
        wrapper.addPassiveFeedbackFragment(Fragment(), R.id.frame_fragment)

        Thread.sleep(200) // fragmentManager commit() is asynchronous
        Assert.assertNotNull(
            qaActivity.activity.supportFragmentManager.findFragmentByTag(
                UsabillaConstants.FRAGMENT_TAG_NAME
            )
        )

        wrapper.removePassiveFeedbackFragment()
        Thread.sleep(200)
        Assert.assertNull(
            qaActivity.activity.supportFragmentManager.findFragmentByTag(
                UsabillaConstants.FRAGMENT_TAG_NAME
            )
        )

        val callback = wrapper.getDefaultFormCallback(R.id.frame_fragment)
        val formClient = mockk<FormClient>(relaxed = true)
        val fragment = DialogFragment()
        every { formClient.fragment } returns fragment
        callback.formLoadSuccess(formClient)
        callback.formLoadFail()

        verify {
            wrapper.addPassiveFeedbackFragment(fragment, R.id.frame_fragment)
            wrapper["track"](Events.USABILLA_FORM_LOADED, any<Map<String, Any>>())
            wrapper["track"](
                Events.USABILLA_FORM_LOAD_ERROR,
                any<Map<String, Any>>()
            )
        }
    }
}