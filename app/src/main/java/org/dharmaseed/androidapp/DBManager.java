package org.dharmaseed.androidapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by bbethke on 2/7/16.
 */
public class DBManager extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Dharmaseed.db";

    // Database contract class
    final class C {
        public C() {}

        public abstract class Talk {
            public static final String TITLE = "title";
            public static final String DESCRIPTION = "description";
            public static final String ID = "id";
            public static final String VENUE_ID = "venue_id";
            public static final String TEACHER_ID = "teacher_id";
            public static final String AUDIO_URL = "audio_url";
            public static final String DURATION_IN_MINUTES = "duration_in_minutes";
            public static final String UPDATE_DATE = "update_date";
            public static final String RECORDING_DATE = "recording_date";
            public static final String RETREAT_ID = "retreat_id";

            public static final String TABLE_NAME = "talks";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY,"
                    +TITLE+" TEXT,"
                    +DESCRIPTION+" TEXT,"
                    +VENUE_ID+" INTEGER,"
                    +TEACHER_ID+" INTEGER,"
                    +AUDIO_URL+" TEXT,"
                    +DURATION_IN_MINUTES+" REAL,"
                    +UPDATE_DATE+" TEXT,"
                    +RECORDING_DATE+" TEXT,"
                    +RETREAT_ID+" INTEGER)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;
        }

    }

    public DBManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(C.Talk.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(C.Talk.DROP_TABLE);
        onCreate(db);
    }

    private void insertValue(ContentValues values, JSONObject obj, String key) {
        try {
            String value = obj.getString(key);
            values.put(key, value);
        } catch (JSONException e) {
            values.putNull(key);
        }
    }

    // Insert a talk into the database, given the JSON object for a talk returned by dharamaseed.org's API
    public void insertTalk(JSONObject talk) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            insertValue(values, talk, C.Talk.TITLE);
            insertValue(values, talk, C.Talk.DESCRIPTION);
            insertValue(values, talk, C.Talk.ID);
            insertValue(values, talk, C.Talk.VENUE_ID);
            insertValue(values, talk, C.Talk.TEACHER_ID);
            insertValue(values, talk, C.Talk.AUDIO_URL);
            insertValue(values, talk, C.Talk.DURATION_IN_MINUTES);
            insertValue(values, talk, C.Talk.UPDATE_DATE);
            insertValue(values, talk, C.Talk.RECORDING_DATE);
            insertValue(values, talk, C.Talk.RETREAT_ID);

            db.insertWithOnConflict(C.Talk.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();

        } catch (Exception e) {
            Log.e("DBInsert", e.toString());
        }
    }

    // Return all talk titles
    // TODO: Change to using a SimpleCursorAdapter
    public ArrayList<String> getTalkTitles() {
        ArrayList<String> result = new ArrayList<String>();
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM " + C.Talk.TABLE_NAME, null);
            if(cursor.getCount() >= 1) {
                while(cursor.moveToNext()) {
                    result.add(cursor.getString(cursor.getColumnIndexOrThrow(C.Talk.TITLE)));
                }
            }
        } catch (Exception e) {
            Log.e("DBManager", e.toString());
        }
        return result;
    }
}
