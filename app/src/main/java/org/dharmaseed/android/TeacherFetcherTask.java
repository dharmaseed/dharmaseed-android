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

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by bbethke on 2/19/16.
 */
public class TeacherFetcherTask extends DataFetcherTask {

    private static final String LOG_TAG = "TeacherFetcherTask";

    Context context;

    public TeacherFetcherTask(DBManager dbManager, NavigationActivity navigationActivity, Context context) {
        super(dbManager, navigationActivity);
        this.context = context;
    }

    public boolean fetchTeacherPhoto(
            Context context, int teacherID, String teacherPhoto,
            Request.Builder requestBuilder
    ) {
        boolean success = true;
        Log.i(LOG_TAG, "Fetching teacher photo " + teacherID);
        Request request = requestBuilder.url(
                "https://www.dharmaseed.org/api/1/teachers/" +
                teacherID + "/" + teacherPhoto + "/?maxW=120&maxH=180"
        ).build();
        try {
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                FileManager.setPhoto(context, teacherID, body.bytes());
                body.close();
            } else {
                Log.e(LOG_TAG, "Error retrieving teacher photo: code " + response.code());
            }
        } catch (IOException e2) {
            Log.e(LOG_TAG, "Error retrieving or writing teacher photo: " + e2);
        }
        return success;
    }

    @Override
    protected Void doInBackground(Void... params) {

        updateTable(DBManager.C.Teacher.TABLE_NAME, DBManager.C.Teacher.ID,
                "teachers/",
                new String[]{
                        DBManager.C.Teacher.WEBSITE,
                        DBManager.C.Teacher.DONATION_URL,
                        DBManager.C.Teacher.BIO,
                        DBManager.C.Teacher.NAME,
                        DBManager.C.Teacher.PHOTO,
                        DBManager.C.Teacher.PUBLIC,
                        DBManager.C.Teacher.MONASTIC
                });

        // Download teacher photos
        String query = String.format(
                "SELECT %s, %s FROM %s",
                DBManager.C.Teacher.ID,
                DBManager.C.Teacher.PHOTO,
                DBManager.C.Teacher.TABLE_NAME
        );
        Cursor cursor = dbManager.getReadableDatabase().rawQuery(query, null);

        Request.Builder requestBuilder = new Request.Builder();
        while(cursor.moveToNext()) {
            String photo = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.PHOTO));
            int id    = cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.ID));
            if(!photo.equals("") && FileManager.findPhoto(context, id, false) == null) {
                // Only need to fetch the photo if we don't already have it
                fetchTeacherPhoto(context, id, photo, requestBuilder);
            } else {
                Bitmap photo1 = FileManager.getPhoto(context, id);
                if (photo1 != null && photo1.sameAs(FileManager.getAssetPhoto(context, id))) {
                    FileManager.deletePhoto(context, id);
                    Log.i(LOG_TAG, "Deleting redundant teacher photo " + id);
                }
            }
        }

        publishProgress();
        return null;
    }

    @Override
    protected void extraTableProcessing(DBManager dbManager, JSONObject items) throws JSONException {
        // always update teacher photos for newly added teacher entries just in case the teacher
        // entry is actually being updated (deleted and then re-added) with a new photo
        Request.Builder requestBuilder = new Request.Builder();
        Iterator<String> it = items.keys();
        Log.i(LOG_TAG, "updating " + items.length() + "teacher photos");
        while (it.hasNext()) {
            String itemID = it.next();
            JSONObject item = items.getJSONObject(itemID);
            String photo = item.getString(DBManager.C.Teacher.PHOTO);
            fetchTeacherPhoto(context, Integer.parseInt(itemID), photo, requestBuilder);
        }
    }
}
