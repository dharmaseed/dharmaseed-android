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
    public Cursor getCenters(List<String> searchTerms, boolean isStarred)
    {
        String query = "SELECT centers._id, centers.name FROM centers ";

        if (isStarred)
        {
            query += joinStarredCenters();
        }

        String where = "";
        if (searchTerms != null && !searchTerms.isEmpty())
        {
            String[] selectionColumns = new String[]
            {
                DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME,
            };

            where += " WHERE " + getSearchStatement(searchTerms, selectionColumns);
        }

        query += where + " ORDER BY centers.name ASC";

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
