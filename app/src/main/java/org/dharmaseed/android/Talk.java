package org.dharmaseed.android;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Model of the TALK table in the DB
 *
 * @author jakewilson
 */
public class Talk {

    private static final String LOG_TAG = "Talk";

    private String title;
    private String description;
    private String audioUrl;
    private String date;
    private String teacherName;
    private ArrayList<String> extraTeacherNames;
    private String centerName;
    private String photoFileName;
    private String path; // where the talk is downloaded, if it is

    private int id;
    private int venueId;
    private int teacherId;
    private int retreatId;

    private Context context;

    private double durationInMinutes;

    public Talk() {
        extraTeacherNames = new ArrayList<>();
    }

    /**
     * Create a talk object by setting all model values from the database
     * @param dbManager The database manager
     * @param context Application context
     * @param talkID talk ID
     */
    public static Talk lookup(DBManager dbManager, Context context, int talkID) {
        Talk talk = null;
        Cursor cursor = getCursor(dbManager, talkID);
        if (cursor.moveToFirst()) {
            talk = new Talk();
            talk.title = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TITLE)).trim();
            talk.description = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DESCRIPTION)).trim();

            String url = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.AUDIO_URL));
            talk.audioUrl = "https://www.dharmaseed.org" + url;

            String recDate = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.RECORDING_DATE));
            if(recDate == null) {
                recDate = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.UPDATE_DATE));
            }
            talk.date = recDate;

            talk.teacherId = cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TEACHER_ID));
            talk.teacherName = cursor.getString(cursor.getColumnIndexOrThrow("teacher_name")).trim();
            talk.centerName = cursor.getString(cursor.getColumnIndexOrThrow("center_name")).trim();
            talk.photoFileName = DBManager.getTeacherPhotoFilename(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.ID)));
            talk.retreatId = cursor.getColumnIndexOrThrow(DBManager.C.Talk.RETREAT_ID);
            talk.durationInMinutes = cursor.getDouble(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DURATION_IN_MINUTES));
            talk.path = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.FILE_PATH));

            talk.id = talkID;
            talk.context = context;

            setExtraTeachers(dbManager, talk);

        } else {
            Log.e(LOG_TAG, "Could not look up talk, id=" + talkID);
        }

        cursor.close();
        return talk;

    }

    private static Cursor getCursor(DBManager dbManager, int talkID) {

        SQLiteDatabase db = dbManager.getReadableDatabase();
        String query = String.format(
                "SELECT %s, %s.%s, %s, %s, %s, %s, %s, %s, %s, %s.%s AS teacher_name, %s.%s AS center_name, "
                        + "%s.%s FROM %s, %s, %s WHERE %s.%s=%s.%s AND %s.%s=%s.%s AND %s.%s=%s",
                DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.DESCRIPTION,
                DBManager.C.Talk.AUDIO_URL,
                DBManager.C.Talk.DURATION_IN_MINUTES,
                DBManager.C.Talk.RECORDING_DATE,
                DBManager.C.Talk.UPDATE_DATE,
                DBManager.C.Talk.RETREAT_ID,
                DBManager.C.Talk.FILE_PATH,
                DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.NAME,
                DBManager.C.Center.TABLE_NAME,
                DBManager.C.Center.NAME,

                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,

                // FROM
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Center.TABLE_NAME,

                // WHERE
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,

                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.VENUE_ID,
                DBManager.C.Center.TABLE_NAME,
                DBManager.C.Center.ID,

                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.ID,
                talkID
        );

        return db.rawQuery(query, null);
    }

    private static void setExtraTeachers(DBManager dbManager, Talk talk) {
        SQLiteDatabase db = dbManager.getReadableDatabase();
        String query = String.format(
                "SELECT %s.%s FROM %s, %s WHERE %s.%s=%s.%s AND %s.%s=%s",
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.NAME,

                // FROM
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.ExtraTalkTeachers.TABLE_NAME,

                // WHERE
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,
                DBManager.C.ExtraTalkTeachers.TABLE_NAME,
                DBManager.C.ExtraTalkTeachers.TEACHER_ID,

                DBManager.C.ExtraTalkTeachers.TABLE_NAME,
                DBManager.C.ExtraTalkTeachers.TALK_ID,
                talk.id
        );

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            talk.getExtraTeacherNames().add(
                    cursor.getString(cursor.getColumnIndexOrThrow(AbstractDBManager.C.Teacher.NAME)).trim()
            );
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public double getDurationInMinutes() {
        return durationInMinutes;
    }

    public String getFormattedDuration() {
        return DateUtils.formatElapsedTime((long)(durationInMinutes*60));
    }

    public String getDate() {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return DateFormat.getDateInstance().format(parser.parse(date));
        } catch (ParseException e) {
            Log.w(LOG_TAG, "Could not parse talk date for talk ID " + id);
            return date;
        }
    }

    public String getTeacherName() {
        return teacherName;
    }

    public ArrayList<String> getExtraTeacherNames() {
        return extraTeacherNames;
    }

    // Return a comma-separated list of all teachers for this talk as a single string, with the primary teacher first
    public String getAllTeachers() {
        String allTeachers = teacherName;
        for (String teacher : extraTeacherNames) {
            allTeachers += ", " + teacher;
        }
        return allTeachers;
    }

    public String getCenterName() {
        return centerName;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public int getId() {
        return id;
    }

    public int getVenueId() {
        return venueId;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public int getRetreatId() {
        return retreatId;
    }

    public Context getContext() {
        return context;
    }

    /**
     * The talk has been downloaded if the path field is populated
     * @return whether the talk has been downloaded
     */
    public boolean isDownloaded() {
        return this.getPath().length() > 0;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
