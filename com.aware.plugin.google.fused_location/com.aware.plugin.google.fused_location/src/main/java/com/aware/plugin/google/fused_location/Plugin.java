
package com.aware.plugin.google.fused_location;

import android.Manifest;
import android.accounts.Account;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SyncRequest;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Locations_Provider;
import com.aware.providers.Locations_Provider.Locations_Data;
import com.aware.utils.Aware_Plugin;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Fused location service for Aware framework
 * Requires Google Services API available on the device.
 *
 * @author denzil
 */
public class Plugin extends Aware_Plugin implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Broadcasted event: new location available
     */
    public static final String ACTION_AWARE_LOCATIONS = "ACTION_AWARE_LOCATIONS";
    public static final String EXTRA_DATA = "data";

    private static GoogleApiClient mLocationClient;
    private final static LocationRequest mLocationRequest = new LocationRequest();
    private static PendingIntent pIntent;

    public static ContextProducer contextProducer;

    private static Location lastGeofence;

    private Intent geofences;

    @Override
    public void onCreate() {
        super.onCreate();

        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE::Google Fused Location";

        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                Location currentLocation = new Location("Current location");
                Cursor data = getContentResolver().query(Locations_Data.CONTENT_URI, null, null, null, Locations_Data.TIMESTAMP + " DESC LIMIT 1");
                if (data != null && data.moveToFirst()) {
                    currentLocation.setLatitude(data.getDouble(data.getColumnIndex(Locations_Data.LATITUDE)));
                    currentLocation.setLongitude(data.getDouble(data.getColumnIndex(Locations_Data.LONGITUDE)));
                    currentLocation.setAccuracy(data.getFloat(data.getColumnIndex(Locations_Data.ACCURACY)));
                }
                if (data != null && !data.isClosed()) data.close();

                Intent context = new Intent(ACTION_AWARE_LOCATIONS);
                context.putExtra(Plugin.EXTRA_DATA, currentLocation);
                sendBroadcast(context);

                checkGeofences();
            }
        };
        contextProducer = CONTEXT_PRODUCER;

        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (!is_google_services_available()) {
            if (DEBUG)
                Log.e(TAG, "Google Services fused location is not available on this device.");
        } else {
            mLocationClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApiIfAvailable(LocationServices.API)
                    .build();

            Intent locationIntent = new Intent(this, com.aware.plugin.google.fused_location.Algorithm.class);
            pIntent = PendingIntent.getService(this, 0, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            geofences = new Intent(this, GeofencesTracker.class);
            startService(geofences);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            if (Aware.getSetting(getApplicationContext(), Settings.STATUS_GOOGLE_FUSED_LOCATION).length() == 0) {
                //to create the database so that fused can store fused location's data
                Aware.setSetting(this, Aware_Preferences.STATUS_LOCATION_NETWORK, true);
                Aware.setSetting(this, Aware_Preferences.FREQUENCY_LOCATION_NETWORK, 300);
                Aware.startLocations(this);

                Aware.setSetting(getApplicationContext(), Settings.STATUS_GOOGLE_FUSED_LOCATION, true);
            } else {
                if (Aware.getSetting(getApplicationContext(), Settings.STATUS_GOOGLE_FUSED_LOCATION).equalsIgnoreCase("false")) {
                    Aware.stopPlugin(getApplicationContext(), getPackageName());
                    return START_STICKY;
                }
            }

            if (Aware.getSetting(this, Settings.FREQUENCY_GOOGLE_FUSED_LOCATION).length() == 0)
                Aware.setSetting(this, Settings.FREQUENCY_GOOGLE_FUSED_LOCATION, 300);

            if (Aware.getSetting(this, Settings.MAX_FREQUENCY_GOOGLE_FUSED_LOCATION).length() == 0)
                Aware.setSetting(this, Settings.MAX_FREQUENCY_GOOGLE_FUSED_LOCATION, 60);

            if (Aware.getSetting(this, Settings.ACCURACY_GOOGLE_FUSED_LOCATION).length() == 0)
                Aware.setSetting(this, Settings.ACCURACY_GOOGLE_FUSED_LOCATION, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            if (Aware.getSetting(this, Settings.FALLBACK_LOCATION_TIMEOUT).length() == 0)
                Aware.setSetting(this, Settings.FALLBACK_LOCATION_TIMEOUT, 20);

            if (Aware.getSetting(this, Settings.LOCATION_SENSITIVITY).length() == 0)
                Aware.setSetting(this, Settings.LOCATION_SENSITIVITY, 5);

            if (mLocationClient != null && !mLocationClient.isConnected())
                mLocationClient.connect();

            checkGeofences();

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

                String location_authority = Locations_Provider.getAuthority(this);
                ContentResolver.setIsSyncable(aware_account, location_authority, 1);
                ContentResolver.setSyncAutomatically(aware_account, location_authority, true);
                SyncRequest request_local = new SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(aware_account, location_authority)
                        .setExtras(new Bundle()).build();
                ContentResolver.requestSync(request_local);
            }
        }
        return START_STICKY;
    }

    /**
     * How are we doing regarding the geofences?
     */
    private void checkGeofences() {
        Location currentLocation = new Location("Current location");
        Cursor data = getContentResolver().query(Locations_Data.CONTENT_URI, null, null, null, Locations_Data.TIMESTAMP + " DESC LIMIT 1");
        if (data != null && data.moveToFirst()) {
            currentLocation.setLatitude(data.getDouble(data.getColumnIndex(Locations_Data.LATITUDE)));
            currentLocation.setLongitude(data.getDouble(data.getColumnIndex(Locations_Data.LONGITUDE)));
            currentLocation.setAccuracy(data.getFloat(data.getColumnIndex(Locations_Data.ACCURACY)));
            currentLocation.setTime(data.getLong(data.getColumnIndex(Locations_Data.TIMESTAMP)));
        }
        if (data != null && !data.isClosed()) data.close();

        Cursor geofences = GeofenceUtils.getLabels(this, null); //get list of defined geofences
        if (geofences != null && geofences.moveToFirst()) {
            do {
                Location geofenceLocation = new Location("Geofence location");
                geofenceLocation.setLatitude(geofences.getDouble(geofences.getColumnIndex(Provider.Geofences.GEO_LAT)));
                geofenceLocation.setLongitude(geofences.getDouble(geofences.getColumnIndex(Provider.Geofences.GEO_LONG)));

                //Current location is within this geofence
                if (GeofenceUtils.getDistance(currentLocation, geofenceLocation) <= GeofenceUtils.getLabelLocationRadius(getApplicationContext(),geofences.getString(geofences.getColumnIndex(Provider.Geofences.GEO_LABEL)))) {
                    //First time in this geofence
                    if (lastGeofence == null) {

                        ContentValues entered = new ContentValues();
                        entered.put(Provider.Geofences_Data.TIMESTAMP, System.currentTimeMillis());
                        entered.put(Provider.Geofences_Data.DEVICE_ID, Aware.getSetting(this, Aware_Preferences.DEVICE_ID));
                        entered.put(Provider.Geofences_Data.GEO_LABEL, geofences.getString(geofences.getColumnIndex(Provider.Geofences.GEO_LABEL)));
                        entered.put(Provider.Geofences_Data.GEO_LAT, geofences.getDouble(geofences.getColumnIndex(Provider.Geofences.GEO_LAT)));
                        entered.put(Provider.Geofences_Data.GEO_LONG, geofences.getString(geofences.getColumnIndex(Provider.Geofences.GEO_LONG)));
                        entered.put(Provider.Geofences_Data.DISTANCE, GeofenceUtils.getDistance(currentLocation, geofenceLocation));
                        entered.put(Provider.Geofences_Data.STATUS, GeofencesTracker.STATUS_ENTER);

                        getContentResolver().insert(Provider.Geofences_Data.CONTENT_URI, entered);

                        Intent geofenced = new Intent(GeofencesTracker.ACTION_AWARE_PLUGIN_FUSED_ENTERED_GEOFENCE);
                        geofenced.putExtra(GeofencesTracker.EXTRA_LABEL, geofences.getString(geofences.getColumnIndex(Provider.Geofences.GEO_LABEL)));
                        geofenced.putExtra(GeofencesTracker.EXTRA_LOCATION, geofenceLocation);
                        geofenced.putExtra(GeofencesTracker.EXTRA_RADIUS, geofences.getDouble(geofences.getColumnIndex(Provider.Geofences.GEO_RADIUS)));
                        sendBroadcast(geofenced);

                        if (Aware.DEBUG)
                            Log.d(Aware.TAG, "Geofence enter: \n" + entered.toString());

                        lastGeofence = geofenceLocation;
                        break;
                    } else {
                        Intent geofenced = new Intent(GeofencesTracker.ACTION_AWARE_PLUGIN_FUSED_INSIDE_GEOGENCE);
                        geofenced.putExtra(GeofencesTracker.EXTRA_LABEL, GeofenceUtils.getLabel(this, lastGeofence));
                        geofenced.putExtra(GeofencesTracker.EXTRA_LOCATION, lastGeofence);
                        geofenced.putExtra(GeofencesTracker.EXTRA_RADIUS, GeofenceUtils.getLabelLocationRadius(this, GeofenceUtils.getLabel(this, lastGeofence)));
                        sendBroadcast(geofenced);

                        if (Aware.DEBUG)
                            Log.d(Aware.TAG, "Inside geofence: " + GeofenceUtils.getLabel(this, lastGeofence));
                    }
                }
            } while (geofences.moveToNext());

            if (lastGeofence != null && GeofenceUtils.getDistance(currentLocation, lastGeofence) > GeofenceUtils.getLabelLocationRadius(this, GeofenceUtils.getLabel(this, lastGeofence))) { //exited last geofence
                String label = GeofenceUtils.getLabel(this, lastGeofence);
                double radius = GeofenceUtils.getLabelLocationRadius(this, GeofenceUtils.getLabel(this, lastGeofence));

                ContentValues exited = new ContentValues();
                exited.put(Provider.Geofences_Data.TIMESTAMP, System.currentTimeMillis());
                exited.put(Provider.Geofences_Data.DEVICE_ID, Aware.getSetting(this, Aware_Preferences.DEVICE_ID));
                exited.put(Provider.Geofences_Data.GEO_LABEL, label);
                exited.put(Provider.Geofences_Data.GEO_LAT, lastGeofence.getLatitude());
                exited.put(Provider.Geofences_Data.GEO_LONG, lastGeofence.getLongitude());
                exited.put(Provider.Geofences_Data.DISTANCE, GeofenceUtils.getDistance(currentLocation, lastGeofence));
                exited.put(Provider.Geofences_Data.STATUS, GeofencesTracker.STATUS_EXIT);

                getContentResolver().insert(Provider.Geofences_Data.CONTENT_URI, exited);

                Intent geofenced = new Intent(GeofencesTracker.ACTION_AWARE_PLUGIN_FUSED_EXITED_GEOFENCE);
                geofenced.putExtra(GeofencesTracker.EXTRA_LABEL, label);
                geofenced.putExtra(GeofencesTracker.EXTRA_LOCATION, lastGeofence);
                geofenced.putExtra(GeofencesTracker.EXTRA_RADIUS, radius);
                sendBroadcast(geofenced);

                if (Aware.DEBUG)
                    Log.d(Aware.TAG, "Geofence exit:\n" + exited.toString());

                lastGeofence = null;
            }
        } else {
            lastGeofence = null;
        }
        if (geofences != null && !geofences.isClosed()) geofences.close();
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

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Locations_Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Locations_Provider.getAuthority(this),
                Bundle.EMPTY
        );

        Aware.setSetting(this, Settings.STATUS_GOOGLE_FUSED_LOCATION, false);

        Aware.setSetting(this, Aware_Preferences.STATUS_LOCATION_NETWORK, false);
        Aware.stopLocations(this);

        if (mLocationClient != null && mLocationClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient, pIntent);
            mLocationClient.disconnect();
        }

        if (geofences != null) stopService(geofences);
    }

    private boolean is_google_services_available() {
        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
        int result = googleApi.isGooglePlayServicesAvailable(this);
        return (result == ConnectionResult.SUCCESS);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connection_result) {
        if (DEBUG)
            Log.w(TAG, "Error connecting to Google Fused Location services, will try again in 5 minutes");
    }

    @Override
    public void onConnected(Bundle arg0) {
        if (DEBUG)
            Log.i(TAG, "Connected to Google's Location API");

        mLocationRequest.setPriority(Integer.parseInt(Aware.getSetting(this, Settings.ACCURACY_GOOGLE_FUSED_LOCATION)));
        mLocationRequest.setInterval(Long.parseLong(Aware.getSetting(this, Settings.FREQUENCY_GOOGLE_FUSED_LOCATION)) * 1000);
        mLocationRequest.setFastestInterval(Long.parseLong(Aware.getSetting(this, Settings.MAX_FREQUENCY_GOOGLE_FUSED_LOCATION)) * 1000);
        mLocationRequest.setMaxWaitTime(Long.parseLong(Aware.getSetting(this, Settings.FALLBACK_LOCATION_TIMEOUT)) * 1000); //wait X seconds for GPS
        mLocationRequest.setSmallestDisplacement(Float.parseFloat(Aware.getSetting(this, Settings.LOCATION_SENSITIVITY)));

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, pIntent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (DEBUG)
            Log.w(TAG, "Error connecting to Google Fused Location services, will try again in 5 minutes");
    }
}
