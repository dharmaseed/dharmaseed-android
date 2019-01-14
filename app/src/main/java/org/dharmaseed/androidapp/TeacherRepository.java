package org.dharmaseed.androidapp;

import android.database.Cursor;

public class TeacherRepository extends Repository
{

    private DBManager dbManager;

    private static final String LOG_TAG = "TeacherRepository";

    public TeacherRepository(DBManager dbManager)
    {
        super(dbManager);
    }

    /**
     * @param id the id to search for
     * @return all teachers with <id>
     */
    public Cursor getTeacherById(int id)
    {
        String query = String.format(
                "SELECT * FROM %s WHERE %s = %s",
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,
                id
        );
        return queryIfNotNull(query, null);
    }

}
