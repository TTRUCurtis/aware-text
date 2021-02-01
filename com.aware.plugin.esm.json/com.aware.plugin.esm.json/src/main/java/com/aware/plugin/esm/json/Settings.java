package com.aware.plugin.esm.json;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.aware.Aware;
import com.aware.ui.AppCompatPreferenceActivity;

public class Settings extends AppCompatPreferenceActivity {
    public static final String DEFAULT_URL = "https://aware.lucidcardinality.com/esm/example.json";

    public static final String STATUS_PLUGIN_ESM_JSON = "status_plugin_esm_json";
    public static final String URL_PLUGIN_ESM_JSON = "url_plugin_esm_json";

    private static CheckBoxPreference status;
    private static EditTextPreference url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_esm_json);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_ESM_JSON);
        if (Aware.getSetting(this, STATUS_PLUGIN_ESM_JSON).length() == 0) {
            Aware.setSetting(this, STATUS_PLUGIN_ESM_JSON, true);
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_ESM_JSON).equals("true"));

        url = (EditTextPreference) findPreference(URL_PLUGIN_ESM_JSON);
        if (Aware.getSetting(this, URL_PLUGIN_ESM_JSON).length() == 0) {
            Aware.setSetting(this, URL_PLUGIN_ESM_JSON, DEFAULT_URL);
        }
        url.setText(Aware.getSetting(this, URL_PLUGIN_ESM_JSON));
        url.setSummary(Aware.getSetting(this, URL_PLUGIN_ESM_JSON));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);

        if (preference.getKey().equals(STATUS_PLUGIN_ESM_JSON)) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            status.setChecked(sharedPreferences.getBoolean(key, false));
        }

        if (preference.getKey().equals(URL_PLUGIN_ESM_JSON)) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, DEFAULT_URL));
        }

        if (Aware.getSetting(this, STATUS_PLUGIN_ESM_JSON).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.esm.json");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.esm.json");
        }
    }
}
