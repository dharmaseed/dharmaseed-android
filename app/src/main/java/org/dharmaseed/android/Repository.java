package org.dharmaseed.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class Repository
{

    protected DBManager dbManager;

    public Repository(DBManager dbManager)
    {
        this.dbManager = dbManager;
    }

    /**
     * Runs a query after checking if the DB is not null
     * @param query
     * @param selectionArgs
     * @return
     */
    protected Cursor queryIfNotNull(String query, String[] selectionArgs)
    {
        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor cursor = null;
        if (db != null)
        {
            cursor = db.rawQuery(query, selectionArgs);
        }
        return cursor;
    }

    /**
     * Returns the search terms LIKE statements, for example if searchTerms is "joseph"
     * it returns:
     * (selectionColumns[0] LIKE '%joseph%' OR selectionColumns[1] LIKE '%joseph%' OR ...)
     * @param searchTerms
     * @return
     */
    protected String getSearchStatement(List<String> searchTerms, String[] selectionColumns)
    {
        List<String> subqueries = new ArrayList<>();

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

        return TextUtils.join(" AND ", subqueries) + " ";
    }

    /**
     * Returns an INNER JOIN statement
     * @param table
     * @param firstId should be in the form <table_name>._id
     * @param secondId should be in the form <table_name>._id
     * @return
     */
    protected String innerJoin(String table, String firstId, String secondId)
    {
        return String.format(
                " INNER JOIN %s ON %s = %s ",
                table, firstId, secondId
        );
    }

}
