package com.example.app

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
//import com.example.app.BuildConfig
import com.example.app.R
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<ImageView>(R.id.button_settings_back).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findViewById<ImageView>(R.id.button_settings_back).callOnClick()
            }
        }
        this.onBackPressedDispatcher.addCallback(callback)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            darkModeSetting()
            langSetting()
            accountSetting()
//            github()
            version()
        }

        private fun darkModeSetting() {
            val currentNightMode = (resources.configuration.uiMode
                    and Configuration.UI_MODE_NIGHT_MASK)
            val sharedPreferences = requireContext().getSharedPreferences("dark", 0)

            val darkMode = findPreference<SwitchPreference>(getString(R.string.setting_dark))

            darkMode?.isChecked = sharedPreferences.getBoolean("dark_mode", currentNightMode == Configuration.UI_MODE_NIGHT_YES)

            darkMode?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    sharedPreferences.edit().putBoolean("dark_mode", true).apply()
                    true
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    sharedPreferences.edit().putBoolean("dark_mode", false).apply()
                    false
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun langSetting() {
            var lang = "ru"
            val configuration = Configuration(resources.configuration)
            if (configuration.locale.displayLanguage != "ru")
                lang = "en"
            val sharedPreferencesLang = requireContext().getSharedPreferences("lang", 0)
            val langSetting = sharedPreferencesLang.getString("lang", lang)

            val listLang = findPreference<ListPreference>(getString(R.string.setting_lang))
            val sharedPreferences = requireContext().getSharedPreferences("lang", 0)

            if (langSetting != "ru")
                listLang?.setValueIndex(0)
            else
                listLang?.setValueIndex(1)

            listLang?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == resources.getStringArray(R.array.lang_array)[0]) {
                    configuration.locale = Locale.ENGLISH
                    sharedPreferences.edit().putString("lang", "en").apply()
                } else {
                    configuration.locale = Locale("ru")
                    sharedPreferences.edit().putString("lang", "ru").apply()
                }
                resources.updateConfiguration(configuration, resources.displayMetrics)
                startActivity(Intent(this.context, SettingsActivity::class.java))
                false
            }
        }

        private fun accountSetting() {
            val account = findPreference<Preference>(getString(R.string.setting_account))
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                account?.summary = getString(R.string.account_off)
            } else {
                account?.summary = getString(
                    R.string.account_on, if (currentUser.displayName == null) getString(
                        R.string.anonymous
                    ) else currentUser.displayName)
            }

            account?.setOnPreferenceClickListener {
                if (currentUser != null) {
                    val builder = AlertDialog.Builder(this.context)
                    builder.setMessage(getString(R.string.alert_setting_account))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                this.context?.let { it1 ->
                                    AuthUI.getInstance()
                                            .signOut(it1)
                                            .addOnCompleteListener {
                                                startActivity(Intent(this.context, MainActivity::class.java))
                                            }
                                }
                            }
                            .setNegativeButton(android.R.string.cancel) { _, _ -> }
                            .show()
                    true
                } else {
                    Toast.makeText(this.context, getString(R.string.account_off), Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

//        private fun github() {
//            val github = findPreference<Preference>(getString(R.string.github_key))
//            val openURL = Intent(Intent.ACTION_VIEW)
//
//            github?.setOnPreferenceClickListener {
//                openURL.data = Uri.parse("https://github.com/xxx/xxxxx")
//                startActivity(openURL)
//                true
//            }
//        }

//        private fun contactUs() {
//            val contact = findPreference<Preference>(getString(R.string.setting_contact_key))
//            val send = Intent(Intent.ACTION_SENDTO)
//
//            contact?.setOnPreferenceClickListener {
//                send.type = "text/plain"
//                send.data = Uri.parse("mailto:xxx@gmail.com")
//
//                send.putExtra(Intent.EXTRA_SUBJECT, "Contact")
//                send.putExtra(Intent.EXTRA_TEXT, "")
//
//                startActivity(Intent.createChooser(send, "Choose Email Client..."))
//
//                true
//            }
//        }

        private fun version() {
            val version = findPreference<Preference>(getString(R.string.App_version))
            version?.summary = BuildConfig.VERSION_NAME
        }
    }
}