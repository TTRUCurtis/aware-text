package com.aware.plugin.google.fused_location;

import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import androidx.annotation.Nullable;
import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

/**
 * Created by denzil on 08/06/16.
 */
public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.google.fused_location.provider.geofences";
    public static final int DATABASE_VERSION = 3;

    public static final String DATABASE_NAME = "fused_geofences.db";

    public static final String DB_TBL_GEOFENCES = "fused_geofences";
    public static final String DB_TBL_GEOFENCES_DATA = "fused_geofences_data";

    public static final String[] DATABASE_TABLES = {
            DB_TBL_GEOFENCES,
            DB_TBL_GEOFENCES_DATA
    };

    public static final class Geofences implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_GEOFENCES);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.google.fused_location.geofences";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.google.fused_location.geofences";

        public static final String GEO_LABEL = "geofence_label";
        public static final String GEO_LAT = "double_latitude";
        public static final String GEO_LONG = "double_longitude";
        public static final String GEO_RADIUS = "double_radius";
    }

    public static final class Geofences_Data implements AWAREColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + DB_TBL_GEOFENCES_DATA);
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.google.fused_location.geofences.data";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.google.fused_location.geofences.data";

        public static final String GEO_LABEL = "geofence_label";
        public static final String GEO_LAT = "double_latitude";
        public static final String GEO_LONG = "double_longitude";
        public static final String DISTANCE = "double_distance";
        public static final String STATUS = "status";
    }

    private static final String DB_TBL_GEOFENCES_FIELDS =
            Geofences._ID + " integer primary key autoincrement," +
                    Geofences.TIMESTAMP + " real default 0," +
                    Geofences.DEVICE_ID + " text default ''," +
                    Geofences.GEO_LABEL + " text default ''," +
                    Geofences.GEO_LAT + " real default null," +
                    Geofences.GEO_LONG + " real default null," +
                    Geofences.GEO_RADIUS + " real default null";

    private static final String DB_TBL_GEOFENCES_DATA_FIELDS =
            Geofences_Data._ID + " integer primary key autoincrement," +
                    Geofences_Data.TIMESTAMP + " real default 0," +
                    Geofences_Data.DEVICE_ID + " text default ''," +
                    Geofences_Data.GEO_LABEL + " text default ''," +
                    Geofences_Data.GEO_LAT + " real default null," +
                    Geofences_Data.GEO_LONG + " real default null," +
                    Geofences_Data.DISTANCE + " real default null," +
                    Geofences_Data.STATUS + " integer default null"
            ;

    public static final String[] TABLES_FIELDS = {
            DB_TBL_GEOFENCES_FIELDS,
            DB_TBL_GEOFENCES_DATA_FIELDS
    };

    public interface AWAREColumns extends BaseColumns {
        String _ID = "_id";
        String TIMESTAMP = "timestamp";
        String DEVICE_ID = "device_id";
    }

    private static UriMatcher sUriMatcher;
    private static HashMap<String, String> geoHash, geoDataHash;

    private static final int GEO_DIR = 1;
    private static final int GEO_ITEM = 2;
    private static final int GEO_DATA_DIR = 3;
    private static final int GEO_DATA_ITEM = 4;

    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;

    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }

    /**
     * Returns the provider authority that is dynamic
     * @return
     */
    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".provider.geofences";
        return AUTHORITY;
    }

    @Override
    public boolean onCreate() {

        AUTHORITY = getContext().getPackageName() + ".provider.geofences";

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], GEO_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", GEO_ITEM);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], GEO_DATA_DIR);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", GEO_DATA_ITEM);

        geoHash = new HashMap<>();
        geoHash.put(Geofences._ID, Geofences._ID);
        geoHash.put(Geofences.TIMESTAMP, Geofences.TIMESTAMP);
        geoHash.put(Geofences.DEVICE_ID, Geofences.DEVICE_ID);
        geoHash.put(Geofences.GEO_LABEL, Geofences.GEO_LABEL);
        geoHash.put(Geofences.GEO_LAT, Geofences.GEO_LAT);
        geoHash.put(Geofences.GEO_LONG, Geofences.GEO_LONG);
        geoHash.put(Geofences.GEO_RADIUS, Geofences.GEO_RADIUS);

        geoDataHash = new HashMap<>();
        geoDataHash.put(Geofences_Data._ID, Geofences_Data._ID);
        geoDataHash.put(Geofences_Data.TIMESTAMP, Geofences_Data.TIMESTAMP);
        geoDataHash.put(Geofences_Data.DEVICE_ID, Geofences_Data.DEVICE_ID);
        geoDataHash.put(Geofences_Data.GEO_LABEL, Geofences_Data.GEO_LABEL);
        geoDataHash.put(Geofences_Data.GEO_LAT, Geofences_Data.GEO_LAT);
        geoDataHash.put(Geofences_Data.GEO_LONG, Geofences_Data.GEO_LONG);
        geoDataHash.put(Geofences_Data.DISTANCE, Geofences_Data.DISTANCE);
        geoDataHash.put(Geofences_Data.STATUS, Geofences_Data.STATUS);

        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case GEO_DIR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(geoHash);
                break;
            case GEO_DATA_DIR:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(geoDataHash);
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

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case GEO_DIR:
                return Geofences.CONTENT_TYPE;
            case GEO_ITEM:
                return Geofences.CONTENT_ITEM_TYPE;
            case GEO_DATA_DIR:
                return Geofences_Data.CONTENT_TYPE;
            case GEO_DATA_ITEM:
                return Geofences_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public synchronized Uri insert(Uri uri, ContentValues new_values) {
        initialiseDatabase();

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();

        database.beginTransaction();


        switch (sUriMatcher.match(uri)) {
            case GEO_DIR:
                long geo_id = database.insertWithOnConflict(DATABASE_TABLES[0], Geofences.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (geo_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Geofences.CONTENT_URI, geo_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    database.setTransactionSuccessful();
                    database.endTransaction();
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case GEO_DATA_DIR:
                long geo_data_id = database.insertWithOnConflict(DATABASE_TABLES[1], Geofences_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (geo_data_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(Geofences_Data.CONTENT_URI, geo_data_id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    database.setTransactionSuccessful();
                    database.endTransaction();
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
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {
            case GEO_DIR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            case GEO_DATA_DIR:
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
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {
            case GEO_DIR:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;
            case GEO_DATA_DIR:
                count = database.update(DATABASE_TABLES[1], values, selection, selectionArgs);
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
