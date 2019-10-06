package org.dharmaseed.android;

import android.database.Cursor;

/**
 * Model of the TALK table in the DB
 *
 * @author jakewilson
 */
public class Talk {

    private String title;
    private String description;
    private String audioUrl;
    private String date;
    private String teacherName;
    private String centerName;
    private String photoFileName;
    private String path; // where the talk is downloaded, if it is

    private int id;
    private int venueId;
    private int teacherId;
    private int retreatId;

    private double durationInMinutes;

    public Talk(Cursor cursor) {
        this.create(cursor);
    }

    /**
     * Create the object by setting all model values from the cursor
     * @param cursor the result from the DB
     */
    private void create(Cursor cursor) {
        setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TITLE)).trim());

        setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DESCRIPTION)).trim());

        String url = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.AUDIO_URL));
        setAudioUrl("http://www.dharmaseed.org" + url);

        String recDate = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.RECORDING_DATE));
        if(recDate == null) {
            recDate = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.UPDATE_DATE));
        }
        setDate(recDate);

        setTeacherId(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TEACHER_ID)));

        setTeacherName(cursor.getString(cursor.getColumnIndexOrThrow("teacher_name")).trim());

        setCenterName(cursor.getString(cursor.getColumnIndexOrThrow("center_name")).trim());

        setPhotoFileName(DBManager.getTeacherPhotoFilename(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.ID))));

        setRetreatId(cursor.getColumnIndexOrThrow(DBManager.C.Talk.RETREAT_ID));

        setDurationInMinutes(cursor.getDouble(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DURATION_IN_MINUTES)));

        setPath(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.FILE_PATH)));
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public double getDurationInMinutes() {
        return durationInMinutes;
    }

    public String getDate() {
        return date;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getCenterName() {
        return centerName;
    }

    public String getPhotoFileName() {
        return photoFileName;
    }

    public int getId() {
        return id;
    }

    public int getVenueId() {
        return venueId;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public int getRetreatId() {
        return retreatId;
    }

    /**
     * The talk has been downloaded if the path field is populated
     * @return whether the talk has been downloaded
     */
    public boolean isDownloaded() {
        return this.getPath().length() > 0;
    }

    public String getPath() {
        return path;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    private void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    private void setDurationInMinutes(double durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    private void setDate(String date) {
        this.date = date;
    }

    private void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    private void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    private void setPhotoFileName(String photoFileName) {
        this.photoFileName = photoFileName;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setId(int id) {
        this.id = id;
    }

    private void setVenueId(int venueId) {
        this.venueId = venueId;
    }

    private void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    private void setRetreatId(int retreatId) {
        this.retreatId = retreatId;
    }
}
