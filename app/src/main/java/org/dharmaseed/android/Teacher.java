package org.dharmaseed.android;

import android.database.Cursor;

public class Teacher
{
    private int id = -1;

    private String website;
    private String donationUrl;
    private String bio;
    private String name;
    private String photo;

    private boolean isPublic;
    private boolean isMonastic;

    public Teacher()
    {

    }

    /**
     * Creates a teacher from a cursor object
     * @param cursor
     * @return a new teacher
     */
    public static Teacher create(Cursor cursor)
    {
        Teacher teacher = new Teacher();

        if (cursor.moveToFirst())
        {
            teacher.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.ID)));
            teacher.setWebsite(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.WEBSITE)));
            teacher.setDonationUrl(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.DONATION_URL)));
            teacher.setBio(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.BIO)));
            teacher.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.NAME)));
            teacher.setPhoto(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.PHOTO)));
            teacher.setPublic(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.PUBLIC)).equals("true"));
            teacher.setMonastic(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.MONASTIC)).equals("true"));
        }

        return teacher;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDonationUrl() {
        return donationUrl;
    }

    public void setDonationUrl(String donationUrl) {
        this.donationUrl = donationUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public boolean isMonastic() {
        return isMonastic;
    }

    public void setMonastic(boolean monastic) {
        isMonastic = monastic;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
