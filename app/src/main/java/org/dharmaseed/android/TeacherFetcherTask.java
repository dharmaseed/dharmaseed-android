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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by bbethke on 2/19/16.
 */
public class TeacherFetcherTask extends DataFetcherTask {

    Context context;

    public TeacherFetcherTask(DBManager dbManager, NavigationActivity navigationActivity, Context context) {
        super(dbManager, navigationActivity);
        this.context = context;
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
            String filename = DBManager.getTeacherPhotoFilename(id);
            if(!photo.equals("")) {
                try {
                    FileInputStream file = context.openFileInput(filename);
                    file.close();
                } catch (IOException e1) {
                    // Only need to fetch the photo if we don't already have it
                    Log.i("teacherFetcherTask", "Fetching teacher photo " + id);
                    Request request = requestBuilder.url("https://www.dharmaseed.org/api/1/teachers/" + id + "/" + photo + "/?maxW=120&maxH=180").build();
                    try {
                        Response response = httpClient.newCall(request).execute();
                        if (response.isSuccessful()) {
                            ResponseBody body = response.body();
                            FileOutputStream outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
                            outputStream.write(body.bytes());
                            body.close();
                            outputStream.close();
                        } else {
                            Log.e("teacherFetcherTask", "Error retrieving teacher photo: code " + response.code());
                        }
                    } catch (IOException e2) {
                        Log.e("teacherFetcherTask", "Error retrieving or writing teacher photo: " + e2);
                    }
                }
            }
        }


        publishProgress();
        return null;
    }
}
