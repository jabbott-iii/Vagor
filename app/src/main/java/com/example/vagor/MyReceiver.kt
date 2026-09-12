package com.example.vagor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val message = intent.getStringExtra("message")
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}