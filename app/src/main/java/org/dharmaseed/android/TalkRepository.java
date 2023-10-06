package org.dharmaseed.android;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class TalkRepository extends Repository {

    private static final String LOG_TAG = "TalkRepository";
    private List<String> talkAdapterColumns;

    public TalkRepository(AbstractDBManager dbManager) {
        super(dbManager);

        talkAdapterColumns = new ArrayList<String>();
        talkAdapterColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        talkAdapterColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE);
        talkAdapterColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);
        talkAdapterColumns.add(DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME);
        talkAdapterColumns.add(DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME);
        talkAdapterColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.DURATION_IN_MINUTES);
        talkAdapterColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.RECORDING_DATE);

        removeOldDownloads();
    }

    /**
     * @param searchTerms
     * @param isStarred
     * @param isDownloaded
     * @return talk data for the talk adapter
     */
    public Cursor getTalkAdapterData(
            List<String> searchTerms,
            boolean isStarred,
            boolean isDownloaded
    ) {
        return getTalks(talkAdapterColumns, searchTerms, isStarred, isDownloaded, "");
    }

    /**
     * @param columns      list of fully-qualified column names (i.e. "talks.title") to return from the query
     *                     columns returned in the cursor will be aliased; use DBManager.getAlias to convert
     *                     a fully-qualified column name to its alias
     * @param searchTerms  the term to filter by or null if none
     * @param isStarred    whether the talk is starred
     * @param isDownloaded whether the talk is downloaded
     * @param extraWhere   an extra where clause that can be used to filter on a specific teacher/center id
     * @return every talk in the db filtered by search term, is starred, and is downloaded
     */
    public Cursor getTalks(
            List<String> columns,
            List<String> searchTerms,
            boolean isStarred,
            boolean isDownloaded,
            String extraWhere
    ) {
        if (columns == null || columns.size() == 0) {
            throw new IllegalArgumentException("No columns were specified; at least one is required");
        }

        String queryColumns = "";
        List<String> namedColumns = new ArrayList<String>();

        // Note: we must always have a column called _id in order to use the returned cursor
        // with the CursorAdapter class
        namedColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID + " AS _id");
        for (String column : columns) {
            namedColumns.add(column + " AS " + DBManager.getAlias(column));
        }
        queryColumns = TextUtils.join(", ", namedColumns);

        String query = "SELECT " + queryColumns + " FROM " + DBManager.C.Talk.TABLE_NAME;

        String innerJoin = "";

        boolean teachers = false, centers = false;
        if (queryColumns.contains(DBManager.C.Teacher.TABLE_NAME)) {
            teachers = true;
            innerJoin += joinTalkIdTeacherId();
        }
        if (queryColumns.contains(DBManager.C.Center.TABLE_NAME)) {
            centers = true;
            innerJoin += joinTalkIdVenueId();
        }

        if (isStarred) {
            innerJoin += joinStarredTalks();
        }

        if (isDownloaded) {
            innerJoin += joinDownloadedTalks();
        }

        String where = "";
        if (searchTerms != null && searchTerms.size() > 0) {
            // get the teachers table if we haven't already
            if (!teachers) {
                innerJoin += joinTalkIdTeacherId();
            }

            // get the centers table if we haven't already
            if (!centers) {
                innerJoin += joinTalkIdVenueId();
            }

            String[] selectionColumns = new String[]
                    {
                            DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE,
                            DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.DESCRIPTION,
                            DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME,
                            DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME
                    };

            where += getSearchStatement(searchTerms, selectionColumns);
        }

        if (!innerJoin.isEmpty()) {
            query += innerJoin;
        }

        List<String> whereClauses = new ArrayList<String>();
        if (!where.isEmpty()) {
            whereClauses.add(where);
        }
        if (!extraWhere.isEmpty()) {
            whereClauses.add(extraWhere);
        }
        String fullWhere = TextUtils.join(" AND ", whereClauses);
        if (!fullWhere.isEmpty()) {
            query += " WHERE " + fullWhere;
        }

        query += " ORDER BY "
                + DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.RECORDING_DATE
                + " DESC"
        ;
        Log.d(LOG_TAG, query);
        return queryIfNotNull(query, null);
    }

    public Cursor getTalks(
            List<String> columns,
            List<String> searchTerms,
            boolean isStarred,
            boolean isDownloaded
    ) {
        return getTalks(columns, searchTerms, isStarred, isDownloaded, "");
    }

    /**
     * @param searchTerms
     * @param teacherId    the teacher id
     * @param isStarred
     * @param isDownloaded
     * @return all talks with by a teacher
     */
    public Cursor getTalksByTeacher(
            List<String> searchTerms,
            long teacherId,
            boolean isStarred,
            boolean isDownloaded
    ) {
        return getTalks(talkAdapterColumns, searchTerms, isStarred, isDownloaded,
                DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.ID + "=" + teacherId);
    }

    /**
     * @param searchTerms
     * @param venueId
     * @param isStarred
     * @param isDownloaded
     * @return talks filtered by venue id, search terms, stars and downloads
     */
    public Cursor getTalksByCenter(
            List<String> searchTerms,
            long venueId,
            boolean isStarred,
            boolean isDownloaded
    ) {
        return getTalks(talkAdapterColumns, searchTerms, isStarred, isDownloaded,
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.ID + "=" + venueId);
    }

    /**
     * Check if we have talks downloaded in old/inaccessible locations, and mark them as not downloaded if so
     *
     */
    public void removeOldDownloads()
    {
        File downloadsDir = TalkManager.getDir(dbManager.getContext());
        Log.i(LOG_TAG, "HI " + downloadsDir.toString());

        ArrayList<String> columns = new ArrayList<String>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.FILE_PATH);

        Cursor downloaded = getTalks(columns, null, false, true);

        while (downloaded.moveToNext())
        {
            int id = downloaded.getInt(downloaded.getColumnIndexOrThrow(DBManager.getAlias(
                    DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID)));
            File file = new File(downloaded.getString(downloaded.getColumnIndexOrThrow(DBManager.getAlias(
                    DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.FILE_PATH))).trim());

            String directory = file.getAbsoluteFile().getParent();
            String downloadDirectory = TalkManager.getDir(dbManager.getContext()).getAbsolutePath();

            if (directory != null && ! directory.equals(downloadDirectory)) {
                Log.w(LOG_TAG, "Detected old style download for talk " + file.toString());
                dbManager.removeDownload(id);

                try {
                    File copyTarget = new File(downloadsDir.toString() + "/" + file.getName());
                    copy(file, copyTarget);
                    dbManager.addDownload(id, copyTarget.toString());
                    Log.i(LOG_TAG, "Successfully moved old talk to " + copyTarget.toString());
                    file.delete();
                } catch (IOException e) {
                    Log.e(LOG_TAG, e.toString());
                }
            }
        }

        downloaded.close();
    }

    /**
     * Copy src to dst.  From https://stackoverflow.com/questions/9292954/how-to-make-a-copy-of-a-file-in-android
     * Unfortunately, Files.copy is not available before API version 26
     * @param src
     * @param dst
     * @throws IOException
     */
    private static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private String joinStarredTalks() {
        return innerJoin(
                DBManager.C.TalkStars.TABLE_NAME,
                DBManager.C.TalkStars.TABLE_NAME + "." + DBManager.C.TalkStars.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID
        );
    }

    private String joinDownloadedTalks() {
        return innerJoin(
                DBManager.C.DownloadedTalks.TABLE_NAME,
                DBManager.C.DownloadedTalks.TABLE_NAME + "." + DBManager.C.DownloadedTalks.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID
        );
    }

    private String joinTalkIdTeacherId() {
        return innerJoin(
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID
        );
    }

    private String joinTalkIdVenueId() {
        return innerJoin(
                DBManager.C.Center.TABLE_NAME,
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.VENUE_ID
        );
    }

}
