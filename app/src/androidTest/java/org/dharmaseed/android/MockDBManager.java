package org.dharmaseed.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.InputStream;

public class MockDBManager extends SQLiteOpenHelper
{

    public static MockDBManager instance = null;
    private boolean initialized;

    private MockDBManager(Context context)
    {
        // name is null because we want an in-memory DB
        super(context, null, null, 1);
        initialized = false;
    }

    public static synchronized MockDBManager getInstance(Context context)
    {
        if (instance == null) {
            instance = new MockDBManager(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
    }

    public void init(Context context) throws Exception
    {
        if (!initialized)
        {
            SQLiteDatabase db = getWritableDatabase();
            InputStream fis = context.getAssets().open("inserts.txt");
            String line = "";
            int ch;
            // run every statement from "inserts.txt"
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
            initialized = true;
        }
    }
}
