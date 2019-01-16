package org.dharmaseed.androidapp;

import android.database.Cursor;

public class Center
{
    private int id;

    private String website;
    private String description;
    private String name;

    public Center() {}

    public static Center create(Cursor cursor)
    {
        Center center = new Center();

        if (cursor != null && cursor.moveToFirst())
        {
            center.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Center.ID)));
            center.setWebsite(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Center.WEBSITE)));
            center.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Center.DESCRIPTION)));
            center.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Center.NAME)));
        }

        return center;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
