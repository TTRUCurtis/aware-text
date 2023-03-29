
package com.aware.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * ContentProvider database helper<br/>
 * This class is responsible to make sure we have the most up-to-date database structures from plugins and sensors
 *
 * @author denzil
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private final boolean DEBUG = true;

    private final String TAG = "AwareDBHelper";

    private final String[] databaseTables;
    private final String[] tableFields;
    private final int newVersion;

    private HashMap<String, String> renamed_columns = new HashMap<>();

    public DatabaseHelper(Context context, String database_name, CursorFactory cursor_factory, int database_version, String[] database_tables, String[] table_fields) {
        super(context, database_name, cursor_factory, database_version);
        databaseTables = database_tables;
        tableFields = table_fields;
        newVersion = database_version;
    }

    public void setRenamedColumns(HashMap<String, String> renamed) {
        renamed_columns = renamed;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (DEBUG) Log.w(TAG, "Creating database: " + db.getPath());
        for (int i = 0; i < databaseTables.length; i++) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + databaseTables[i] + " (" + tableFields[i] + ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS time_device ON " + databaseTables[i] + " (timestamp, device_id);");
        }
        db.setVersion(newVersion);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (DEBUG) Log.w(TAG, "Upgrading database: " + db.getPath());

        for (int i = 0; i < databaseTables.length; i++) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + databaseTables[i] + " (" + tableFields[i] + ");");

            //Modify existing tables if there are changes, while retaining old data. This also works for brand new tables, where nothing is changed.
            List<String> columns = getColumns(db, databaseTables[i]);

            db.execSQL("ALTER TABLE " + databaseTables[i] + " RENAME TO temp_" + databaseTables[i] + ";");

            db.execSQL("CREATE TABLE " + databaseTables[i] + " (" + tableFields[i] + ");");
            db.execSQL("CREATE INDEX IF NOT EXISTS time_device ON " + databaseTables[i] + " (timestamp, device_id);");

            columns.retainAll(getColumns(db, databaseTables[i]));

            String cols = TextUtils.join(",", columns);
            String new_cols = cols;

            if (renamed_columns.size() > 0) {
                for (String key : renamed_columns.keySet()) {
                    if (DEBUG) Log.d(TAG, "Renaming: " + key + " -> " + renamed_columns.get(key));
                    new_cols = new_cols.replace(key, Objects.requireNonNull(renamed_columns.get(key)));
                }
            }

            //restore old data back
            if (DEBUG)
                Log.d(TAG, String.format("INSERT INTO %s (%s) SELECT %s from temp_%s;", databaseTables[i], cols, cols, databaseTables[i]));

            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s from temp_%s;", databaseTables[i], cols, cols, databaseTables[i]));
            db.execSQL("DROP TABLE temp_" + databaseTables[i] + ";");
        }
        db.setVersion(newVersion);
    }

    /**
     * Creates a String of a JSONArray representation of a database cursor result
     *
     * @param cursor input cursor
     * @return String
     */
    public static String cursorToString(Cursor cursor) {
        JSONArray jsonArray = new JSONArray();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int nColumns = cursor.getColumnCount();
                JSONObject row = new JSONObject();
                for (int i = 0; i < nColumns; i++) {
                    String colName = cursor.getColumnName(i);
                    if (colName != null) {
                        try {
                            switch (cursor.getType(i)) {
                                case Cursor.FIELD_TYPE_BLOB:
                                    row.put(colName, Arrays.toString(cursor.getBlob(i)));
                                    break;
                                case Cursor.FIELD_TYPE_FLOAT:
                                    row.put(colName, cursor.getDouble(i));
                                    break;
                                case Cursor.FIELD_TYPE_INTEGER:
                                    row.put(colName, cursor.getLong(i));
                                    break;
                                case Cursor.FIELD_TYPE_NULL:
                                    row.put(colName, null);
                                    break;
                                case Cursor.FIELD_TYPE_STRING:
                                    row.put(colName, cursor.getString(i));
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                jsonArray.put(row);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();

        return jsonArray.toString();
    }

    private static List<String> getColumns(SQLiteDatabase db, String tableName) {
        List<String> columns = null;
        Cursor database_meta = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 1", null);
        if (database_meta != null) {
            columns = new ArrayList<>(Arrays.asList(database_meta.getColumnNames()));
        }
        if (database_meta != null && !database_meta.isClosed()) database_meta.close();

        return columns;
    }

}
