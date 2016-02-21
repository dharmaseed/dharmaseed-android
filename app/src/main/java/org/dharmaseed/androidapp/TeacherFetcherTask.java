package org.dharmaseed.androidapp;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;

import java.io.FileNotFoundException;
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

    public TeacherFetcherTask(DBManager dbManager, CursorAdapter cursorAdapter, Context context) {
        super(dbManager, cursorAdapter);
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {

        // Update the list view immediately
        publishProgress();

        updateTable(DBManager.C.Teacher.TABLE_NAME, DBManager.C.Teacher.ID,
                "teachers/",
                new String[]{
                        DBManager.C.Teacher.WEBSITE,
                        DBManager.C.Teacher.BIO,
                        DBManager.C.Teacher.NAME,
                        DBManager.C.Teacher.PHOTO
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
                    context.openFileInput(filename);
                } catch (FileNotFoundException e1) {
                    // Only need to fetch the photo if we don't already have it
                    Log.i("teacherFetcherTask", "Fetching teacher photo " + id);
                    Request request = requestBuilder.url("http://www.dharmaseed.org/api/1/teachers/" + id + "/" + photo + "/?maxW=120&maxH=180").build();
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
