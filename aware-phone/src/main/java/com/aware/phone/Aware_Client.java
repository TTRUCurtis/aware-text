
package com.aware.phone;


import android.app.Dialog;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.*;
import android.provider.Settings;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.phone.ui.Aware_Activity;
import com.aware.phone.ui.AwareParticipant;
import com.aware.phone.ui.AwareJoinStudy;
import com.aware.phone.ui.Aware_Participant;
import com.aware.phone.ui.onboarding.JoinStudyActivity;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Https;
import com.aware.utils.SSLManager;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author df
 */
public class Aware_Client extends Aware_Activity implements SharedPreferences.OnSharedPreferenceChangeListener, PermissionsHandler.PermissionCallback {

    private static Hashtable<Integer, Boolean> listSensorType;
    private static SharedPreferences prefs;

    private static final Hashtable<String, Integer> optionalSensors = new Hashtable<>();

    private final Aware.AndroidPackageMonitor packageMonitor = new Aware.AndroidPackageMonitor();

    public static final String EXTRA_REDIRECT_SERVICE = "redirect_service";
    public static final String ACTION_AWARE_PERMISSIONS_CHECK = "ACTION_AWARE_PERMISSIONS_CHECK";
    private PermissionsHandler permissionsHandler;
    private String redirectActivityComponent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("com.aware.phone", Context.MODE_PRIVATE);
        addPreferencesFromResource(R.xml.aware_preferences);

        setContentView(R.layout.aware_ui);

        optionalSensors.put(Aware_Preferences.STATUS_ACCELEROMETER, Sensor.TYPE_ACCELEROMETER);
        optionalSensors.put(Aware_Preferences.STATUS_SIGNIFICANT_MOTION, Sensor.TYPE_ACCELEROMETER);
        optionalSensors.put(Aware_Preferences.STATUS_BAROMETER, Sensor.TYPE_PRESSURE);
        optionalSensors.put(Aware_Preferences.STATUS_GRAVITY, Sensor.TYPE_GRAVITY);
        optionalSensors.put(Aware_Preferences.STATUS_GYROSCOPE, Sensor.TYPE_GYROSCOPE);
        optionalSensors.put(Aware_Preferences.STATUS_LIGHT, Sensor.TYPE_LIGHT);
        optionalSensors.put(Aware_Preferences.STATUS_LINEAR_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION);
        optionalSensors.put(Aware_Preferences.STATUS_MAGNETOMETER, Sensor.TYPE_MAGNETIC_FIELD);
        optionalSensors.put(Aware_Preferences.STATUS_PROXIMITY, Sensor.TYPE_PROXIMITY);
        optionalSensors.put(Aware_Preferences.STATUS_ROTATION, Sensor.TYPE_ROTATION_VECTOR);
        optionalSensors.put(Aware_Preferences.STATUS_TEMPERATURE, Sensor.TYPE_AMBIENT_TEMPERATURE);

