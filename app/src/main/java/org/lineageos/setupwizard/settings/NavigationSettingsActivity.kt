/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2022-2025 The LeafOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.settings

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Bundle
import android.os.SystemProperties
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import com.airbnb.lottie.LottieAnimationView
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.SetupWizardApp
import org.lineageos.setupwizard.SetupWizardApp.Companion.NAVIGATION_OPTION_KEY
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils

class NavigationSettingsActivity : BaseSetupWizardActivity() {

    private var selection: String = NAV_BAR_MODE_GESTURAL_OVERLAY

    private lateinit var hideGesturalHint: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navBarEnabled = hasNavigationBar(this)

        glifLayout.setDescriptionText(getString(R.string.navigation_summary))
        setNextText(R.string.next)

        var available = 3
        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            findViewById<View>(R.id.radio_gesture).visibility = View.GONE
            findViewById<RadioButton>(R.id.radio_sw_keys).isChecked = true
            available--
        }

        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            findViewById<View>(R.id.radio_sw_keys).visibility = View.GONE
            available--
        }

        if (!navBarEnabled || available <= 1) {
            SetupWizardApp.settingsBundle.putString(
                NAVIGATION_OPTION_KEY,
                NAV_BAR_MODE_3BUTTON_OVERLAY,
            )
            finishAction(RESULT_OK)
        }

        val navigationIllustration = findViewById<LottieAnimationView>(R.id.navigation_illustration)
        val radioGroup = findViewById<RadioGroup>(R.id.navigation_radio_group)
        hideGesturalHint = findViewById(R.id.hide_navigation_hint)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_gesture -> {
                    selection = NAV_BAR_MODE_GESTURAL_OVERLAY
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_fully_gestural)
                    revealHintCheckbox()
                }

                R.id.radio_sw_keys -> {
                    selection = NAV_BAR_MODE_3BUTTON_OVERLAY
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_3_button)
                    hideHintCheckBox()
                }
            }
            navigationIllustration.playAnimation()
        }
    }

    private fun revealHintCheckbox() {
        hideGesturalHint.animate().cancel()

        if (hideGesturalHint.visibility == View.VISIBLE) {
            return
        }

        hideGesturalHint.visibility = View.VISIBLE
        hideGesturalHint.alpha = 0.0f
        hideGesturalHint.animate().translationY(0f).alpha(1.0f).setListener(null)
    }

    private fun hideHintCheckBox() {
        if (hideGesturalHint.visibility == View.INVISIBLE) {
            return
        }

        hideGesturalHint
            .animate()
            .translationY(-hideGesturalHint.height.toFloat())
            .alpha(0.0f)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        hideGesturalHint.visibility = View.INVISIBLE
                    }
                }
            )
    }

    override fun onNextPressed() {
        SetupWizardApp.settingsBundle.putString(NAVIGATION_OPTION_KEY, selection)
        val hideHint = hideGesturalHint.isChecked
        Settings.System.putIntForUser(
            contentResolver,
            Settings.System.NAVIGATION_BAR_HINT,
            if (hideHint) 0 else 1,
            UserHandle.USER_CURRENT,
        )
        super.onNextPressed()
    }

    override val layoutResId: Int = R.layout.setup_navigation

    override val titleResId: Int = R.string.setup_navigation

    override val iconResId: Int = R.drawable.ic_navigation

    companion object {
        fun hasNavigationBar(context: Context): Boolean {
            val resources = context.resources
            val resIdShow = resources.getIdentifier("config_showNavigationBar", "bool", "android")
            val hasNavigationBar = if (resIdShow > 0) resources.getBoolean(resIdShow) else false
            return hasNavigationBar || "1" == SystemProperties.get("qemu.hw.mainkeys")
        }
    }
}
