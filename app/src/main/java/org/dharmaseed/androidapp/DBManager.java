/*
 *     Dharmaseed Android app
 *     Copyright (C) 2016  Brett Bethke
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.dharmaseed.androidapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by bbethke on 2/7/16.
 */
public class DBManager extends SQLiteOpenHelper {

    private static final int DB_VERSION = 27;
    private static final String DB_NAME = "Dharmaseed.db";

    // Database contract class
    final class C {
        public C() {}

        public abstract class Talk {
            public static final String TITLE = "title";
            public static final String DESCRIPTION = "description";
            public static final String ID = "_id";
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

        public abstract class TalkStars {
            public static final String TALK_ID = "talk_id";

            public static final String TABLE_NAME = "talk_stars";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+TALK_ID+" INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;
        }

        public abstract class Teacher {
            public static final String WEBSITE = "website";
            public static final String BIO = "bio";
            public static final String ID = "_id";
            public static final String NAME = "name";
            public static final String PHOTO = "photo";

            public static final String TABLE_NAME = "teachers";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY,"
                    +WEBSITE+" TEXT,"
                    +BIO+" TEXT,"
                    +NAME+" TEXT,"
                    +PHOTO+" TEXT)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;

        }

        public abstract class Center {
            public static final String WEBSITE = "website";
            public static final String DESCRIPTION = "description";
            public static final String ID = "_id";
            public static final String NAME = "name";

            public static final String TABLE_NAME = "centers";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY,"
                    +WEBSITE+" TEXT,"
                    +DESCRIPTION+" TEXT,"
                    +NAME+" TEXT)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;

        }

        public abstract class Edition {
            public static final String TABLE = "_table";
            public static final String EDITION = "edition";

            public static final String TABLE_NAME = "editions";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("
                    +TABLE+" TEXT PRIMARY KEY,"+EDITION+" TEXT)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;
        }

    }

    public DBManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(C.Talk.CREATE_TABLE);
        db.execSQL(C.TalkStars.CREATE_TABLE);
        db.execSQL(C.Teacher.CREATE_TABLE);
        db.execSQL(C.Center.CREATE_TABLE);
        db.execSQL(C.Edition.CREATE_TABLE);

        // Populate editions table with initial data
        ContentValues v = new ContentValues();
        v.put(C.Edition.TABLE, C.Talk.TABLE_NAME);
        db.insert(C.Edition.TABLE_NAME, null, v);
        v.put(C.Edition.TABLE, C.Teacher.TABLE_NAME);
        db.insert(C.Edition.TABLE_NAME, null, v);
        v.put(C.Edition.TABLE, C.Center.TABLE_NAME);
        db.insert(C.Edition.TABLE_NAME, null, v);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("DBManager", "Upgrading database to version "+DB_VERSION);
        db.execSQL(C.Talk.DROP_TABLE);
        db.execSQL(C.TalkStars.DROP_TABLE);
        db.execSQL(C.Teacher.DROP_TABLE);
        db.execSQL(C.Center.DROP_TABLE);
        db.execSQL(C.Edition.DROP_TABLE);
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

    // Insert an item into the database, given the JSON object for an item returned by dharamaseed.org's API
    public void insertItem(String itemID, JSONObject item, String tableName, String[] itemKeys) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(C.Talk.ID, itemID);
            for(String itemKey : itemKeys) {
                insertValue(values, item, itemKey);
            }

            db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);

        } catch (Exception e) {
            Log.e("DBInsert", e.toString());
        }
    }

    public void insertItems(JSONObject json, String tableName, String[] itemKeys) {
        try {
            JSONObject items = json.getJSONObject("items");
            Iterator<String> it = items.keys();
            while (it.hasNext()) {
                String itemID = it.next();
                JSONObject item = items.getJSONObject(itemID);
                insertItem(itemID, item, tableName, itemKeys);
            }
        } catch (JSONException e) {
            Log.d("insertItems", e.toString());
        }
    }

    public static String getTeacherPhotoFilename(int teacherID) {
        return "teacher-"+teacherID+".png";
    }

    public boolean isTalkStarred(int talkId) {
        SQLiteDatabase db = getReadableDatabase();
        String query = String.format("SELECT %s FROM %s WHERE %s=%s",
                C.TalkStars.TALK_ID,
                C.TalkStars.TABLE_NAME,
                C.TalkStars.TALK_ID,
                talkId);
        Cursor cursor = db.rawQuery(query, null);
        return (cursor.getCount() != 0);
    }

    public void starTalk(int talkId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(C.TalkStars.TALK_ID, talkId);
        db.insert(C.TalkStars.TABLE_NAME, null, v);
    }

    public void unstarTalk(int talkId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(C.TalkStars.TABLE_NAME, C.TalkStars.TALK_ID+"="+talkId, null);
    }

}
