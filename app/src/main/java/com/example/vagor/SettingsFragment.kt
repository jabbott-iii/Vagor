package com.example.vagor

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

// The Settings tab on the home screen.
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchDarkMode = view.findViewById<MaterialSwitch>(R.id.switchDarkMode)
        switchDarkMode.isChecked = ThemeSettings.isDarkMode(requireContext())
        // Changing the night mode recreates every open screen in the new theme; the home screen
        // comes back on this tab.
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeSettings.setDarkMode(requireContext(), isChecked)
        }

        view.findViewById<Button>(R.id.buttonPrivacyPolicy).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.privacy_policy)
                .setMessage(R.string.privacy_policy_text)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}
