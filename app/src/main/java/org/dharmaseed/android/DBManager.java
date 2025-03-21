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
import android.database.sqlite.SQLiteException;
import androidx.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bbethke on 2/7/16.
 */
public class DBManager extends AbstractDBManager {

    private static final int DB_VERSION = 5;
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
                    copyAssetDB(dbFile);
                } catch (IOException ioe) {
                    Log.e("dbManager", ioe.toString());
                }
            }
            /**
             * to completely reset the databases and re-download all the tables/photos etc from
             * the web, follow these steps:
             * 1. remove all files from assets/teacher_photos
             * 2. uncomment the line below
             * 3. increment DB_VERSION (if you intend to get a new .db file to be rolled out to all users)
             * 4. start the app on a completely fresh VM (Wipe Data in Device Manager)
             * 5. after the refresh is done, copy
             *    - Dharmaseed.db (/data/user/0/org.dharmaseed.android/databases/Dharmaseed.db)
             *    - teacher photos (/data/user/0/org.dharmaseed.android/files/teacher-*.png)
             *    to the assets folder
             */
            // dbFile.delete();
        }
    }

    protected void copyAssetDB(File destFile) throws IOException {
        InputStream dbIn = context.getAssets().open(DB_NAME);
        destFile.getParentFile().mkdirs();
        OutputStream dbOut = new FileOutputStream(destFile);

        byte[] buf = new byte[1024];
        int len;
        while ((len = dbIn.read(buf)) > 0) {
            dbOut.write(buf, 0, len);
        }

        dbOut.flush();
        dbOut.close();
        dbIn.close();
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
        db.execSQL(C.TalkTeachers.CREATE_TABLE);
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
        if (oldVersion <= 3 && newVersion > 3) {
            db.execSQL(C.TalkTeachers.CREATE_TABLE);
            Log.i(LOG_TAG,"Upgrade: Created talk teachers table");
        }

        // Optionally import updated tables from the assets DB.
        // This should be the last step in onUpgrade, to ensure that we don't encounter any problem
        // _after_ setTransactionSuccessful is called below.
        File dbUpgradeFile = context.getDatabasePath(DB_NAME+".upgrade");
        try {
            copyAssetDB(dbUpgradeFile);
            String dbUpgradePath = dbUpgradeFile.getAbsolutePath();
            SQLiteDatabase assetDb = SQLiteDatabase.openDatabase(dbUpgradePath, null, SQLiteDatabase.OPEN_READONLY);
            int assetVersion = assetDb.getVersion();
            assetDb.close();
            Log.d(LOG_TAG, "Asset DB has version " + assetVersion);
            if (assetVersion == newVersion) {
                Log.i(LOG_TAG, "Asset DB version matches upgrade version. Copying updated tables!");
                db.execSQL("ATTACH DATABASE '" + dbUpgradePath + "' AS upgradeDb");
                String[][] t_info = {
                        {C.Talk.TABLE_NAME, C.Talk.CREATE_TABLE},
                        {C.TalkTeachers.TABLE_NAME, C.TalkTeachers.CREATE_TABLE},
                        {C.Teacher.TABLE_NAME, C.Teacher.CREATE_TABLE},
                        {C.Center.TABLE_NAME, C.Center.CREATE_TABLE},
                        {C.Edition.TABLE_NAME, C.Edition.CREATE_TABLE}
                };
                for (String[] t: t_info) {
                    db.execSQL("DROP TABLE IF EXISTS " + t[0]);
                    db.execSQL(t[1]);
                    db.execSQL("INSERT INTO " + t[0] + " SELECT * FROM upgradeDb." + t[0]);
                    Log.i(LOG_TAG, "Copied table " + t[0] + " from asset DB.");
                }
                /* note on transactions:
                onUpgrade is called with an open transaction such that the upgrade can be rolled
                back in case something goes wrong.
                 */
                db.setTransactionSuccessful();
                db.endTransaction(); // successfully end upgrade transaction
                db.execSQL("DETACH upgradeDb"); // DETACH can't be performed within a transaction
                db.beginTransaction(); // open new transaction, which is ended by caller
                Log.i(LOG_TAG, "Successfully transferred all tables from asset DB");
            }
        } catch (IOException e) {
            Log.w(LOG_TAG, "Aborting DB upgrade due to IO error: " + e.getMessage());
        } catch (SQLiteException e) {
            Log.w(LOG_TAG, "Aborting DB upgrade due to SQLite error: " + e.getMessage());
        }

        dbUpgradeFile.delete();
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
        ContentValues values = new ContentValues();
        values.put(C.Talk.ID, itemID);
        for(String itemKey : itemKeys) {
            insertValue(values, item, itemKey);
        }

        db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void insertItems(JSONObject json, String tableName, String[] itemKeys) throws JSONException {
        JSONObject items = json.getJSONObject("items");
        Iterator<String> it = items.keys();
        while (it.hasNext()) {
            String itemID = it.next();
            JSONObject item = items.getJSONObject(itemID);
            insertItem(itemID, item, tableName, itemKeys);
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

    public void deleteID(Integer id, String tableName) {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(id);
        deleteIDs(ids, tableName);
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

    /**
     * Retrieves the edition string for a table in the DB
     * @param tableName: name of the table
     */
    public String getEdition(String tableName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT "+DBManager.C.Edition.EDITION+" FROM "
                        +DBManager.C.Edition.TABLE_NAME
                        +" WHERE "+DBManager.C.Edition.TABLE+"=\""+tableName+"\""
                , null);
        String edition = "";
        if (cursor.moveToFirst()) {
            edition = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Edition.EDITION));
        }
        cursor.close();
        return edition;
    }

    /**
     * Sets the edition string for a table in the DB
     * @param tableName: name of the table
     * @param newEdition: the new edition of the table
     */
    public void setEdition(String tableName, String newEdition) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBManager.C.Edition.TABLE, tableName);
        values.put(DBManager.C.Edition.EDITION, newEdition);
        db.insertWithOnConflict(DBManager.C.Edition.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
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
    }

    public boolean addDownload(Talk talk) {
        return addDownload(talk.getId());
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
