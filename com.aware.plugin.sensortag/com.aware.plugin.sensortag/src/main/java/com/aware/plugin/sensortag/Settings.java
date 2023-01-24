package com.aware.plugin.sensortag;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;
import com.aware.ui.AppCompatPreferenceActivity;

public class Settings extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * State of this plugin
     */
    public static final String FREQUENCY_PLUGIN_SENSORTAG = "frequency_plugin_sensortag";
    public static final String STATUS_PLUGIN_SENSORTAG = "status_plugin_sensortag";

    private CheckBoxPreference status;
    private ListPreference frequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_sensortag);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_SENSORTAG);
        frequency = (ListPreference) findPreference(FREQUENCY_PLUGIN_SENSORTAG);

        if (Aware.getSetting(this, STATUS_PLUGIN_SENSORTAG).length() == 0) {
            Aware.setSetting(this, STATUS_PLUGIN_SENSORTAG, true);
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_SENSORTAG).equals("true"));

        if (Aware.getSetting(this, FREQUENCY_PLUGIN_SENSORTAG).length() == 0) {
            Aware.setSetting(this, FREQUENCY_PLUGIN_SENSORTAG, "30");
        }
        frequency.setSummary(Aware.getSetting(this, FREQUENCY_PLUGIN_SENSORTAG) + " Hz");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = (Preference) findPreference(key);

        if (preference.getKey().equals(STATUS_PLUGIN_SENSORTAG)) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (preference.getKey().equals(FREQUENCY_PLUGIN_SENSORTAG)) {
            Aware.setSetting(this, key, sharedPreferences.getString(key, "30"));
            preference.setSummary(Aware.getSetting(this, FREQUENCY_PLUGIN_SENSORTAG) + " Hz");
        }

        if (Aware.getSetting(this, STATUS_PLUGIN_SENSORTAG).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.sensortag");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.sensortag");
        }
    }
}
