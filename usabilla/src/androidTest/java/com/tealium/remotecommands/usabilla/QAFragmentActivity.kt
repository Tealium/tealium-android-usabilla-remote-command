package com.tealium.remotecommands.usabilla

import android.content.Intent
import android.os.Bundle
//import android.support.v7.app.AppCompatActivity
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

open class QAFragmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qa)

        findViewById<Button>(R.id.btn_next_activity).setOnClickListener {
            it.context.startActivity(
                Intent(it.context, QANonFragmentActivity::class.java)
            )
        }
    }
}