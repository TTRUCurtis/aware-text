/**
 * @author: denzil
 */
// DONE REMOVE REDUNDANT CODE FOR THE DEVICE PICKER BUTTON THAT DOESN'T FUNCTION
package com.aware.plugin.sensortag;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    /**
     * Authority of this content provider
     */
    public static String AUTHORITY = "com.aware.plugin.sensortag.provider.sensortag";

    /**
     * ContentProvider database version. Increment every time you modify the database structure
     */
    public static final int DATABASE_VERSION = 6;

    /**
     * Shared in all database tables
     */
    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    public static final class SensorTag_Data implements AWAREColumns {
        private SensorTag_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sensortag_data");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.aware.plugin.sensortag.data";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.aware.plugin.sensortag.data";

        public static final String SENSOR_TAG_SENSOR = "sensortag_sensor";
        public static final String SENSOR_TAG_DATA = "sensortag_data";
    }

    public static final class SensorTag_Devices implements AWAREColumns {
        private SensorTag_Devices() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sensortag_devices");
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.aware.plugin.sensortag.devices";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.aware.plugin.sensortag.devices";

        public static final String SENSOR_TAG_DEVICE = "sensortag_device";
        public static final String SENSOR_TAG_INFO = "sensortag_info";
    }

    //ContentProvider query indexes
    private static final int SENSORTAG_DATA = 1;
    private static final int SENSORTAG_DATA_ID = 2;
    private static final int SENSORTAG_DEVICES = 3;
    private static final int SENSORTAG_DEVICES_ID = 4;

    /**
     * Database stored in external folder: /AWARE/plugin_sensortag.db
     */
    public static final String DATABASE_NAME = "plugin_sensortag.db";

    /**
     * Database tables:<br/>
     * - plugin_phone_usage
     */
    public static final String[] DATABASE_TABLES = {"sensortag_data", "sensortag_devices"};

    private static final String DB_TBL_SENSOR_TAG =
            SensorTag_Data._ID + " integer primary key autoincrement," +
                    SensorTag_Data.TIMESTAMP + " real default 0," +
                    SensorTag_Data.DEVICE_ID + " text default ''," +
                    SensorTag_Data.SENSOR_TAG_SENSOR + " text default ''," +
                    SensorTag_Data.SENSOR_TAG_DATA + " text default ''";

    private static final String DB_TBL_SENSOR_TAG_DEVICES =
            SensorTag_Devices._ID + " integer primary key autoincrement," +
                    SensorTag_Devices.TIMESTAMP + " real default 0," +
                    SensorTag_Devices.DEVICE_ID + " text default ''," +
                    SensorTag_Devices.SENSOR_TAG_DEVICE + " text default '',"+
                    SensorTag_Devices.SENSOR_TAG_INFO + " text default ''";

    /**
     * Database table fields
     */
    public static final String[] TABLES_FIELDS = {
            DB_TBL_SENSOR_TAG,
            DB_TBL_SENSOR_TAG_DEVICES
    };

    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> sensorDataHash = null;
    private static HashMap<String, String> sensorDevicesHash = null;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;

    /**
     * Returns the provider authority that is dynamic
     *
     * @return
     */
    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".provider.sensortag";
        return AUTHORITY;
    }

    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }

    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {
            case SENSORTAG_DATA:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            case SENSORTAG_DEVICES:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case SENSORTAG_DATA:
                return SensorTag_Data.CONTENT_TYPE;
            case SENSORTAG_DATA_ID:
                return SensorTag_Data.CONTENT_ITEM_TYPE;
            case SENSORTAG_DEVICES:
                return SensorTag_Devices.CONTENT_TYPE;
            case SENSORTAG_DEVICES_ID:
                return SensorTag_Devices.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues new_values) {
        initialiseDatabase();

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {
            case SENSORTAG_DATA:
                long _id = database.insertWithOnConflict(DATABASE_TABLES[0],
                        SensorTag_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(
                            SensorTag_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case SENSORTAG_DEVICES:
                long sensor_id = database.insertWithOnConflict(DATABASE_TABLES[1],
                        SensorTag_Devices.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (sensor_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(
                            SensorTag_Devices.CONTENT_URI, sensor_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider.sensortag";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], SENSORTAG_DATA); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", SENSORTAG_DATA_ID); //URI for a single record

        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], SENSORTAG_DEVICES); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", SENSORTAG_DEVICES_ID); //URI for a single record

        sensorDataHash = new HashMap<>();
        sensorDataHash.put(SensorTag_Data._ID, SensorTag_Data._ID);
        sensorDataHash.put(SensorTag_Data.TIMESTAMP, SensorTag_Data.TIMESTAMP);
        sensorDataHash.put(SensorTag_Data.DEVICE_ID, SensorTag_Data.DEVICE_ID);
        sensorDataHash.put(SensorTag_Data.SENSOR_TAG_SENSOR, SensorTag_Data.SENSOR_TAG_SENSOR);
        sensorDataHash.put(SensorTag_Data.SENSOR_TAG_DATA, SensorTag_Data.SENSOR_TAG_DATA);

        sensorDevicesHash = new HashMap<>();
        sensorDevicesHash.put(SensorTag_Devices._ID, SensorTag_Devices._ID);
        sensorDevicesHash.put(SensorTag_Devices.TIMESTAMP, SensorTag_Devices.TIMESTAMP);
        sensorDevicesHash.put(SensorTag_Devices.DEVICE_ID, SensorTag_Devices.DEVICE_ID);
        sensorDevicesHash.put(SensorTag_Devices.SENSOR_TAG_DEVICE, SensorTag_Devices.SENSOR_TAG_DEVICE);
        sensorDevicesHash.put(SensorTag_Devices.SENSOR_TAG_INFO, SensorTag_Devices.SENSOR_TAG_INFO);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case SENSORTAG_DATA:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(sensorDataHash);
                break;
            case SENSORTAG_DEVICES:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(sensorDevicesHash);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {
            case SENSORTAG_DATA:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            case SENSORTAG_DEVICES:
                count = database.update(DATABASE_TABLES[1], values, selection,
                        selectionArgs);
                break;
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }
}
