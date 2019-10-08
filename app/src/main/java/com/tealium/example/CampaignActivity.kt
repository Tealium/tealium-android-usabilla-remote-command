package com.tealium.example

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.tealium.example.helper.TealiumHelper

class CampaignActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_campaign)

        findViewById<Button>(R.id.btn_usabilla_campaign_event).setOnClickListener {
            val campaignEvent =
                findViewById<EditText>(R.id.edit_usabilla_campaign_event).text.toString()
            TealiumHelper.trackEvent(campaignEvent, mutableMapOf<String, Any>())
        }

        findViewById<Button>(R.id.btn_usabilla_campaign_reset).setOnClickListener {
            TealiumHelper.trackEvent("reset", mutableMapOf<String, Any>())
        }
    }
}