package org.dharmaseed.androidapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TalkRepository {

    private DBManager dbManager;

    private static final String LOG_TAG = "TalkRepository";

    public TalkRepository(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * @return every talk in the DB
     */
    public Cursor getTalks() {
        return null;
    }

    /**
     * @param searchTerms the term to filter the results by
     * @return every talk in the DB filtered by a search term
     */
    public Cursor getTalks(List<String> searchTerms) {
        return null;
    }

    /**
     * @param searchTerms the term to filter by
     * @param isStarred whether the talk is starred or not
     * @return every talk in the db filtered by search term and is starred
     */
    public Cursor getTalks(List<String> searchTerms, boolean isStarred) {
        return null;
    }

    /**
     *
     * @param searchTerms
     * @param isStarred
     * @param isDownloaded
     * @return talk data for the talk adapter
     */
    public Cursor getTalkAdapterData(
            List<String> searchTerms,
            boolean isStarred,
            boolean isDownloaded
    )
    {
        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME    + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME    + "." + DBManager.C.Talk.TITLE);
        columns.add(DBManager.C.Talk.TABLE_NAME    + "." + DBManager.C.Talk.TEACHER_ID);
        columns.add(DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME);
        return getTalks(columns, searchTerms, isStarred, isDownloaded);
    }

    /**
     * @param columns the columns to select or null if all
     * @param searchTerms the term to filter by or null if none
     * @param isStarred whether the talk is starred
     * @param isDownloaded whether the talk is downloaded
     * @return every talk in the db filtered by search term, is starred, and is downloaded
     */
    public Cursor getTalks(
           List<String> columns,
           List<String> searchTerms,
           boolean isStarred,
           boolean isDownloaded
    )
    {
        String queryColumns = "";
        if (columns == null || columns.size() == 0)
        {
            queryColumns = "*";
        }
        else
        {
            queryColumns = TextUtils.join(", ", columns);
        }

        String query = "SELECT " + queryColumns + " FROM " + DBManager.C.Talk.TABLE_NAME;

        String innerJoin = innerJoin(
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID
        );

        if (isStarred)
        {
            innerJoin += innerJoin(
                    DBManager.C.TalkStars.TABLE_NAME,
                    DBManager.C.TalkStars.TABLE_NAME + "." + DBManager.C.TalkStars.ID,
                    DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID
            );
        }

        if (isDownloaded)
        {
            innerJoin += innerJoin(
                    DBManager.C.DownloadedTalks.TABLE_NAME,
                    DBManager.C.DownloadedTalks.TABLE_NAME + "." + DBManager.C.DownloadedTalks.ID,
                    DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID
            );
        }

        String where = "";
        if (searchTerms != null && searchTerms.size() > 0)
        {
            // we only need the center table if we have search terms
            innerJoin += innerJoin(
                    DBManager.C.Center.TABLE_NAME,
                    DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.ID,
                    DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.VENUE_ID
            );

            List<String> subqueries = new ArrayList<>();

            String[] selectionColumns = new String[]
            {
                    DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE,
                    DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.DESCRIPTION,
                    DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME,
                    DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME
            };

            // for each term, generate a series of LIKE statements for each selection column
            for (String term : searchTerms)
            {
                List<String> subquery = new ArrayList<>(selectionColumns.length);
                for (String col : selectionColumns)
                {
                    subquery.add(col + " LIKE '%" + term + "%'");
                }
                subqueries.add("(" + TextUtils.join(" OR ", subquery) + ")");
            }

            where += TextUtils.join(" AND ", subqueries) + " ";
        }

        if (!innerJoin.isEmpty())
        {
            query += innerJoin;
        }

        if (!where.isEmpty())
        {
            query += " WHERE " + where;
        }

        query += " ORDER BY "
                + DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.RECORDING_DATE
                + " DESC"
                ;
        Log.d(LOG_TAG, query);
        return queryIfNotNull(query, null);
    }

    /**
     * Returns an INNER JOIN statement
     * @param table
     * @param firstId should be in the form <table_name>._id
     * @param secondId should be in the form <table_name>._id
     * @return
     */
    private String innerJoin(String table, String firstId, String secondId)
    {
        return String.format(
                " INNER JOIN %s ON %s = %s ",
                table, firstId, secondId
        );
    }

    /**
     * @param teacherId the teacher id
     * @return all talks with by a teacher
     */
    public Cursor getTalksByTeacher(int teacherId) {
        String query =
                "SELECT * FROM " + DBManager.C.Talk.TABLE_NAME
                + " WHERE " + DBManager.C.Talk.TEACHER_ID + " = " + teacherId
                ;

        return queryIfNotNull(query, null);
    }

    /**
     * Runs a query after checking if the DB is not null
     * @param query
     * @param selectionArgs
     * @return
     */
    private Cursor queryIfNotNull(String query, String[] selectionArgs) {
        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, selectionArgs);
        }
        return cursor;
    }

}
