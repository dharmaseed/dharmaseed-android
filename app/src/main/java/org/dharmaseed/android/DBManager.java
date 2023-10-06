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
import androidx.annotation.NonNull;
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
public class DBManager extends AbstractDBManager {

    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "Dharmaseed.db";
    private static final String LOG_TAG = "DBManager";

    private static DBManager instance = null;
    private boolean didUpdate;

    private DBManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        didUpdate = false;

        if (context != null) {
            File dbFile = context.getDatabasePath(DB_NAME);
            if(! dbFile.exists()) {
                Log.i(LOG_TAG, "Trying to populate with pre-seeded database");
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
        db.execSQL(C.TalkHistory.CREATE_TABLE);
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
                LOG_TAG,
                "Upgrading database from v" + oldVersion + " to v" + newVersion
        );
        if (oldVersion == 1 && newVersion > 1) {
            db.execSQL(C.TalkHistory.CREATE_TABLE);
            Log.i(LOG_TAG,"Upgrade: Created talk history table");
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(
                LOG_TAG,
                "Downgrading database from v" + oldVersion + " to v" + newVersion
        );
        if (newVersion <= 1) {
            db.execSQL(C.TalkHistory.DROP_TABLE);
            Log.i(LOG_TAG,"Downgrade: Dropped talk history table");
        }
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
        boolean starred = (cursor.getCount() != 0);
        cursor.close();
        return starred;
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

    public double getTalkProgress(int id) {
        SQLiteDatabase db = getReadableDatabase();
        String query = String.format("SELECT %s FROM %s WHERE %s=%s",
                C.TalkHistory.PROGRESS_IN_MINUTES,
                C.TalkHistory.TABLE_NAME,
                C.TalkHistory.ID,
                id);
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.getCount() <= 0) {
            cursor.close();
            return 0.0;
        }
        cursor.moveToFirst();
        double progress = cursor.getDouble(0);
        cursor.close();
        return progress;
    }

    public void setTalkProgress(int id, String date_time, double progress) {
        SQLiteDatabase db = getWritableDatabase();
        String query = String.format("SELECT _id FROM %s WHERE _id=%s",
                C.TalkHistory.TABLE_NAME,
                id);

        ContentValues v = new ContentValues();
        v.put(C.TalkHistory.ID, id);
        v.put(C.TalkHistory.DATE_TIME, date_time);
        v.put(C.TalkHistory.PROGRESS_IN_MINUTES, progress);
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.getCount() <= 0) {
            db.insert(C.TalkHistory.TABLE_NAME, null, v);
            Log.i(LOG_TAG, "Created new talk history item for talk "+id+"("+progress+" min)");
        } else {
            db.update(C.TalkHistory.TABLE_NAME, v, C.TalkHistory.ID+"=?",
                    new String[]{Integer.toString(id)});
            Log.i(LOG_TAG, "Updated talk history item for talk "+id+"("+progress+" min)");
        }
        cursor.close();
        db.close();
    }

    public boolean addDownload(Talk talk) {
        return addDownload(talk.getId(), talk.getPath());
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
