package com.aware.plugin.sentimental;

import android.content.ContentProvider;
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

    public static final int DATABASE_VERSION = 4;

    /**
     * Provider authority: com.aware.plugin.sentimental.provider.sentimental
     */
    public static String AUTHORITY = "com.aware.plugin.sentimental.provider.sentimental";

    private static final int SENTIMENTAL = 1;
    private static final int SENTIMENTAL_ID = 2;

    public static final String DATABASE_NAME = "plugin_sentimental.db";

    public static final String[] DATABASE_TABLES = {
            "plugin_sentimental"
    };

    public static final String[] TABLES_FIELDS = {
            Sentimental_Data._ID + " integer primary key autoincrement," +
                    Sentimental_Data.TIMESTAMP + " real default 0," +
                    Sentimental_Data.DEVICE_ID + " text default ''," +
                    Sentimental_Data.APP_NAME + " text default ''," +
                    Sentimental_Data.WORD_CATEGORY + " text default ''," +
                    Sentimental_Data.SENTIMENT_SCORE + " real default 0"
    };

    public static final class Sentimental_Data implements BaseColumns {
        private Sentimental_Data() {
        }

        ;

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plugin_sentimental");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.sentimental";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.sentimental";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String APP_NAME = "app_name";
        public static final String WORD_CATEGORY = "word_category";
        public static final String SENTIMENT_SCORE = "double_sentiment_score";
    }

    private static UriMatcher URIMatcher;
    private static HashMap<String, String> databaseMap;

    /**
     * Returns the provider authority that is dynamic
     *
     * @return
     */
    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".provider.sentimental";
        return AUTHORITY;
    }

    @Override
    public boolean onCreate() {
        AUTHORITY = getContext().getPackageName() + ".provider.sentimental";

        URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], SENTIMENTAL);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", SENTIMENTAL_ID);

        databaseMap = new HashMap<>();
        databaseMap.put(Sentimental_Data._ID, Sentimental_Data._ID);
        databaseMap.put(Sentimental_Data.TIMESTAMP, Sentimental_Data.TIMESTAMP);
        databaseMap.put(Sentimental_Data.DEVICE_ID, Sentimental_Data.DEVICE_ID);
        databaseMap.put(Sentimental_Data.APP_NAME, Sentimental_Data.APP_NAME);
        databaseMap.put(Sentimental_Data.WORD_CATEGORY, Sentimental_Data.WORD_CATEGORY);
        databaseMap.put(Sentimental_Data.SENTIMENT_SCORE, Sentimental_Data.SENTIMENT_SCORE);

        return true;
    }

    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;

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
        switch (URIMatcher.match(uri)) {
            case SENTIMENTAL:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
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
        switch (URIMatcher.match(uri)) {
            case SENTIMENTAL:
                return Sentimental_Data.CONTENT_TYPE;
            case SENTIMENTAL_ID:
                return Sentimental_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues initialValues) {
        initialiseDatabase();

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        database.beginTransaction();

        switch (URIMatcher.match(uri)) {
            case SENTIMENTAL:
                long weather_id = database.insertWithOnConflict(DATABASE_TABLES[0], Sentimental_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);

                if (weather_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            Sentimental_Data.CONTENT_URI,
                            weather_id);
                    getContext().getContentResolver().notifyChange(new_uri, null, false);
                    database.setTransactionSuccessful();
                    database.endTransaction();
                    return new_uri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URIMatcher.match(uri)) {
            case SENTIMENTAL:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
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
    public synchronized int update(Uri uri, ContentValues values, String selection,
                                   String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (URIMatcher.match(uri)) {
            case SENTIMENTAL:
                count = database.update(DATABASE_TABLES[0], values, selection,
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
