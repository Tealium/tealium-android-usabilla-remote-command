@file:JvmName("UsabillaConstants")

package com.tealium.remotecommands.usabilla

object UsabillaConstants {

    const val TAG = "Tealium-Usabilla"
    const val SEPARATOR = ","
    const val FRAGMENT_TAG_NAME = "tealium_usabilla_passive_feedback"
}

object Commands {

    const val INITIALIZE = "initialize"
    const val SEND_EVENT = "sendevent"
    const val DEBUG_ENABLED = "debugenabled"
    const val DISPLAY_CAMPAIGN = "displaycampaign"
    const val LOAD_FEEDBACK_FORM = "loadfeedbackform"
    const val PRELOAD_FEEDBACK_FORMS = "preloadfeedbackforms"
    const val REMOVE_CACHED_FORMS = "removecachedforms"
    const val DISMISS_AUTOMATICALLY = "dismissautomatically"
    const val SET_CUSTOM_VARIABLES = "setcustomvariables"
    const val RESET = "resetcampaigndata"
}

object Keys {

    const val COMMAND_NAME = "command_name"

    const val APP_ID = "appId"
    const val EVENT_NAME = "event"
    const val DEBUG_ENABLED = "debugEnabled"
    const val FORM_ID = "formId"
    const val FORM_IDS = "formIds"
    const val CUSTOM = "custom"
    const val FRAGMENT_ID = "fragmentId"

    const val USABILLA_RATING = "usabilla_rating"
    const val USABILLA_ABANDONED_PAGE_INDEX = "usabilla_abandoned_page_index"
    const val USABILLA_SENT = "usabilla_sent"
}

object Events {
    const val USABILLA_FORM_CLOSED = "usabilla_form_closed"
    const val USABILLA_FORM_LOAD_ERROR = "usabilla_form_load_error"
    const val USABILLA_FORM_LOADED = "usabilla_form_did_load"
}