        SensorManager manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ALL);
        listSensorType = new Hashtable<>();
        for (int i = 0; i < sensors.size(); i++) {
            listSensorType.put(sensors.get(i).getType(), true);
        }

        IntentFilter awarePackages = new IntentFilter();
        awarePackages.addAction(Intent.ACTION_PACKAGE_ADDED);
        awarePackages.addAction(Intent.ACTION_PACKAGE_REMOVED);
        awarePackages.addDataScheme("package");
        registerReceiver(packageMonitor, awarePackages);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, final Preference preference) {
        if (preference instanceof PreferenceScreen) {
            Dialog subpref = ((PreferenceScreen) preference).getDialog();
            ViewGroup root = (ViewGroup) subpref.findViewById(android.R.id.content).getParent();
            Toolbar toolbar = new Toolbar(this);
            toolbar.setBackgroundColor(ContextCompat.getColor(preferenceScreen.getContext(), R.color.primary));
            toolbar.setTitleTextColor(ContextCompat.getColor(preferenceScreen.getContext(), android.R.color.white));
            toolbar.setTitle(preference.getTitle());
            root.addView(toolbar, 0); //add to the top

            subpref.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    new SettingsSync().execute(preference);
                }
            });
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String value = "";
        Map<String, ?> keys = sharedPreferences.getAll();
        if (keys.containsKey(key)) {
            Object entry = keys.get(key);
            if (entry instanceof Boolean)
                value = String.valueOf(sharedPreferences.getBoolean(key, false));
            else if (entry instanceof String)
                value = String.valueOf(sharedPreferences.getString(key, "error"));
            else if (entry instanceof Integer)
                value = String.valueOf(sharedPreferences.getInt(key, 0));
        }

        Aware.setSetting(getApplicationContext(), key, value);
        Preference pref = findPreference(key);
        if (CheckBoxPreference.class.isInstance(pref)) {
            CheckBoxPreference check = (CheckBoxPreference) findPreference(key);
            check.setChecked(Aware.getSetting(getApplicationContext(), key).equals("true"));

            //update the parent to show active/inactive
            new SettingsSync().execute(pref);

            //Start/Stop sensor
            Aware.startAWARE(getApplicationContext());
        }
        if (EditTextPreference.class.isInstance(pref)) {
            EditTextPreference text = (EditTextPreference) findPreference(key);
            text.setText(Aware.getSetting(getApplicationContext(), key));
        }
        if (ListPreference.class.isInstance(pref)) {
            ListPreference list = (ListPreference) findPreference(key);
            list.setSummary(list.getEntry());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsHandler.handlePermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPermissionGranted() {

            Intent redirectActivity = new Intent();
            String[] component = redirectActivityComponent.split("/");
            redirectActivity.setComponent(new ComponentName(component[0], component[1]));
            startActivity(redirectActivity);
    }

    @Override
    public void onPermissionDenied(@Nullable List<String> deniedPermissions) {
        new AlertDialog.Builder(this)
                .setTitle("Grant Camera Permissions Manually")
                .setMessage("Proceed to settings, click on permissions, and select camera. " +
                        "Please select \"Allow only while using the app\" or \"ask every time\".")
                .setPositiveButton(
                        "Proceed to Settings",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(
                                        new Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null)
                                        )
                                );
                            }
                        })
                .setNegativeButton(
                        "Cancel joining study",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        }
                )
                .show();

    }

    @Override
    public void onPermissionDeniedWithRationale(@Nullable List<String> deniedPermissions) {
        new AlertDialog.Builder(this)
                .setTitle("Permission required to use camera")
                .setMessage("Press OK " +
                        "to review the required permission. Please select \"Allow\" or \"While using the app\" or \"Only this time\" to use camera.")
                .setPositiveButton(
                        android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                permissionsHandler.requestPermissions(deniedPermissions, Aware_Client.this);
                            }
                        })
                .show();
    }

    private class SettingsSync extends AsyncTask<Preference, Preference, Void> {
        @Override
        protected Void doInBackground(Preference... params) {
            for (Preference pref : params) {
                publishProgress(pref);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Preference... values) {
            super.onProgressUpdate(values);

            Preference pref = values[0];

            if (CheckBoxPreference.class.isInstance(pref)) {
                CheckBoxPreference check = (CheckBoxPreference) findPreference(pref.getKey());
                check.setChecked(Aware.getSetting(getApplicationContext(), pref.getKey()).equals("true"));
                if (check.isChecked()) {
                    if (pref.getKey().equalsIgnoreCase(Aware_Preferences.AWARE_DONATE_USAGE)) {
                        Toast.makeText(getApplicationContext(), "Thanks!", Toast.LENGTH_SHORT).show();
                        new AsyncPing().execute();
                    }
                    if (pref.getKey().equalsIgnoreCase(Aware_Preferences.STATUS_WEBSERVICE)) {
                        if (Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER).length() == 0) {
                            Toast.makeText(getApplicationContext(), "Study URL missing...", Toast.LENGTH_SHORT).show();
                        } else if (!Aware.isStudy(getApplicationContext())) {
                            //Shows UI to allow the user to join study
                            Intent joinStudy = new Intent(getApplicationContext(), AwareJoinStudy.class);
                            joinStudy.putExtra(AwareJoinStudy.EXTRA_STUDY_URL, Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER));
                            startActivity(joinStudy);
                        }
                    }
                    if (pref.getKey().equalsIgnoreCase(Aware_Preferences.FOREGROUND_PRIORITY)) {
                        sendBroadcast(new Intent(Aware.ACTION_AWARE_PRIORITY_FOREGROUND));
                    }
                } else {
                    if (pref.getKey().equalsIgnoreCase(Aware_Preferences.FOREGROUND_PRIORITY)) {
                        sendBroadcast(new Intent(Aware.ACTION_AWARE_PRIORITY_BACKGROUND));
                    }
                }
            }

            if (EditTextPreference.class.isInstance(pref)) {
                EditTextPreference text = (EditTextPreference) findPreference(pref.getKey());
                text.setText(Aware.getSetting(getApplicationContext(), pref.getKey()));
                text.setSummary(Aware.getSetting(getApplicationContext(), pref.getKey()));
            }

            if (ListPreference.class.isInstance(pref)) {
                ListPreference list = (ListPreference) findPreference(pref.getKey());
                list.setValue(Aware.getSetting(getApplicationContext(), pref.getKey()));
                list.setSummary(list.getEntry());
            }

            if (PreferenceScreen.class.isInstance(getPreferenceParent(pref))) {
                PreferenceScreen parent = (PreferenceScreen) getPreferenceParent(pref);
                ListAdapter children = parent.getRootAdapter();
                boolean is_active = false;
                for (int i = 0; i < children.getCount(); i++) {
                    Object obj = children.getItem(i);
                    if (CheckBoxPreference.class.isInstance(obj)) {
                        CheckBoxPreference child = (CheckBoxPreference) obj;
                        if (child.getKey().contains("status_")) {
                            if (child.isChecked()) {
                                is_active = true;
                                break;
                            }
                        }
                    }
                }
                if (is_active) {
                    try {
                        Class res = R.drawable.class;
                        Field field = res.getField("ic_action_" + parent.getKey());
                        int icon_id = field.getInt(null);
                        Drawable category_icon = ContextCompat.getDrawable(getApplicationContext(), icon_id);
                        if (category_icon != null) {
                            category_icon.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.accent), PorterDuff.Mode.SRC_IN));
                            parent.setIcon(category_icon);
                            onContentChanged();
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                    }
                } else {
                    try {
                        Class res = R.drawable.class;
                        Field field = res.getField("ic_action_" + parent.getKey());
                        int icon_id = field.getInt(null);
                        Drawable category_icon = ContextCompat.getDrawable(getApplicationContext(), icon_id);
                        if (category_icon != null) {
                            category_icon.clearColorFilter();
                            parent.setIcon(category_icon);
                            onContentChanged();
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                    }
                }
            }
        }
    }




    @Override
    protected void onResume() {
        super.onResume();

            Set<String> keys = optionalSensors.keySet();
            for (String optionalSensor : keys) {
                Preference pref = findPreference(optionalSensor);
                PreferenceGroup parent = getPreferenceParent(pref);
                if (pref.getKey().equalsIgnoreCase(optionalSensor) && !listSensorType.containsKey(optionalSensors.get(optionalSensor)))
                    parent.setEnabled(false);
            }

            prefs.registerOnSharedPreferenceChangeListener(this);

            new SettingsSync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, //use all cores available to process UI faster
                    findPreference(Aware_Preferences.DEVICE_ID),
                    findPreference(Aware_Preferences.DEVICE_LABEL),
                    findPreference(Aware_Preferences.AWARE_VERSION),
                    findPreference(Aware_Preferences.STATUS_ACCELEROMETER),
                    findPreference(Aware_Preferences.FREQUENCY_ACCELEROMETER),
                    findPreference(Aware_Preferences.FREQUENCY_ACCELEROMETER_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_ACCELEROMETER),
                    findPreference(Aware_Preferences.STATUS_APPLICATIONS),
                    findPreference(Aware_Preferences.FREQUENCY_APPLICATIONS),
                    findPreference(Aware_Preferences.STATUS_BAROMETER),
                    findPreference(Aware_Preferences.FREQUENCY_BAROMETER),
                    findPreference(Aware_Preferences.FREQUENCY_BAROMETER_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_BAROMETER),
                    findPreference(Aware_Preferences.STATUS_BATTERY),
                    findPreference(Aware_Preferences.STATUS_BLUETOOTH),
                    findPreference(Aware_Preferences.FREQUENCY_BLUETOOTH),
                    findPreference(Aware_Preferences.STATUS_CALLS),
                    findPreference(Aware_Preferences.STATUS_COMMUNICATION_EVENTS),
                    findPreference(Aware_Preferences.STATUS_CRASHES),
                    findPreference(Aware_Preferences.STATUS_ESM),
                    findPreference(Aware_Preferences.STATUS_GRAVITY),
                    findPreference(Aware_Preferences.FREQUENCY_GRAVITY),
                    findPreference(Aware_Preferences.FREQUENCY_GRAVITY_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_GRAVITY),
                    findPreference(Aware_Preferences.STATUS_GYROSCOPE),
                    findPreference(Aware_Preferences.FREQUENCY_GYROSCOPE),
                    findPreference(Aware_Preferences.FREQUENCY_GYROSCOPE_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_GYROSCOPE),
                    findPreference(Aware_Preferences.STATUS_INSTALLATIONS),
                    findPreference(Aware_Preferences.STATUS_KEYBOARD),
                    findPreference(Aware_Preferences.MASK_KEYBOARD),
                    findPreference(Aware_Preferences.STATUS_LIGHT),
                    findPreference(Aware_Preferences.FREQUENCY_LIGHT),
                    findPreference(Aware_Preferences.FREQUENCY_LIGHT_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_LIGHT),
                    findPreference(Aware_Preferences.STATUS_LINEAR_ACCELEROMETER),
                    findPreference(Aware_Preferences.FREQUENCY_LINEAR_ACCELEROMETER),
                    findPreference(Aware_Preferences.FREQUENCY_LINEAR_ACCELEROMETER_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_LINEAR_ACCELEROMETER),
                    findPreference(Aware_Preferences.STATUS_LOCATION_GPS),
                    findPreference(Aware_Preferences.FREQUENCY_LOCATION_GPS),
                    findPreference(Aware_Preferences.MIN_LOCATION_GPS_ACCURACY),
                    findPreference(Aware_Preferences.STATUS_LOCATION_NETWORK),
                    findPreference(Aware_Preferences.FREQUENCY_LOCATION_NETWORK),
                    findPreference(Aware_Preferences.MIN_LOCATION_NETWORK_ACCURACY),
                    findPreference(Aware_Preferences.LOCATION_EXPIRATION_TIME),
                    findPreference(Aware_Preferences.LOCATION_GEOFENCE),
                    findPreference(Aware_Preferences.LOCATION_SAVE_ALL),
                    findPreference(Aware_Preferences.STATUS_LOCATION_PASSIVE),
                    findPreference(Aware_Preferences.STATUS_MAGNETOMETER),
                    findPreference(Aware_Preferences.FREQUENCY_MAGNETOMETER),
                    findPreference(Aware_Preferences.FREQUENCY_MAGNETOMETER_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_MAGNETOMETER),
                    findPreference(Aware_Preferences.STATUS_MESSAGES),
                    findPreference(Aware_Preferences.STATUS_MQTT),
                    findPreference(Aware_Preferences.STATUS_NETWORK_EVENTS),
                    findPreference(Aware_Preferences.STATUS_NETWORK_TRAFFIC),
                    findPreference(Aware_Preferences.FREQUENCY_NETWORK_TRAFFIC),
                    findPreference(Aware_Preferences.STATUS_NOTIFICATIONS),
                    findPreference(Aware_Preferences.STATUS_PROCESSOR),
                    findPreference(Aware_Preferences.FREQUENCY_PROCESSOR),
                    findPreference(Aware_Preferences.STATUS_PROXIMITY),
                    findPreference(Aware_Preferences.FREQUENCY_PROXIMITY),
                    findPreference(Aware_Preferences.FREQUENCY_PROXIMITY_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_PROXIMITY),
                    findPreference(Aware_Preferences.STATUS_ROTATION),
                    findPreference(Aware_Preferences.FREQUENCY_ROTATION),
                    findPreference(Aware_Preferences.FREQUENCY_ROTATION_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_ROTATION),
                    findPreference(Aware_Preferences.STATUS_SCREEN),
                    findPreference(Aware_Preferences.STATUS_SIGNIFICANT_MOTION),
                    findPreference(Aware_Preferences.STATUS_TEMPERATURE),
                    findPreference(Aware_Preferences.FREQUENCY_TEMPERATURE),
                    findPreference(Aware_Preferences.FREQUENCY_TEMPERATURE_ENFORCE),
                    findPreference(Aware_Preferences.THRESHOLD_TEMPERATURE),
                    findPreference(Aware_Preferences.STATUS_TELEPHONY),
                    findPreference(Aware_Preferences.STATUS_TIMEZONE),
                    findPreference(Aware_Preferences.FREQUENCY_TIMEZONE),
                    findPreference(Aware_Preferences.STATUS_WIFI),
                    findPreference(Aware_Preferences.FREQUENCY_WIFI),
                    findPreference(Aware_Preferences.STATUS_WEBSERVICE),
                    findPreference(Aware_Preferences.MQTT_SERVER),
                    findPreference(Aware_Preferences.MQTT_PORT),
                    findPreference(Aware_Preferences.MQTT_USERNAME),
                    findPreference(Aware_Preferences.MQTT_PASSWORD),
                    findPreference(Aware_Preferences.MQTT_KEEP_ALIVE),
                    findPreference(Aware_Preferences.MQTT_QOS),
                    findPreference(Aware_Preferences.WEBSERVICE_SERVER),
                    findPreference(Aware_Preferences.FREQUENCY_WEBSERVICE),
                    findPreference(Aware_Preferences.FREQUENCY_CLEAN_OLD_DATA),
                    findPreference(Aware_Preferences.WEBSERVICE_CHARGING),
                    findPreference(Aware_Preferences.WEBSERVICE_SILENT),
                    findPreference(Aware_Preferences.WEBSERVICE_WIFI_ONLY),
                    findPreference(Aware_Preferences.WEBSERVICE_FALLBACK_NETWORK),
                    findPreference(Aware_Preferences.REMIND_TO_CHARGE),
                    findPreference(Aware_Preferences.WEBSERVICE_SIMPLE),
                    findPreference(Aware_Preferences.WEBSERVICE_REMOVE_DATA),
                    findPreference(Aware_Preferences.DEBUG_DB_SLOW),
                    findPreference(Aware_Preferences.FOREGROUND_PRIORITY),
                    findPreference(Aware_Preferences.STATUS_TOUCH),
                    findPreference(Aware_Preferences.MASK_TOUCH_TEXT),
                    findPreference(Aware_Preferences.STATUS_WEBSOCKET),
                    findPreference(Aware_Preferences.WEBSOCKET_SERVER),
                    findPreference(Aware_Preferences.DEBUG_FLAG),
                    findPreference(Aware_Preferences.DEBUG_TAG),
                    findPreference(Aware_Preferences.ENFORCE_FREQUENCY_ALL)
            );


            if (Aware.isStudy(this)) {
                if (Aware.getSetting(this, Aware_Preferences.INTERFACE_LOCKED).equals("true") ||
                        Aware.getSetting(this, "ui_mode").equals("1") || Aware.getSetting(this, "ui_mode").equals("2")
                ) {
                    finish();
                    startActivity(new Intent(this, AwareParticipant.class));
                }
            }


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(intent != null && intent.getExtras() != null && intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS) != null) {
            redirectActivityComponent = intent.getStringExtra(PermissionsHandler.EXTRA_REDIRECT_ACTIVITY);
            permissionsHandler = new PermissionsHandler(this);
            ArrayList<String> permission = (ArrayList<String>) intent.getSerializableExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS);
            permissionsHandler.requestPermissions(permission, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(packageMonitor);
    }

    private class AsyncPing extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            // Download the certificate, and block since we are already running in background
            // and we need the certificate immediately.
            SSLManager.handleUrl(getApplicationContext(), "https://api.awareframework.com/index.php", true);

            //Ping AWARE's server with getApplicationContext() device's information for framework's statistics log
            Hashtable<String, String> device_ping = new Hashtable<>();
            device_ping.put(Aware_Preferences.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            device_ping.put("ping", String.valueOf(System.currentTimeMillis()));
            device_ping.put("platform", "android");
            try {
                PackageInfo package_info = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
                device_ping.put("package_name", package_info.packageName);
                if (package_info.packageName.equals("com.aware.phone")) {
                    device_ping.put("package_version_code", String.valueOf(package_info.versionCode));
                    device_ping.put("package_version_name", String.valueOf(package_info.versionName));
                }
            } catch (PackageManager.NameNotFoundException e) {
            }

            try {
                new Https(SSLManager.getHTTPS(getApplicationContext(), "https://api.awareframework.com/index.php")).dataPOST("https://api.awareframework.com/index.php/awaredev/alive", device_ping, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        }
    }
}