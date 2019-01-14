package org.dharmaseed.androidapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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

}
