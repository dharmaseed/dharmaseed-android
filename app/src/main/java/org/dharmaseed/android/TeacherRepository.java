package org.dharmaseed.android;

import android.database.Cursor;
import android.util.Log;

import java.util.List;

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
    public Cursor getTeacherById(long id)
    {
        String query = String.format(
                "SELECT * FROM %s WHERE %s = %s",
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,
                id
        );
        return queryIfNotNull(query, null);
    }

    public Cursor getTeachers(List<String> searchTerms, boolean isStarred)
    {
        String query = "SELECT teachers._id, teachers.name, count(talks._id) AS talk_count FROM teachers ";
        String innerJoin = innerJoin(
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.ID
        );

        if (isStarred)
        {
            innerJoin += joinStarredTeachers();
        }

        String where = " WHERE teachers.public = 'true' ";
        if (searchTerms != null && !searchTerms.isEmpty())
        {
            String[] selectionColumns = new String[]
            {
                    DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME,
            };

            where += " AND " + getSearchStatement(searchTerms, selectionColumns);
        }

        if (!innerJoin.isEmpty())
        {
            query += innerJoin;
        }

        query += where;

        query += " GROUP BY teachers._id";
        query += " ORDER BY teachers.monastic DESC, teachers.name ASC";

        Log.d(LOG_TAG, query);
        return queryIfNotNull(query, null);
    }

    private String joinStarredTeachers()
    {
        return innerJoin(
                DBManager.C.TeacherStars.TABLE_NAME,
                DBManager.C.TeacherStars.TABLE_NAME + "." + DBManager.C.TeacherStars.ID,
                DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.ID
        );
    }

}
