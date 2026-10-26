package com.example.vagor

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setEdgeToEdgeContentView(R.layout.activity_settings)

        val switchDarkMode = findViewById<MaterialSwitch>(R.id.switchDarkMode)
        switchDarkMode.isChecked = ThemeSettings.isDarkMode(this)
        // Changing the night mode recreates every open screen in the new theme.
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeSettings.setDarkMode(this, isChecked)
        }

        findViewById<Button>(R.id.buttonPrivacyPolicy).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.privacy_policy)
                .setMessage(R.string.privacy_policy_text)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}
