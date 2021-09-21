package com.aware.plugin.sentimental

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import com.aware.Aware
import com.aware.ui.AppCompatPreferenceActivity

class Settings : AppCompatPreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        val STATUS_PLUGIN_SENTIMENTAL = "status_plugin_sentimental"
        val PLUGIN_SENTIMENTAL_PACKAGES = "plugin_sentimental_packages"
    }

    lateinit var status : CheckBoxPreference
    lateinit var packages : EditTextPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences_sentimental)
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        status = findPreference(STATUS_PLUGIN_SENTIMENTAL) as CheckBoxPreference
        if (Aware.getSetting(this, STATUS_PLUGIN_SENTIMENTAL).isEmpty())
            Aware.setSetting(this, STATUS_PLUGIN_SENTIMENTAL, true)
        status.isChecked = Aware.getSetting(this, STATUS_PLUGIN_SENTIMENTAL) == "true"

        packages = findPreference(PLUGIN_SENTIMENTAL_PACKAGES) as EditTextPreference
        if (Aware.getSetting(this, PLUGIN_SENTIMENTAL_PACKAGES).isEmpty())
            Aware.setSetting(this, PLUGIN_SENTIMENTAL_PACKAGES, "")
        packages.text = Aware.getSetting(this, PLUGIN_SENTIMENTAL_PACKAGES)
        packages.summary = Aware.getSetting(this, PLUGIN_SENTIMENTAL_PACKAGES)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val pref = findPreference(key)
        when (pref.key) {
            STATUS_PLUGIN_SENTIMENTAL -> {
                Aware.setSetting(this, key, sharedPreferences!!.getBoolean(key, false))
                status.isChecked = sharedPreferences.getBoolean(key, false)
            }
            PLUGIN_SENTIMENTAL_PACKAGES -> {
                Aware.setSetting(this, key, sharedPreferences!!.getString(key, ""))
                packages.text = Aware.getSetting(this, key)
                packages.summary = Aware.getSetting(this, key)
                pref.summary = packages.text
            }
        }
    }
}