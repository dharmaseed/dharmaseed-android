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

    private static MockDBManager dbManager;
    private TalkRepository talkRepository;

    @BeforeClass
    public static void setUp() throws Exception
    {
        dbManager = MockDBManager.getInstance(InstrumentationRegistry.getTargetContext());
        dbManager.init(InstrumentationRegistry.getContext());
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
    public void getAllTalks_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        Cursor actualCursor = talkRepository.getTalks(null, null, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        // the getTalks() method should perform an equivalent query
        Cursor expectedCursor = db.rawQuery("SELECT * FROM talks ORDER BY rec_date DESC", null);

        List<String> columns = new ArrayList<>();

        // compare every column
        columns.add(DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TITLE);
        columns.add(DBManager.C.Talk.TEACHER_ID);
        columns.add(DBManager.C.Talk.RECORDING_DATE);
        columns.add(DBManager.C.Talk.VENUE_ID);
        columns.add(DBManager.C.Talk.FILE_PATH);
        columns.add(DBManager.C.Talk.AUDIO_URL);
        columns.add(DBManager.C.Talk.UPDATE_DATE);
        columns.add(DBManager.C.Talk.DESCRIPTION);
        columns.add(DBManager.C.Talk.DURATION_IN_MINUTES);
        columns.add(DBManager.C.Talk.RETREAT_ID);

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
    public void getTalksTwoSearchTerms_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);

        List<String> searchTerms = new ArrayList<>();
        searchTerms.add("Joseph");
        searchTerms.add("Compassion");

        Cursor actual = talkRepository.getTalks(columns, searchTerms, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT t._id, t.teacher_id " +
                     "FROM talks t " +
                     "INNER JOIN teachers te ON te._id = t.teacher_id " +
                     "INNER JOIN centers c ON c._id = t.venue_id " +
                     "WHERE ((t.title LIKE '%Joseph%') OR (t.description LIKE '%Joseph%') " +
                     "OR (te.name LIKE '%Joseph%') OR (c.name LIKE '%Joseph%')) AND " +
                     " ((t.title LIKE '%Compassion%') OR (t.description LIKE '%Compassion%') " +
                     "OR (te.name LIKE '%Compassion%') OR (c.name LIKE '%Compassion%')) " +
                     "ORDER BY rec_date DESC", null);

        // there should only be one result
        assertEquals(1, actual.getCount());
        assertCursors(expected, actual, columns);
    }

    @Test
    public void getTalksOneSearchTerm_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);

        List<String> searchTerms = new ArrayList<>();
        searchTerms.add("Sally");

        Cursor actual = talkRepository.getTalks(columns, searchTerms, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT t._id, t.teacher_id " +
                        "FROM talks t " +
                        "INNER JOIN teachers te ON te._id = t.teacher_id " +
                        "INNER JOIN centers c ON c._id = t.venue_id " +
                        "WHERE (t.title LIKE '%Sally%') OR (t.description LIKE '%Sally%') " +
                        "OR (te.name LIKE '%Sally%') OR (c.name LIKE '%Sally%') " +
                        "ORDER BY rec_date DESC", null);

        assertCursors(expected, actual, columns);
    }

    @Test
    public void selectIdsTeacherIdsStarred_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);
        Cursor actual = talkRepository.getTalks(columns, null, true, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT talks._id, teacher_id " +
                     "FROM talks " +
                    "INNER JOIN talk_stars ON talks._id = talk_stars._id " +
                     "ORDER BY rec_date DESC ",
                null);

        assertCursors(expected, actual, columns);
    }

    @Test
    public void selectIdsTeacherIdsDownloaded_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);
        Cursor actual = talkRepository.getTalks(columns, null, false, true);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT talks._id, teacher_id " +
                        "FROM talks " +
                        "INNER JOIN downloaded_talks dt ON talks._id = dt._id " +
                        "ORDER BY rec_date DESC ",
                null);

        assertCursors(expected, actual, columns);
    }

    @Test
    public void getTalksDownloadedStarred_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        Cursor actual = talkRepository.getTalks(null, null, true, true);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT * " +
                        "FROM talks " +
                        "INNER JOIN downloaded_talks dt ON talks._id = dt._id " +
                        "INNER JOIN talk_stars ts ON talks._id = ts._id " +
                        "ORDER BY rec_date DESC ",
                null);

        List<String> columns = new ArrayList<>();

        // compare every column
        columns.add(DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TITLE);
        columns.add(DBManager.C.Talk.TEACHER_ID);
        columns.add(DBManager.C.Talk.RECORDING_DATE);
        columns.add(DBManager.C.Talk.VENUE_ID);
        columns.add(DBManager.C.Talk.FILE_PATH);
        columns.add(DBManager.C.Talk.AUDIO_URL);
        columns.add(DBManager.C.Talk.UPDATE_DATE);
        columns.add(DBManager.C.Talk.DESCRIPTION);
        columns.add(DBManager.C.Talk.DURATION_IN_MINUTES);
        columns.add(DBManager.C.Talk.RETREAT_ID);

        // only 2 talks are starred and downloaded
        assertEquals(2, actual.getCount());
        assertCursors(expected, actual, columns);
    }

    @Test
    public void getTalksSearchStarred_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);

        List<String> searchTerms = new ArrayList<>();
        searchTerms.add("Joseph");

        Cursor actual = talkRepository.getTalks(columns, searchTerms, true, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT t._id " +
                        "FROM talks t " +
                        "INNER JOIN teachers te ON te._id = t.teacher_id " +
                        "INNER JOIN centers c ON c._id = t.venue_id " +
                        "INNER JOIN talk_stars ts ON t._id = ts._id " +
                        "WHERE ((t.title LIKE '%Joseph%') OR (t.description LIKE '%Joseph%') " +
                        "OR (te.name LIKE '%Joseph%') OR (c.name LIKE '%Joseph%')) " +
                        "ORDER BY rec_date DESC", null);

        // there are two starred talks by joseph
        assertEquals(2, actual.getCount());
        assertCursors(expected, actual, columns);
    }

    @Test
    public void getTalksSearchDownloaded_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);

        List<String> searchTerms = new ArrayList<>();
        searchTerms.add("Metta");

        Cursor actual = talkRepository.getTalks(columns, searchTerms, false, true);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT t._id " +
                        "FROM talks t " +
                        "INNER JOIN teachers te ON te._id = t.teacher_id " +
                        "INNER JOIN centers c ON c._id = t.venue_id " +
                        "INNER JOIN downloaded_talks dt ON t._id = dt._id " +
                        "WHERE ((t.title LIKE '%Metta%') OR (t.description LIKE '%Metta%') " +
                        "OR (te.name LIKE '%Metta%') OR (c.name LIKE '%Metta%')) " +
                        "ORDER BY rec_date DESC", null);

        // there is one downloaded talk with the word "metta" in it
        assertEquals(1, actual.getCount());
        assertCursors(expected, actual, columns);
    }

    @Test
    public void getTalksSearchStarredDownloaded_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);

        List<String> searchTerms = new ArrayList<>();
        searchTerms.add("Mind");

        Cursor actual = talkRepository.getTalks(columns, searchTerms, true, true);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT t._id " +
                        "FROM talks t " +
                        "INNER JOIN teachers te ON te._id = t.teacher_id " +
                        "INNER JOIN centers c ON c._id = t.venue_id " +
                        "INNER JOIN downloaded_talks dt ON t._id = dt._id " +
                        "INNER JOIN talk_stars ts ON t._id = ts._id " +
                        "WHERE ((t.title LIKE '%Mind%') OR (t.description LIKE '%Mind%') " +
                        "OR (te.name LIKE '%Mind%') OR (c.name LIKE '%Mind%')) " +
                        "ORDER BY rec_date DESC", null);

        assertEquals(1, actual.getCount());
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

    @Test
    public void getTalksByTeacher_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);
        int teacherId = 96;

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.RECORDING_DATE);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE);
        columns.add(DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME);
        Cursor actual = talkRepository.getTalksByTeacher(null, teacherId, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT talks._id, talks.title, talks.teacher_id, teachers.name, talks.rec_date " +
                    "FROM talks " +
                    "INNER JOIN teachers ON teachers._id = talks.teacher_id " +
                    "WHERE teachers._id = " + teacherId + " " +
                    "ORDER BY rec_date DESC",
                null);

        assertCursors(expected, actual, columns);
    }

    @Test
    public void getTalksByCenter_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);
        int centerId = 1;

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.RECORDING_DATE);
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE);
        columns.add(DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME);
        Cursor actual = talkRepository.getTalksByCenter(null, centerId, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT talks._id, talks.title, talks.teacher_id, centers.name, talks.rec_date " +
                    "FROM talks " +
                    "INNER JOIN centers ON centers._id = talks.venue_id " +
                    "WHERE centers._id = " + centerId + " " +
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
        assertEquals("Cursor lengths are not equal.", expected.getCount(), actual.getCount());

        while (expected.moveToNext() && actual.moveToNext())
        {
            for (String column : columns)
            {
                assertEquals(
                        "failed on column: " + column,
                        expected.getString(expected.getColumnIndex(column)),
                        actual.getString(actual.getColumnIndex(column))
                );
            }
        }
    }

}