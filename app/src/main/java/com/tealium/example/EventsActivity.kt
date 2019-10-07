package com.tealium.example

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.tealium.example.helper.TealiumHelper

class EventsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)

        findViewById<Button>(R.id.btn_usabilla_init_event).setOnClickListener {
            val data = mutableMapOf<String, Any>()
            data["app_id"] = findViewById<EditText>(R.id.edit_usabilla_app_id).text.toString()

            TealiumHelper.trackView("launch", data)
        }

        findViewById<Button>(R.id.btn_usabilla_custom_event).setOnClickListener {
            val eventName = findViewById<EditText>(R.id.edit_usabilla_event_name).text.toString()
            TealiumHelper.trackEvent(eventName, mutableMapOf<String, Any>())
        }

        findViewById<Button>(R.id.btn_usabilla_set_custom_variable).setOnClickListener {
            val data = mutableMapOf<String, Any>()
            val variableName = findViewById<EditText>(R.id.edit_usabilla_variable_name).text.toString()
            val variableValue = findViewById<EditText>(R.id.edit_usabilla_variable_value).text.toString()
            data[variableName] = variableValue

            TealiumHelper.trackEvent("set_variables", data)
        }
    }
}