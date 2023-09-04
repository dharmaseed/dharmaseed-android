package org.dharmaseed.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class AbstractDBManager extends SQLiteOpenHelper {
    protected Context context;

    public AbstractDBManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public Context getContext() { return context; }

    /**
     * Updates the Talk table to set the "file_path" column to the talk path
     * also add the Talk ID to the downloaded_talks table
     *
     * @param talkId   the talk ID
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

    /**
     * Updates the Talk table to set the "file_path" column to the empty string and removes
     * the talk id from the downloaded_talks table
     *
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

    // Database contract class
    final class C {
        public C() {
        }

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
            public static final String FILE_PATH = "file_path";

            public static final String TABLE_NAME = "talks";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
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
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
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
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
                    + WEBSITE + " TEXT,"
                    + DONATION_URL + " TEXT,"
                    + BIO + " TEXT,"
                    + NAME + " TEXT,"
                    + PUBLIC + " INTEGER,"
                    + MONASTIC + " INTEGER,"
                    + PHOTO + " TEXT)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

        }

        public abstract class Center {
            public static final String WEBSITE = "website";
            public static final String DESCRIPTION = "description";
            public static final String ID = "_id";
            public static final String NAME = "name";
            public static final String HAS_VENUE_VIEW = "has_venue_view";

            public static final String TABLE_NAME = "centers";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
                    + WEBSITE + " TEXT,"
                    + DESCRIPTION + " TEXT,"
                    + NAME + " TEXT,"
                    + HAS_VENUE_VIEW + " INT"
                    + ")";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

        }

        public abstract class Retreat {
            public static final String ID = "_id";
            public static final String NAME = "name";
            public static final String START_DATE = "start_date";

            public static final String TABLE_NAME = "retreats";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                    ID + " INTEGER PRIMARY KEY," +
                    NAME + " TEXT," +
                    START_DATE + " TEXT" +
                    ")";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

        }

        public abstract class TalkStars {
            public static final String ID = "_id";

            public static final String TABLE_NAME = "talk_stars";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        }

        public abstract class TalkHistory {
            public static final String ID = "_id";
            public static final String PROGRESS_IN_MINUTES = "progress_in_minutes";
            public static final String DATE_TIME = "date_time";
            public static final String TABLE_NAME = "talk_history";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY,"
                    + DATE_TIME + " TEXT,"
                    + PROGRESS_IN_MINUTES + " REAL"
                    + ")";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        }


        public abstract class TeacherStars {
            public static final String ID = "_id";

            public static final String TABLE_NAME = "teacher_stars";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        }

        public abstract class CenterStars {
            public static final String ID = "_id";

            public static final String TABLE_NAME = "center_stars";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        }

        public abstract class DownloadedTalks {
            public static final String ID = "_id";

            public static final String TABLE_NAME = "downloaded_talks";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        }

        public abstract class Edition {
            public static final String TABLE = "_table";
            public static final String EDITION = "edition";

            public static final String TABLE_NAME = "editions";
            public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                    + TABLE + " TEXT PRIMARY KEY," + EDITION + " TEXT)";
            public static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

            public static final String LAST_SYNC = "last_sync";
        }

    }
}
