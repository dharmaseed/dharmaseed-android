package org.dharmaseed.androidapp;

import android.database.Cursor;

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
}
