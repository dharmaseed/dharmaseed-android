package org.dharmaseed.android;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TalkRepository extends Repository
{

    private static final String LOG_TAG = "TalkRepository";
    private DBManager dbManager;

    public TalkRepository(DBManager dbManager)
    {
        super(dbManager);
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
    private Cursor getTalks(
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

        String innerJoin = joinTalkIdTeacherId();

        if (isStarred)
        {
            innerJoin += joinStarredTalks();
        }

        if (isDownloaded)
        {
            innerJoin += joinDownloadedTalks();
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

            String[] selectionColumns = new String[]
            {
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.DESCRIPTION,
                DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME,
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME
            };

            where += getSearchStatement(searchTerms, selectionColumns);
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
     * @param searchTerms
     * @param teacherId the teacher id
     * @param isStarred
     * @param isDownloaded
     * @return all talks with by a teacher
     */
    public Cursor getTalksByTeacher(
            List<String> searchTerms,
            long teacherId,
            boolean isStarred,
            boolean isDownloaded
    )
    {
        String query = "SELECT talks._id, talks.title, talks.teacher_id, teachers.name, talks.rec_date FROM talks";

        if (isStarred)
        {
            query += joinStarredTalks();
        }

        if (isDownloaded)
        {
            query += joinDownloadedTalks();
        }

        query += joinTalkIdTeacherId();

        String where = " WHERE ";

        if (searchTerms != null && !searchTerms.isEmpty())
        {
            String[] selectionColumns = new String[]
            {
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.DESCRIPTION,
            };

            where += getSearchStatement(searchTerms, selectionColumns) + " AND ";
        }

        where += String.format(
                " %s.%s = %s ",
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TEACHER_ID,
                teacherId
        );

        query += where + " ORDER BY talks.rec_date DESC ";

        return queryIfNotNull(query, null);
    }

    /**
     *
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
    )
    {
        String query = "SELECT talks._id, talks.title, talks.teacher_id, centers.name, talks.rec_date FROM talks";

        if (isStarred)
        {
            query += joinStarredTalks();
        }

        if (isDownloaded)
        {
            query += joinDownloadedTalks();
        }

        query += joinTalkIdVenueId();

        String where = " WHERE ";

        if (searchTerms != null && !searchTerms.isEmpty())
        {
            String[] selectionColumns = new String[]
            {
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.DESCRIPTION,
            };

            where += getSearchStatement(searchTerms, selectionColumns) + " AND ";
        }

        where += String.format(
                " %s.%s = %s ",
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.VENUE_ID,
                venueId
        );

        query += where + " ORDER BY talks.rec_date DESC ";

        return queryIfNotNull(query, null);
    }


    private String joinStarredTalks()
    {
        return innerJoin(
                DBManager.C.TalkStars.TABLE_NAME,
                DBManager.C.TalkStars.TABLE_NAME + "." + DBManager.C.TalkStars.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID
        );
    }

    private String joinDownloadedTalks()
    {
        return innerJoin(
                DBManager.C.DownloadedTalks.TABLE_NAME,
                DBManager.C.DownloadedTalks.TABLE_NAME + "." + DBManager.C.DownloadedTalks.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID
        );
    }

    private String joinTalkIdTeacherId()
    {
        return innerJoin(
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID
        );
    }

    private String joinTalkIdVenueId()
    {
        return innerJoin(
                DBManager.C.Center.TABLE_NAME,
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.ID,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.VENUE_ID
        );
    }

}
