package org.dharmaseed.android;

import android.database.Cursor;

import java.util.List;

public class CenterRepository extends Repository {

    public CenterRepository(DBManager dbManager) {
        super(dbManager);
    }

    public Cursor getCenterById(long id)
    {
        String query = String.format(
            "SELECT * FROM %s WHERE %s = %s",
            DBManager.C.Center.TABLE_NAME,
            DBManager.C.Center.ID,
            id
        );
        return queryIfNotNull(query, null);
    }

    /**
     *
     * @param searchTerms
     * @param isStarred
     * @return a list of centers filtered by search terms and stars
     */
    public Cursor getCenters(
            List<String> searchTerms,
            boolean isStarred, boolean isDownloaded, boolean inHistory)
    {
        String query = "SELECT centers._id, centers.name, count(talks._id) AS talk_count FROM centers ";
        String innerJoin = innerJoin(
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.VENUE_ID,
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.ID
        );

        if (isStarred)
            innerJoin += joinStarredCenters();

        if (isDownloaded)
            innerJoin += joinDownloadedTalks();

        if (inHistory)
            innerJoin += joinTalkHistory();

        query += innerJoin;

        String where = " WHERE centers.has_venue_view = 'true' ";
        if (searchTerms != null && !searchTerms.isEmpty())
        {
            String[] selectionColumns = new String[]
            {
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME,
            };

            where += " AND " + getSearchStatement(searchTerms, selectionColumns);
        }

        query += where + "GROUP BY " +
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.ID +
                " ORDER BY ";
        if (inHistory)
            query += DBManager.C.TalkHistory.TABLE_NAME + "." + DBManager.C.TalkHistory.DATE_TIME + " DESC";
        else
            query += DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME + " ASC";


        return queryIfNotNull(query, null);
    }

    private String joinStarredCenters()
    {
        return innerJoin(
                DBManager.C.CenterStars.TABLE_NAME,
                DBManager.C.CenterStars.TABLE_NAME + "." + DBManager.C.CenterStars.ID,
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.ID
        );
    }
}
