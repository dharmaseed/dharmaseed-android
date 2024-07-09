package org.dharmaseed.android;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateUtils;
import android.util.Log;

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
    protected static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private String title;
    private String description;
    private String audioUrl;
    private String date;
    private String teacherName;
    private ArrayList<String> allTeacherNames;
    private String centerName;

    private int id;
    private int venueId;
    private int teacherId;
    private int retreatId;

    private Context context;

    private double durationInMinutes;

    public Talk() {
        allTeacherNames = new ArrayList<>();
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
            talk.retreatId = cursor.getColumnIndexOrThrow(DBManager.C.Talk.RETREAT_ID);
            talk.durationInMinutes = cursor.getDouble(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DURATION_IN_MINUTES));

            talk.id = talkID;
            talk.context = context;

            talk.setExtraTeachers(dbManager);

        } else {
            Log.e(LOG_TAG, "Could not look up talk, id=" + talkID);
        }

        cursor.close();
        return talk;

    }

    private static Cursor getCursor(DBManager dbManager, int talkID) {

        SQLiteDatabase db = dbManager.getReadableDatabase();
        String query = String.format(
                "SELECT %s, %s.%s, %s, %s, %s, %s, %s, %s, %s.%s AS teacher_name, %s.%s AS center_name, "
                        + "%s.%s FROM %s, %s, %s WHERE %s.%s=%s.%s AND %s.%s=%s.%s AND %s.%s=%s",
                DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.DESCRIPTION,
                DBManager.C.Talk.AUDIO_URL,
                DBManager.C.Talk.DURATION_IN_MINUTES,
                DBManager.C.Talk.RECORDING_DATE,
                DBManager.C.Talk.UPDATE_DATE,
                DBManager.C.Talk.RETREAT_ID,
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

    public void setExtraTeachers(DBManager dbManager) {
        SQLiteDatabase db = dbManager.getReadableDatabase();
        String query = String.format(
                "SELECT %s.%s FROM %s, %s WHERE %s.%s=%s.%s AND %s.%s=%s",
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.NAME,

                // FROM
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.TalkTeachers.TABLE_NAME,

                // WHERE
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,
                DBManager.C.TalkTeachers.TABLE_NAME,
                DBManager.C.TalkTeachers.TEACHER_ID,

                DBManager.C.TalkTeachers.TABLE_NAME,
                DBManager.C.TalkTeachers.TALK_ID,
                id
        );

        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            String teacherName = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.NAME)).trim();
            if (!allTeacherNames.contains(teacherName)) {
                allTeacherNames.add(teacherName);
            }
        }
        cursor.close();
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
            SimpleDateFormat parser = new SimpleDateFormat(DATE_FORMAT);
            return DateFormat.getDateInstance().format(parser.parse(date));
        } catch (ParseException e) {
            Log.w(LOG_TAG, "Could not parse talk date for talk ID " + id);
            return date;
        }
    }

    public String getTeacherName() {
        return teacherName;
    }

    // Return a comma-separated list of all teachers for this talk as a single string, with the primary teacher first
    public String getAllTeacherNames() {
        // Make sure to list the primary teacher first
        String allTeachers = teacherName;
        for (String teacher : allTeacherNames) {
            // The primary teacher will also be listed in allTeacherNames, so skip it here
            // so we don't include it twice
            if (!teacher.equals(teacherName)) {
                allTeachers += ", " + teacher;
            }
        }
        return allTeachers;
    }

    public String getCenterName() {
        return centerName;
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
    public boolean isDownloaded(DBManager dbManager) {
        if (!FileManager.getFile(this).exists())
            return false;

        if (dbManager == null)
            dbManager = DBManager.getInstance(context);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        String query = String.format(
                "SELECT %s.%s FROM %s WHERE %s.%s=%s",
                DBManager.C.DownloadedTalks.TABLE_NAME,
                DBManager.C.DownloadedTalks.ID,

                // FROM
                DBManager.C.DownloadedTalks.TABLE_NAME,

                // WHERE
                DBManager.C.DownloadedTalks.TABLE_NAME,
                DBManager.C.DownloadedTalks.ID,
                id
        );

        Cursor cursor = db.rawQuery(query, null);
        return cursor.getCount() == 1;
    }

    public String getPath() {
        return FileManager.getFile(this).getPath();
    }
}
