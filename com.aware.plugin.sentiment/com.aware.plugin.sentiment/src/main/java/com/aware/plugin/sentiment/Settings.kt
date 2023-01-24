package com.aware.plugin.sentiment

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
        val STATUS_PLUGIN_SENTIMENT = "status_plugin_sentiment"
        val PLUGIN_SENTIMENT_PACKAGES = "plugin_sentiment_packages"
    }

    lateinit var status : CheckBoxPreference
    lateinit var packages : EditTextPreference

    @Deprecated("Deprecated in Java")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences_sentiment)
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        status = findPreference(STATUS_PLUGIN_SENTIMENT) as CheckBoxPreference
        if (Aware.getSetting(this, STATUS_PLUGIN_SENTIMENT).isEmpty())
            Aware.setSetting(this, STATUS_PLUGIN_SENTIMENT, true)
        status.isChecked = Aware.getSetting(this, STATUS_PLUGIN_SENTIMENT) == "true"

        packages = findPreference(PLUGIN_SENTIMENT_PACKAGES) as EditTextPreference
        if (Aware.getSetting(this, PLUGIN_SENTIMENT_PACKAGES).isEmpty())
            Aware.setSetting(this, PLUGIN_SENTIMENT_PACKAGES, "")
        packages.text = Aware.getSetting(this, PLUGIN_SENTIMENT_PACKAGES)
        packages.summary = Aware.getSetting(this, PLUGIN_SENTIMENT_PACKAGES)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val pref = findPreference(key)
        when (pref.key) {
            STATUS_PLUGIN_SENTIMENT -> {
                Aware.setSetting(this, key, sharedPreferences!!.getBoolean(key, false))
                status.isChecked = sharedPreferences.getBoolean(key, false)
            }
            PLUGIN_SENTIMENT_PACKAGES -> {
                Aware.setSetting(this, key, sharedPreferences!!.getString(key, ""))
                packages.text = Aware.getSetting(this, key)
                packages.summary = Aware.getSetting(this, key)
                pref.summary = packages.text
            }
        }
    }
}