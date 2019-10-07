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

package org.dharmaseed.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bbethke on 2/7/16.
 */
public class DBManager extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Dharmaseed.db";
    private static final String LOG_TAG = "DBManager";

    private static DBManager instance = null;
    private boolean didUpdate;
    private Context context;

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
            public static final String RECORDING_DATE = "rec_date";
            public static final String RETREAT_ID = "retreat_id";
            public static final String FILE_PATH  = "file_path";

            public static final String TABLE_NAME = "talks";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY,"
                    + TITLE + " TEXT,"
                    + DESCRIPTION + " TEXT,"
                    + VENUE_ID + " INTEGER,"
                    + TEACHER_ID + " INTEGER,"
                    + AUDIO_URL + " TEXT,"
                    + DURATION_IN_MINUTES + " REAL,"
                    + UPDATE_DATE + " TEXT,"
                    + RECORDING_DATE + " TEXT,"
                    + RETREAT_ID + " INTEGER,"
                    + FILE_PATH + " TEXT NOT NULL DEFAULT ''" // empty if not downloaded
                    + ")";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;
        }

        public abstract class Teacher {
            public static final String WEBSITE = "website";
            public static final String DONATION_URL = "donation_url";
            public static final String BIO = "bio";
            public static final String ID = "_id";
            public static final String NAME = "name";
            public static final String PHOTO = "photo";
            public static final String PUBLIC = "public";
            public static final String MONASTIC = "monastic";


            public static final String TABLE_NAME = "teachers";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY,"
                    +WEBSITE+" TEXT,"
                    +DONATION_URL+" TEXT,"
                    +BIO+" TEXT,"
                    +NAME+" TEXT,"
                    +PUBLIC+" INTEGER,"
                    +MONASTIC+" INTEGER,"
                    +PHOTO+" TEXT)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;

        }

        public abstract class Center {
            public static final String WEBSITE = "website";
            public static final String DESCRIPTION = "description";
            public static final String ID = "_id";
            public static final String NAME = "name";
            public static final String HAS_VENUE_VIEW = "has_venue_view";

            public static final String TABLE_NAME = "centers";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY,"
                     + WEBSITE + " TEXT,"
                     + DESCRIPTION + " TEXT,"
                     + NAME + " TEXT,"
                     + HAS_VENUE_VIEW + " INT"
                     + ")";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;

        }

        public abstract class Retreat {
            public static final String ID = "_id";
            public static final String NAME = "name";
            public static final String START_DATE = "start_date";

            public static final String TABLE_NAME = "retreats";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" (" +
                    ID + " INTEGER PRIMARY KEY," +
                    NAME + " TEXT," +
                    START_DATE + " TEXT" +
                    ")";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;

        }

        public abstract class TalkStars {
            public static final String ID = "_id";

            public static final String TABLE_NAME = "talk_stars";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;
        }

        public abstract class TeacherStars {
            public static final String ID = "_id";

            public static final String TABLE_NAME = "teacher_stars";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;
        }

        public abstract class CenterStars {
            public static final String ID = "_id";

            public static final String TABLE_NAME = "center_stars";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;
        }

        public abstract class DownloadedTalks {
            public static final String ID = "_id";

            public static final String TABLE_NAME = "downloaded_talks";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("+ID+" INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;
        }

        public abstract class Edition {
            public static final String TABLE = "_table";
            public static final String EDITION = "edition";

            public static final String TABLE_NAME = "editions";
            public static final String CREATE_TABLE = "CREATE TABLE "+TABLE_NAME+" ("
                    +TABLE+" TEXT PRIMARY KEY,"+EDITION+" TEXT)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME;

            public static final String LAST_SYNC = "last_sync";
        }

    }

    private DBManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        didUpdate = false;

        if (context != null) {
            File dbFile = context.getDatabasePath(DB_NAME);
            if(! dbFile.exists()) {
                Log.i("dbManager", "Trying to populate with pre-seeded database");
                try {
                    // Copy the pre-seeded database if it exists
                    InputStream dbIn = context.getAssets().open(DB_NAME);
                    dbFile.getParentFile().mkdirs();
                    OutputStream dbOut = new FileOutputStream(dbFile);

                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = dbIn.read(buf)) > 0) {
                        dbOut.write(buf, 0, len);
                    }

                    dbOut.flush();
                    dbOut.close();
                    dbIn.close();
                } catch (IOException ioe) {
                    Log.e("dbManager", ioe.toString());
                }
            }
        }
    }

    public static synchronized DBManager getInstance(Context context) {
        if (instance == null) {
            instance = new DBManager(context.getApplicationContext());
        }

        return instance;
    }

    /**
     * Used for testing purposes
     * @return the db name
     */
    protected String getDbName() {
        return DB_NAME;
    }

    public Context getContext() { return context; }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(C.Talk.CREATE_TABLE);
        db.execSQL(C.Teacher.CREATE_TABLE);
        db.execSQL(C.Center.CREATE_TABLE);
        db.execSQL(C.Retreat.CREATE_TABLE);
        db.execSQL(C.TalkStars.CREATE_TABLE);
        db.execSQL(C.TeacherStars.CREATE_TABLE);
        db.execSQL(C.CenterStars.CREATE_TABLE);
        db.execSQL(C.DownloadedTalks.CREATE_TABLE);
        db.execSQL(C.Edition.CREATE_TABLE);

        // Populate editions table with initial data
        ContentValues v = new ContentValues();
        v.put(C.Edition.TABLE, C.Talk.TABLE_NAME);
        db.insert(C.Edition.TABLE_NAME, null, v);
        v.put(C.Edition.TABLE, C.Teacher.TABLE_NAME);
        db.insert(C.Edition.TABLE_NAME, null, v);
        v.put(C.Edition.TABLE, C.Center.TABLE_NAME);
        db.insert(C.Edition.TABLE_NAME, null, v);
        v.put(C.Edition.TABLE, C.Retreat.TABLE_NAME);
        db.insert(C.Edition.TABLE_NAME, null, v);
        v.put(C.Edition.TABLE, C.Edition.LAST_SYNC);
        v.put(C.Edition.EDITION, 0);
        db.insert(C.Edition.TABLE_NAME, null, v);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(
                "DBManager",
                "Upgrading database from v" + oldVersion + " to v" + newVersion
        );
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // do nothing for now - this just prevents exceptions if the user
        // downgrades
    }

    /**
     * Removes the edition entry for `tableName`
     * @param db
     * @param tableName
     */
    private void clearEdition(SQLiteDatabase db, String tableName) {
        ContentValues v = new ContentValues();
        v.put(C.Edition.TABLE, tableName);
        v.putNull(C.Edition.EDITION);
        db.insertWithOnConflict(C.Edition.TABLE_NAME, null, v, SQLiteDatabase.CONFLICT_REPLACE);

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

    /**
     * Delete all ids from DB with a query like:
     * DELETE FROM `table_name` WHERE _id IN (ids[0], ids[1], ... ids[n-1])
     * @param ids
     * @param tableName
     */
    public void deleteIDs(List<Integer> ids, String tableName) {
        if (!ids.isEmpty()) {
            String idsToDelete = "(" + ids.get(0);
            for (Integer item : ids) {
                idsToDelete += "," + item;
            }
            idsToDelete += ")";

            SQLiteDatabase db = getWritableDatabase();
            db.delete(tableName, "_id IN " + idsToDelete, null);
        }
    }

    public static String getTeacherPhotoFilename(int teacherID) {
        return "teacher-"+teacherID+".png";
    }

    public boolean isStarred(String starTableName, int id) {
        SQLiteDatabase db = getReadableDatabase();
        String query = String.format("SELECT _id FROM %s WHERE _id=%s",
                starTableName,
                id);
        Cursor cursor = db.rawQuery(query, null);
        return (cursor.getCount() != 0);
    }

    public void addStar(String starTableName, int id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("_id", id);
        db.insert(starTableName, null, v);
    }

    public void removeStar(String starTableName, int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(starTableName, "_id="+id, null);
    }

    /**
     * Updates the Talk table to set the "file_path" column to the talk path
     * also add the Talk ID to the downloaded_talks table
     * @param talkId the talk ID
     * @param talkPath the path to the talk's downloaded file
     * @return true if all rows were updated successfully, false if at least one was not
     */
    public boolean addDownload(int talkId, String talkPath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C.Talk.FILE_PATH, talkPath);
        String whereClause = C.Talk.ID + "=" + talkId;
        if (db.update(C.Talk.TABLE_NAME, cv, whereClause, null) != 1)
            return false;

        // add id to downloaded_talks table
        cv.clear();
        cv.put(C.DownloadedTalks.ID, talkId);
        // insert returns a -1 on error
        if (db.insert(C.DownloadedTalks.TABLE_NAME, null, cv) == -1)
            return false;

        return true;
    }

    public boolean addDownload(Talk talk) {
        return addDownload(talk.getId(), talk.getPath());
    }

    /**
     * Updates the Talk table to set the "file_path" column to the empty string and removes
     * the talk id from the downloaded_talks table
     * @param talkId the talk ID to delete
     * @return true if all rows were updated successfully, false if at least one was not
     */
    public boolean removeDownload(int talkId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(C.Talk.FILE_PATH, ""); // a path of "" indicates that the talk is not downloaded
        String whereClause = C.Talk.ID + "=" + talkId;
        if (db.update(C.Talk.TABLE_NAME, cv, whereClause, null) != 1)
            return false;

        // remove row from downloaded_talks table
        whereClause = C.DownloadedTalks.ID + "=" + talkId;
        if (db.delete(C.DownloadedTalks.TABLE_NAME, whereClause, null) != 1)
            return false;

        return true;
    }

    public boolean removeDownload(Talk talk) {
        return removeDownload(talk.getId());
    }

    /**
     * @return whether we should sync with the dharmaseed api again
     */
    public boolean shouldSync() {
        if (didUpdate) {
            didUpdate = false;
            return true;
        }

        String query = String.format("SELECT %s FROM %s WHERE %s=\"%s\"",
                DBManager.C.Edition.EDITION,
                DBManager.C.Edition.TABLE_NAME,
                DBManager.C.Edition.TABLE,
                DBManager.C.Edition.LAST_SYNC);
        Cursor cursor = getReadableDatabase().rawQuery(query, null);
        boolean outOfDate = false;
        if(cursor.moveToFirst()) {
            long lastSync = cursor.getLong(cursor.getColumnIndexOrThrow(DBManager.C.Edition.EDITION));
            Date nowDate = new Date();
            long now = nowDate.getTime();
            if(now - lastSync > 1000*60*60*1) { // Update every hour
                outOfDate = true;
            }
        }
        cursor.close();
        return outOfDate;
    }

    /**
     * Get an alias for a fully qualified column name. This is useful in naming columns in a query
     * using SQL AS clauses and referencing them later
     * @param fullyQualifiedColumn Name of the column with the table name prepended (i.e. "talks.title")
     * @return The alias for this column
     */
    @NonNull
    public static String getAlias(String fullyQualifiedColumn) {
        return fullyQualifiedColumn.replace(".", "_");
    }
}
