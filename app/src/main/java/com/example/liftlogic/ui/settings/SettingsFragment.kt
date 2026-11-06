package com.example.liftlogic.ui.settings

import android.Manifest
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.BuildConfig
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentSettingsBinding
import com.example.liftlogic.databinding.ItemSettingsRowBinding
import com.example.liftlogic.databinding.ItemSwitchRowBinding
import com.example.liftlogic.notifications.ReminderScheduler
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.auth.ProfileSetupFragment
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.AppPrefs
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Units
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {

    /** A language option: display name + language tag (matches res/xml/locales_config.xml) */
    private data class Language(@StringRes val label: Int, val tag: String)

    private val languages = listOf(
        Language(R.string.language_english, "en"),
        Language(R.string.language_afrikaans, "af"),
        Language(R.string.language_zulu, "zu")
    )

    private val restOptions = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)

    /** Turning on reminders on Android 13+ needs the notification permission. */
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) setReminder(true) else bindingOrNull?.rowReminder?.toggle?.isChecked = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = findNavController()
        setupToolbar(binding.appBar.toolbar, R.string.settings_title)

        // ----- Account -----
        bindRow(binding.rowEditProfile, R.drawable.ic_person, R.string.settings_edit_profile) {
            nav.navigate(R.id.profileSetupFragment, bundleOf(ProfileSetupFragment.ARG_ONBOARDING to false))
        }
        bindRow(binding.rowChangePassword, R.drawable.ic_lock, R.string.settings_change_password) { nav.navigate(R.id.changePasswordFragment) }
        bindRow(binding.rowProfilePicture, R.drawable.ic_image, R.string.settings_profile_picture) { nav.navigate(R.id.profilePictureFragment) }

        // ----- Training -----
        bindRow(binding.rowUnits, R.drawable.ic_monitor_weight, R.string.settings_units) { chooseUnits() }
        bindRow(binding.rowRest, R.drawable.ic_timer, R.string.settings_default_rest) { chooseRest() }
        bindSwitch(binding.rowRestAlert, R.drawable.ic_notifications, R.string.settings_rest_alert, R.string.settings_rest_alert_desc)
        binding.rowRestAlert.toggle.setOnCheckedChangeListener { _, checked ->
            if (checked == app.currentUser.value?.notificationsEnabled) return@setOnCheckedChangeListener
            val userId = app.session.currentUserId ?: return@setOnCheckedChangeListener
            launchUi { app.userRepo.update(userId) { it.copy(notificationsEnabled = checked) } }
        }

        observe(app.currentUser) { user ->
            if (user == null) return@observe
            showSubtitle(binding.rowUnits, getString(if (user.units == Units.LB) R.string.unit_lb_long else R.string.unit_kg_long))
            showSubtitle(binding.rowRest, Formatters.clock(user.defaultRestSec))
            binding.rowRestAlert.toggle.isChecked = user.notificationsEnabled
        }

        // ----- Reminders -----
        bindSwitch(binding.rowReminder, R.drawable.ic_notifications, R.string.settings_daily_reminder, R.string.settings_daily_reminder_desc)
        binding.rowReminder.toggle.isChecked = AppPrefs.isReminderOn(requireContext())
        binding.rowReminder.toggle.setOnCheckedChangeListener { _, checked ->
            if (checked == AppPrefs.isReminderOn(requireContext())) return@setOnCheckedChangeListener
            if (checked && !app.notifications.canPostNotifications() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                setReminder(checked)
            }
        }
        bindRow(binding.rowReminderTime, R.drawable.ic_event, R.string.settings_reminder_time) { chooseReminderTime() }
        updateReminderRow()

        // ----- Appearance -----
        val nightNow = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        bindSwitch(binding.rowDarkMode, R.drawable.ic_dark_mode, R.string.settings_dark_mode, null)
        binding.rowDarkMode.toggle.isChecked = nightNow
        binding.rowDarkMode.toggle.setOnCheckedChangeListener { _, checked ->
            // The activity is recreated automatically with the new mode
            AppPrefs.setNightMode(requireContext(), if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }
        bindRow(binding.rowTheme, R.drawable.ic_palette, R.string.settings_theme) { nav.navigate(R.id.themeFragment) }
        bindRow(binding.rowLanguage, R.drawable.ic_language, R.string.settings_language) { showLanguageDialog() }
        showSubtitle(binding.rowLanguage, getString(languages[currentLanguageIndex()].label))

        // ----- About / logout -----
        bindRow(binding.rowAbout, R.drawable.ic_info, R.string.settings_about) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.text_about)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        bindRow(binding.rowLogout, R.drawable.ic_logout, R.string.action_log_out) {
            app.restTimer.skip()
            app.session.logout()
            nav.navigate(R.id.action_global_logout)
        }
        val errorColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorError)
        binding.rowLogout.title.setTextColor(errorColor)
        binding.rowLogout.icon.imageTintList = ColorStateList.valueOf(errorColor)

        binding.versionText.text = getString(R.string.format_version, BuildConfig.VERSION_NAME)
    }

    private fun bindRow(row: ItemSettingsRowBinding, @DrawableRes icon: Int, @StringRes title: Int, onClick: () -> Unit) {
        row.icon.setImageResource(icon)
        row.title.setText(title)
        row.root.setOnClickListener { onClick() }
    }

    private fun bindSwitch(row: ItemSwitchRowBinding, @DrawableRes icon: Int, @StringRes title: Int, @StringRes subtitle: Int?) {
        row.icon.setImageResource(icon)
        row.title.setText(title)
        row.subtitle.visibleIf(subtitle != null)
        subtitle?.let { row.subtitle.setText(it) }
        row.root.setOnClickListener { row.toggle.performClick() }
    }

    private fun showSubtitle(row: ItemSettingsRowBinding, text: String) {
        row.subtitle.visibleIf(true)
        row.subtitle.text = text
    }

    // ----- dialogs -----

    private fun chooseUnits() {
        val userId = app.session.currentUserId ?: return
        val current = if (app.unit == Units.LB) 1 else 0
        val names = arrayOf(getString(R.string.unit_kg_long), getString(R.string.unit_lb_long))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_units)
            .setSingleChoiceItems(names, current) { dialog, which ->
                dialog.dismiss()
                val unit = if (which == 1) Units.LB else Units.KG
                launchUi { app.userRepo.update(userId) { it.copy(units = unit) } }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun chooseRest() {
        val userId = app.session.currentUserId ?: return
        val current = restOptions.indexOf(app.currentUser.value?.defaultRestSec ?: 90).coerceAtLeast(0)
        val names = restOptions.map { Formatters.clock(it) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_default_rest)
            .setSingleChoiceItems(names, current) { dialog, which ->
                dialog.dismiss()
                launchUi { app.userRepo.update(userId) { it.copy(defaultRestSec = restOptions[which]) } }
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun chooseReminderTime() {
        val context = requireContext()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(AppPrefs.reminderHour(context))
            .setMinute(AppPrefs.reminderMinute(context))
            .setTitleText(R.string.settings_reminder_time)
            .build()
        picker.addOnPositiveButtonClickListener {
            AppPrefs.setReminder(context, true, picker.hour, picker.minute)
            bindingOrNull?.rowReminder?.toggle?.isChecked = true
            ReminderScheduler.apply(context)
            updateReminderRow()
            toast(R.string.text_reminder_set)
        }
        picker.show(childFragmentManager, "reminder_time")
    }

    private fun setReminder(on: Boolean) {
        val context = requireContext()
        AppPrefs.setReminder(context, on, AppPrefs.reminderHour(context), AppPrefs.reminderMinute(context))
        ReminderScheduler.apply(context)
        updateReminderRow()
        if (on) toast(R.string.text_reminder_set)
    }

    private fun updateReminderRow() {
        val b = bindingOrNull ?: return
        val context = requireContext()
        showSubtitle(b.rowReminderTime, Formatters.time(AppPrefs.reminderHour(context), AppPrefs.reminderMinute(context)))
        b.rowReminderTime.root.alpha = if (AppPrefs.isReminderOn(context)) 1f else 0.5f
    }

    private fun currentLanguageIndex(): Int {
        val current = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"
        return languages.indexOfFirst { it.tag == current }.coerceAtLeast(0)
    }

    private fun showLanguageDialog() {
        val names = languages.map { getString(it.label) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_select_language)
            .setSingleChoiceItems(names, currentLanguageIndex()) { dialog, which ->
                dialog.dismiss()
                // AppCompat restarts the activity in the chosen language
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languages[which].tag))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }
}
