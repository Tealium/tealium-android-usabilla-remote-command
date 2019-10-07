package com.tealium.example

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.tealium.example.helper.TealiumHelper

class PassiveFeedbackActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passive)


        findViewById<Button>(R.id.btn_usabilla_load_form_event).setOnClickListener {
            val data = mutableMapOf<String, Any>()
            data["form_id"] = findViewById<EditText>(R.id.edit_usabilla_form_id).text.toString()
            data["fragment_id"] = R.id.activity_main_frame

            TealiumHelper.trackEvent("load_form", data)
        }

        findViewById<Button>(R.id.btn_usabilla_preload_forms_event).setOnClickListener {
            val data = mutableMapOf<String, Any>()
            data["form_id"] =
                findViewById<EditText>(R.id.edit_usabilla_preload_forms_ids).text.toString()

            TealiumHelper.trackEvent("preload", data)
        }

        findViewById<Button>(R.id.btn_usabilla_dismiss).setOnClickListener {
            TealiumHelper.trackEvent("dismiss", mutableMapOf<String, Any>())
        }
    }
}