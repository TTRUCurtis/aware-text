package com.aware.plugin.sms;

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
     * Provider authority: com.aware.plugin.sms.provider.sms
     */
    public static String AUTHORITY = "com.aware.plugin.sms.provider.sms";

    private static final int SMS = 1;
    private static final int SMS_ID = 2;
    private static final int SENTIMENT = 3;
    private static final int SENTIMENT_ID = 4;

    public static final String DATABASE_NAME = "plugin_sms.db";

    public static final String[] DATABASE_TABLES = {
            "plugin_sms",
            "sentiment_analysis"
    };

    public static final String[] TABLES_FIELDS = {
            Sms_Data._ID + " integer primary key autoincrement," +
                Sms_Data.RETRIEVAL_TIMESTAMP + " real default 0," +
                Sms_Data.DEVICE_ID + " text default ''," +
                Sms_Data.MESSAGE_TIMESTAMP + " BIGINT default 0," +
                Sms_Data.MSG_TYPE + " text default ''," +
                Sms_Data.MSG_THREAD_ID + " text default ''," +
                Sms_Data.MSG_ADDRESS + " text default ''," +
                Sms_Data.MSG_BODY + " text default ''," +
                Sms_Data.MSG_MMS_PART_TYPE + " text default ''",
            Sentiment_Analysis._ID + " integer primary key autoincrement," +
                    Sentiment_Analysis.RETRIEVAL_TIMESTAMP + " real default 0," +
                    Sentiment_Analysis.DEVICE_ID + " text default ''," +
                    Sentiment_Analysis.MESSAGE_TIMESTAMP + " BIGINT default 0," +
                    Sentiment_Analysis.CATEGORY + " text default ''," +
                    Sentiment_Analysis.TOTAL_WORDS + " integer default 0," +
                    Sentiment_Analysis.DICTIONARY_WORDS + " integer default 0," +
                    Sentiment_Analysis.SCORE + " real default 0," +
                    Sentiment_Analysis.ADDRESS + " text default ''," +
                    Sentiment_Analysis.TYPE + " text default ''"

    };

    public static final class Sms_Data implements BaseColumns {
        private Sms_Data() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plugin_sms");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.sms";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.sms";

        // DB Note:
        // We need both a "retrieval timestamp" and a "message timestamp" because of how AWARE does
        // sync-ing with the remote database.  It checks whether the "timestamp" field is later than
        // the last sync date in order to send the data.  If we put the timestamp of historical data
        // into "timestamp" it will save to the local database, but will never send it to the remote
        // AWARE database because it thinks it already sent it.
        public static final String _ID = "_id";
        public static final String RETRIEVAL_TIMESTAMP = "timestamp";  // Data pull time (for AWARE sync)
        public static final String DEVICE_ID = "device_id";
        public static final String MESSAGE_TIMESTAMP = "message_timestamp"; // from "date" (the datestamp of the original message)
        public static final String MSG_TYPE = "type";
        public static final String MSG_THREAD_ID = "thread_id";
        public static final String MSG_ADDRESS = "address"; // should be md5 encrypted
        public static final String MSG_BODY = "body";
        public static final String MSG_MMS_PART_TYPE = "mms_part_type";
    }

    public static final class Sentiment_Analysis implements BaseColumns {
        private Sentiment_Analysis(){
        }

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sentiment_analysis");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.sentiment.analysis";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.sentiment.analysis";

        public static final String _ID = "_id";
        public static final String RETRIEVAL_TIMESTAMP = "timestamp";  // Data pull time (for AWARE sync)
        public static final String DEVICE_ID = "device_id";
        public static final String MESSAGE_TIMESTAMP = "message_timestamp";
        public static final String CATEGORY = "category";
        public static final String TOTAL_WORDS = "total_words";
        public static final String DICTIONARY_WORDS = "dictionary_words";
        public static final String SCORE = "score";
        public static final String ADDRESS = "address";
        public static final String TYPE = "type";
    }


    private static UriMatcher URIMatcher;
    private static HashMap<String, String> databaseMap;
    private static HashMap<String, String> sentimentMap;

    /**
     * Returns the provider authority that is dynamic
     *
     * @return
     */
    public static String getAuthority(Context context) {
        AUTHORITY = context.getPackageName() + ".provider.sms";
        return AUTHORITY;
        //return Provider.AUTHORITY;
    }

    @Override
    public boolean onCreate() {
        //AUTHORITY = Provider.AUTHORITY;
        AUTHORITY = getContext().getPackageName() + ".provider.sms";

        URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], SMS);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", SMS_ID);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], SENTIMENT);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", SENTIMENT_ID);

        databaseMap = new HashMap<>();
        databaseMap.put(Sms_Data._ID, Sms_Data._ID);
        databaseMap.put(Sms_Data.RETRIEVAL_TIMESTAMP, Sms_Data.RETRIEVAL_TIMESTAMP);
        databaseMap.put(Sms_Data.DEVICE_ID, Sms_Data.DEVICE_ID);
        databaseMap.put(Sms_Data.MESSAGE_TIMESTAMP, Sms_Data.MESSAGE_TIMESTAMP);
        databaseMap.put(Sms_Data.MSG_TYPE, Sms_Data.MSG_TYPE);
        databaseMap.put(Sms_Data.MSG_THREAD_ID, Sms_Data.MSG_THREAD_ID);
        databaseMap.put(Sms_Data.MSG_ADDRESS, Sms_Data.MSG_ADDRESS);
        databaseMap.put(Sms_Data.MSG_BODY, Sms_Data.MSG_BODY);
        databaseMap.put(Sms_Data.MSG_MMS_PART_TYPE, Sms_Data.MSG_MMS_PART_TYPE);
        //TODO Remove these maps if not necessary

        sentimentMap = new HashMap<>();
        sentimentMap.put(Sentiment_Analysis._ID, Sentiment_Analysis._ID);
        sentimentMap.put(Sentiment_Analysis.RETRIEVAL_TIMESTAMP, Sentiment_Analysis.RETRIEVAL_TIMESTAMP);
        sentimentMap.put(Sentiment_Analysis.DEVICE_ID, Sentiment_Analysis.DEVICE_ID);
        sentimentMap.put(Sentiment_Analysis.MESSAGE_TIMESTAMP, Sentiment_Analysis.MESSAGE_TIMESTAMP);
        sentimentMap.put(Sentiment_Analysis.CATEGORY, Sentiment_Analysis.CATEGORY);
        sentimentMap.put(Sentiment_Analysis.TOTAL_WORDS, Sentiment_Analysis.TOTAL_WORDS);
        sentimentMap.put(Sentiment_Analysis.DICTIONARY_WORDS, Sentiment_Analysis.DICTIONARY_WORDS);
        sentimentMap.put(Sentiment_Analysis.SCORE, Sentiment_Analysis.SCORE);
        sentimentMap.put(Sentiment_Analysis.ADDRESS, Sentiment_Analysis.ADDRESS);
        sentimentMap.put(Sentiment_Analysis.TYPE, Sentiment_Analysis.TYPE);

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
            case SMS:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            case SENTIMENT:
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
        switch (URIMatcher.match(uri)) {
            case SMS:
                return Sms_Data.CONTENT_TYPE;
            case SMS_ID:
                return Sms_Data.CONTENT_ITEM_TYPE;
            case SENTIMENT:
                return Sentiment_Analysis.CONTENT_TYPE;
            case SENTIMENT_ID:
                return Sentiment_Analysis.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues initialValues) {
        initialiseDatabase();

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        database.beginTransaction();

        switch (URIMatcher.match(uri)) {
            case SMS:
                long insert_id = database.insertWithOnConflict(DATABASE_TABLES[0], Sms_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);

                if (insert_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            Sms_Data.CONTENT_URI,
                            insert_id);
                    getContext().getContentResolver().notifyChange(new_uri, null, false);
                    database.setTransactionSuccessful();
                    database.endTransaction();
                    return new_uri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            case SENTIMENT:
                long sentiment_id = database.insertWithOnConflict(DATABASE_TABLES[1], Sentiment_Analysis.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                if(sentiment_id > 0){
                    Uri new_uri = ContentUris.withAppendedId(
                            Sentiment_Analysis.CONTENT_URI,
                            sentiment_id);
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
            case SMS:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
                break;
            case SENTIMENT:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(sentimentMap);
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
            case SMS:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            case SENTIMENT:
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
