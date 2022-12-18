package org.dharmaseed.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TalkRepositoryTest
{

    private static MockDBManager dbManager;
    private TalkRepository talkRepository;
    private static List<String> allTalkColumns;

    @BeforeClass
    public static void setUp() throws Exception
    {
        dbManager = MockDBManager.getInstance(InstrumentationRegistry.getTargetContext());
        dbManager.init(InstrumentationRegistry.getContext());

        allTalkColumns = new ArrayList<>();
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TITLE);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.RECORDING_DATE);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.VENUE_ID);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.FILE_PATH);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.AUDIO_URL);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.UPDATE_DATE);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.DESCRIPTION);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.DURATION_IN_MINUTES);
        allTalkColumns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.RETREAT_ID);

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
        Cursor expectedCursor = db.rawQuery("SELECT _id, _id AS talks__id FROM talks ORDER BY rec_date DESC", null);

        // Make sure that the cursor always contains an un-aliased _id field for compatibility with CursorAdapter
        columns.add("_id");

        assertCursors(expectedCursor, actualCursor, columns);
    }

    @Test
    public void getAllTalks_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);

        Cursor actualCursor = talkRepository.getTalks(allTalkColumns, null, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        // the getTalks() method should perform an equivalent query
        Cursor expectedCursor = db.rawQuery("SELECT talks._id AS talks__id, talks.title AS " +
                "talks_title, talks.teacher_id AS talks_teacher_id, talks.rec_date AS talks_rec_date, talks.venue_id " +
                "AS talks_venue_id, talks.file_path AS talks_file_path, talks.audio_url AS talks_audio_url, talks" +
                ".update_date AS talks_update_date, talks.description AS talks_description, talks.duration_in_minutes" +
                " AS talks_duration_in_minutes, talks.retreat_id AS talks_retreat_id FROM talks ORDER BY talks" +
                ".rec_date DESC", null);

        assertCursors(expectedCursor, actualCursor, allTalkColumns);
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
        Cursor expected = db.rawQuery("SELECT _id AS talks__id, teacher_id AS talks_teacher_id FROM talks ORDER BY rec_date DESC", null);

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
                "SELECT t._id AS talks__id, t.teacher_id AS talks_teacher_id " +
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
                "SELECT t._id AS talks__id, t.teacher_id AS talks_teacher_id " +
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
                "SELECT talks._id AS talks__id, teacher_id AS talks_teacher_id " +
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
                "SELECT talks._id AS talks__id, teacher_id AS talks_teacher_id " +
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

        Cursor actual = talkRepository.getTalks(allTalkColumns, null, true, true);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT talks._id AS talks__id, talks.title AS talks_title, talks.teacher_id AS talks_teacher_id, " +
                        "talks.rec_date AS talks_rec_date, talks.venue_id AS talks_venue_id, talks.file_path AS " +
                        "talks_file_path, talks.audio_url AS talks_audio_url, talks.update_date AS talks_update_date," +
                        " talks.description AS talks_description, talks.duration_in_minutes AS " +
                        "talks_duration_in_minutes, talks.retreat_id AS talks_retreat_id " +
                        "FROM talks " +
                        "INNER JOIN downloaded_talks dt ON talks._id = dt._id " +
                        "INNER JOIN talk_stars ts ON talks._id = ts._id " +
                        "ORDER BY rec_date DESC ",
                null);

        // only 2 talks are starred and downloaded
        assertEquals(2, actual.getCount());
        assertCursors(expected, actual, allTalkColumns);
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
                "SELECT t._id AS talks__id " +
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
                "SELECT t._id AS talks__id " +
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
                "SELECT t._id AS talks__id " +
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
                "SELECT talks._id AS talks__id, talks.teacher_id AS talks_teacher_id, teachers.name AS teachers_name " +
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
                "SELECT talks._id AS talks__id, talks.title AS talks_title, talks.teacher_id AS talks_teacher_id, teachers.name AS teachers_name, talks.rec_date AS talks_rec_date " +
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
                "SELECT talks._id AS talks__id, talks.title AS talks_title, talks.teacher_id AS talks_teacher_id, centers.name AS centers_name, talks.rec_date AS talks_rec_date " +
                    "FROM talks " +
                    "INNER JOIN centers ON centers._id = talks.venue_id " +
                    "WHERE centers._id = " + centerId + " " +
                    "ORDER BY rec_date DESC",
                null);

        assertCursors(expected, actual, columns);
    }

    @Test
    public void getTalksWithTeacherNameAndCenterName_isCorrect()
    {
        talkRepository = new TalkRepository(dbManager);
        int centerId = 1;

        List<String> columns = new ArrayList<>();
        columns.add(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.ID);
        columns.add(DBManager.C.Teacher.TABLE_NAME + "." + DBManager.C.Teacher.NAME);
        columns.add(DBManager.C.Center.TABLE_NAME + "." + DBManager.C.Center.NAME);
        Cursor actual = talkRepository.getTalks(columns, null, false, false);

        SQLiteDatabase db = dbManager.getReadableDatabase();
        Cursor expected = db.rawQuery(
                "SELECT talks._id AS _id, talks._id AS talks__id, teachers.name AS teachers_name, centers.name AS " +
                        "centers_name FROM talks INNER JOIN teachers ON teachers._id = talks.teacher_id  INNER JOIN " +
                        "centers ON centers._id = talks.venue_id  ORDER BY talks.rec_date DESC",
                null);

        assertCursors(expected, actual, columns);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTalksWithNoColumns_raisesException() {
        talkRepository = new TalkRepository(dbManager);
        talkRepository.getTalks(null, null, false, false);
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
                String alias = DBManager.getAlias(column);
                assertEquals(
                        "failed on column: " + alias,
                        expected.getString(expected.getColumnIndexOrThrow(alias)),
                        actual.getString(actual.getColumnIndexOrThrow(alias))
                );
            }
        }
    }

}