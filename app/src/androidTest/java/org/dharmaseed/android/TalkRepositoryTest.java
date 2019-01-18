package org.dharmaseed.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.test.InstrumentationRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TalkRepositoryTest
{

    private static SQLiteOpenHelper dbManager;
    private TalkRepository talkRepository;

    @BeforeClass
    public static void setUp() throws Exception
    {
        createDB();
    }

    private static void createDB() throws Exception
    {
        dbManager = new MockDBManager(InstrumentationRegistry.getTargetContext());
        SQLiteDatabase db = dbManager.getWritableDatabase();
        InputStream fis = InstrumentationRegistry.getContext().getAssets().open("inserts.txt");
        String line = "";
        int ch;
        while ((ch = fis.read()) != -1)
        {
            if (ch < 8 || ch > 'z') continue;
            line += (char) ch;
            // execute the statement once we hit a semicolon followed by a newline
            if (ch == ';' && fis.read() == '\n')
            {
                db.execSQL(line);
                line = "";
            }
        }
    }

    @AfterClass
    public static void tearDown() throws Exception
    {
        dbManager.close();
    }

    @Test
    public void selectIds_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        Cursor actualCursor = talkRepository.getTalks(columns, null, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        // the getTalks() method should perform an equivalent query
        Cursor expectedCursor = db.rawQuery("SELECT _id FROM talks ORDER BY rec_date DESC", null);

        assertCursors(expectedCursor, actualCursor, columns);
    }

    @Test
    public void selectIdsTeacherIds_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);
        Cursor actual = talkRepository.getTalks(columns, null, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery("SELECT _id, teacher_id FROM talks ORDER BY rec_date DESC", null);

        assertCursors(expected, actual, columns);
    }

    @Test
    public void selectIdsTeacherNames_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);
        columns.add(DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME);
        Cursor actual = talkRepository.getTalks(columns, null, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT talks._id, talks.teacher_id, teachers.name " +
                    "FROM talks " +
                    "INNER JOIN teachers ON teachers._id = talks.teacher_id " +
                    "ORDER BY rec_date DESC",
                null);

        assertCursors(expected, actual, columns);
    }

    /**
     * Verifies that every provided column is the same in both cursors
     * @param expected the expected result
     * @param actual the actual result
     * @param columns the columns to compare
     */
    private void assertCursors(Cursor expected, Cursor actual, List<String> columns)
    {
        assertEquals(expected.getCount(), actual.getCount());

        while (expected.moveToNext() && actual.moveToNext())
        {
            for (String column : columns)
            {
                assertEquals(
                        expected.getString(expected.getColumnIndex(column)),
                        actual.getString(actual.getColumnIndex(column))
                );
            }
        }
    }

}