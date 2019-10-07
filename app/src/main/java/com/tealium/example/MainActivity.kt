package com.tealium.example

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_launch_passive_feedback_activity).setOnClickListener {
            startActivity(Intent(it.context, PassiveFeedbackActivity::class.java))
        }

        findViewById<Button>(R.id.btn_launch_campaign_feedback_activity).setOnClickListener {
            startActivity(Intent(it.context, CampaignActivity::class.java))
        }

        findViewById<Button>(R.id.btn_launch_events_activity).setOnClickListener {
            startActivity(Intent(it.context, EventsActivity::class.java))
        }
    }
}
