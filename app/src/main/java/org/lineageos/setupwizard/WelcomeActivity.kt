/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The Halcyon Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard

import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.internal.app.LocaleHelper
import com.google.android.setupcompat.util.SystemBarHelper
import java.util.Locale
import org.lineageos.setupwizard.SetupWizardApp.Companion.ACTION_EMERGENCY_DIAL
import org.lineageos.setupwizard.base.SubBaseActivity
import org.lineageos.setupwizard.util.SetupWizardUtils

class WelcomeActivity : SubBaseActivity() {

    private var welcomeTheme: Resources.Theme? = null

    override fun onStartSubactivity() {
        // no-op
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        if (welcomeTheme == null) {
            theme.applyStyle(R.style.Theme_Setup_Welcome, true)
            welcomeTheme = theme
        }
        return welcomeTheme!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onSetupStart()
        SystemBarHelper.setBackButtonVisible(window, false)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )

        setupInsets()
        setupEmergencyCallButton()
        setupSkipButton()
        setupLanguageSelector()

        findViewById<View>(R.id.start).setOnClickListener { onNextPressed() }
        findViewById<View>(R.id.launch_accessibility).setOnClickListener {
            startSubactivity(Intent(ACTION_ACCESSIBILITY_SETTINGS))
        }

        val welcomeTitle: TextView = findViewById(R.id.welcome_title)
        welcomeTitle.text =
            if (SetupWizardUtils.isManagedProfile(this)) {
                getString(R.string.setup_managed_profile_welcome_message)
            } else {
                getString(R.string.setup_welcome_message, getString(R.string.os_name))
            }
    }

    override fun onResume() {
        super.onResume()
        updateLanguageSelector()
    }

    private fun setupInsets() {
        val container = findViewById<View>(R.id.welcome_container) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val horizontalPadding =
                resources.getDimensionPixelSize(R.dimen.welcome_page_padding_horizontal)
            view.setPadding(
                horizontalPadding + insets.left,
                insets.top,
                horizontalPadding + insets.right,
                insets.bottom
            )
            windowInsets
        }
    }

    private fun setupSkipButton() {
        val skipButton = findViewById<View>(R.id.skip_button) ?: return
        if (Build.TYPE != "eng") {
            skipButton.visibility = View.GONE
            return
        }
        skipButton.visibility = View.VISIBLE
        skipButton.setOnClickListener {
            SetupWizardUtils.finishSetupWizard(this@WelcomeActivity)
        }
    }

    private fun setupEmergencyCallButton() {
        val emergencyButton = findViewById<View>(R.id.emergency_call) ?: return
        if (!SetupWizardUtils.hasTelephony(this)) {
            emergencyButton.visibility = View.GONE
            return
        }
        emergencyButton.visibility = View.VISIBLE
        emergencyButton.setOnClickListener {
            startSubactivity(Intent(ACTION_EMERGENCY_DIAL))
        }
    }

    private fun setupLanguageSelector() {
        val languageSelector = findViewById<View>(R.id.language_selector) ?: return
        languageSelector.setOnClickListener {
            val existing = supportFragmentManager.findFragmentByTag(LanguagePickerDialogFragment.TAG)
            if (existing == null) {
                LanguagePickerDialogFragment.newInstance()
                    .show(supportFragmentManager, LanguagePickerDialogFragment.TAG)
            }
        }
        updateLanguageSelector()
    }

    private fun updateLanguageSelector() {
        val currentLanguageView = findViewById<TextView>(R.id.current_language) ?: return
        val currentLocale = Locale.getDefault()
        currentLanguageView.text = LocaleHelper.getDisplayName(currentLocale, currentLocale, true)
    }

    override val layoutResId: Int = R.layout.welcome_activity

    override val titleResId: Int = -1

    companion object {
        private const val ACTION_ACCESSIBILITY_SETTINGS =
            "android.settings.ACCESSIBILITY_SETTINGS_FOR_SUW"
    }
}
