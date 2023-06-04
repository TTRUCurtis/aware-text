/**
 * @author: denzil
 */
package com.aware.plugin.sensortag;

import android.Manifest;
import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.database.Cursor;
import android.os.Bundle;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

import org.json.JSONException;
import org.json.JSONObject;

public class Plugin extends Aware_Plugin {

    private static String DEVICE_ID;

    public static final String ACTION_RECORD_SENSORTAG = "ACTION_RECORD_SENSORTAG";

    @Override
    public void onCreate() {
        super.onCreate();

        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE::Sensor Tag";

        //REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        //REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH);
        //REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);

        contextBroadcaster.setTag(TAG);
        contextBroadcaster.setProvider(AUTHORITY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(ACTION_RECORD_SENSORTAG)) {
            String data = intent.getStringExtra("data");
            String sensor = intent.getStringExtra("sensor");
            try {
                JSONObject dataJson = new JSONObject(data);
                saveSmartTagData(getApplicationContext(), sensor, dataJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_SENSORTAG).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_SENSORTAG, true);
            } else {
                if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_SENSORTAG).equalsIgnoreCase("false")) {
                    Aware.stopPlugin(getApplicationContext(), getPackageName());
                    return START_STICKY;
                }
            }

            DEVICE_ID = Aware.getSetting(this, Aware_Preferences.DEVICE_ID);

            if (Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_SENSORTAG).length() == 0)
                Aware.setSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_SENSORTAG, "30");

            if (Aware.isStudy(this)) {
                Account aware_account = Aware.getAWAREAccount(getApplicationContext());
                String authority = Provider.getAuthority(getApplicationContext());
                long frequency = Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;

                ContentResolver.setIsSyncable(aware_account, authority, 1);
                ContentResolver.setSyncAutomatically(aware_account, authority, true);
                SyncRequest request = new SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(aware_account, authority)
                        .setExtras(new Bundle()).build();
                ContentResolver.requestSync(request);
            }

            pairSmartTags();
        }
        return START_STICKY;
    }

    private void pairSmartTags() {
        Cursor paired = getContentResolver().query(Provider.SensorTag_Devices.CONTENT_URI, null, null, null, null);
        if (paired == null || paired.getCount() == 0) {
            Intent devicePicker = new Intent(getApplicationContext(), DevicePicker.class);
            devicePicker.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(devicePicker);
        }
        if (paired != null) paired.close();
    }

    public static void removeSmartTag(Context context, String sensor_mac) {
        context.getContentResolver().delete(Provider.SensorTag_Devices.CONTENT_URI, Provider.SensorTag_Devices.SENSOR_TAG_DEVICE + " like '" + sensor_mac + "'", null);
    }

    public static void saveSmartTag(Context context, String sensor_mac, JSONObject smartag) {
        Cursor smartTags = context.getContentResolver().query(Provider.SensorTag_Devices.CONTENT_URI, null, Provider.SensorTag_Devices.SENSOR_TAG_DEVICE + " like '" + sensor_mac + "'", null, null);
        if (smartTags == null || smartTags.getCount() == 0) {
            ContentValues context_data = new ContentValues();
            context_data.put(Provider.SensorTag_Devices.TIMESTAMP, System.currentTimeMillis());
            context_data.put(Provider.SensorTag_Devices.DEVICE_ID, DEVICE_ID);
            context_data.put(Provider.SensorTag_Devices.SENSOR_TAG_DEVICE, sensor_mac);
            context_data.put(Provider.SensorTag_Devices.SENSOR_TAG_INFO, smartag.toString());

            context.getContentResolver().insert(Provider.SensorTag_Devices.CONTENT_URI, context_data);

            if (awareSensor != null) awareSensor.onSmartTagChanged(context_data);
        }
        if (smartTags != null) smartTags.close();
    }

    public static void saveSmartTagData(Context context, String sensor, JSONObject data) {
        ContentValues context_data = new ContentValues();
        context_data.put(Provider.SensorTag_Data.TIMESTAMP, System.currentTimeMillis());
        context_data.put(Provider.SensorTag_Data.DEVICE_ID, DEVICE_ID);
        context_data.put(Provider.SensorTag_Data.SENSOR_TAG_SENSOR, sensor);
        context_data.put(Provider.SensorTag_Data.SENSOR_TAG_DATA, data.toString());

        context.getContentResolver().insert(Provider.SensorTag_Data.CONTENT_URI, context_data);

        if (awareSensor != null) {
            if (sensor.equalsIgnoreCase("accelerometer"))
                awareSensor.onAccelerometerChanged(context_data);
            if (sensor.equalsIgnoreCase("gyroscope"))
                awareSensor.onGyroscopeChanged(context_data);
            if (sensor.equalsIgnoreCase("humidity"))
                awareSensor.onHumidityChanged(context_data);
            if (sensor.equalsIgnoreCase("ambient_temperature"))
                awareSensor.onAmbientTemperatureChanged(context_data);
            if (sensor.equalsIgnoreCase("target_temperature"))
                awareSensor.onTargetTemperatureChanged(context_data);
            if (sensor.equalsIgnoreCase("light"))
                awareSensor.onLightChanged(context_data);
            if (sensor.equalsIgnoreCase("barometer"))
                awareSensor.onBarometerChanged(context_data);
        }
    }

    /**
     * Supported callbacks for this plugin
     */
    private static AWARESensorObserver awareSensor;

    /**
     * Assign observer from application
     *
     * @param observer
     */
    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }

    /**
     * Return assigned observer
     *
     * @return
     */
    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }

    /**
     * Interface to interact with observers' callbacks
     */
    public interface AWARESensorObserver {
        void onAccelerometerChanged(ContentValues data);

        void onGyroscopeChanged(ContentValues data);

        void onHumidityChanged(ContentValues data);

        void onAmbientTemperatureChanged(ContentValues data);

        void onTargetTemperatureChanged(ContentValues data);

        void onLightChanged(ContentValues data);

        void onBarometerChanged(ContentValues data);

        /**
         * SmartTag device paired
         *
         * @param data
         */
        void onSmartTagChanged(ContentValues data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Provider.getAuthority(this),
                Bundle.EMPTY
        );

        Aware.setSetting(this, Settings.STATUS_PLUGIN_SENSORTAG, false);
    }
}
