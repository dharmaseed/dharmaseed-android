package org.dharmaseed.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MockDBManager extends SQLiteOpenHelper
{

    public MockDBManager(Context context)
    {
        // name is null because we want an in-memory DB
        super(context, null, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
    }

}
