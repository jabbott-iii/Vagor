package com.example.vagor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val button = findViewById<Button>(R.id.buttonGoToVacations)

        button.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(this@HomeActivity, VacationListActivity::class.java)
            startActivity(intent)
        })
    }
}