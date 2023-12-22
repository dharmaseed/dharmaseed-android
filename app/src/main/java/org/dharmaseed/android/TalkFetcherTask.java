/*
 *     Dharmaseed Android app
 *     Copyright (C) 2016  Brett Bethke
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.dharmaseed.android;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by bbethke on 2/19/16.
 */
public class TalkFetcherTask extends DataFetcherTask {

    private static final String LOG_TAG = "TalkFetcherTask";

    public TalkFetcherTask(DBManager dbManager, NavigationActivity navigationActivity) {
        super(dbManager, navigationActivity);
    }

    @Override
    protected Void doInBackground(Void... params) {

        updateTable(DBManager.C.Talk.TABLE_NAME, DBManager.C.Talk.ID,
                "talks/",
                new String[]{
                        DBManager.C.Talk.TITLE,
                        DBManager.C.Talk.DESCRIPTION,
                        DBManager.C.Talk.VENUE_ID,
                        DBManager.C.Talk.TEACHER_ID,
                        DBManager.C.Talk.AUDIO_URL,
                        DBManager.C.Talk.DURATION_IN_MINUTES,
                        DBManager.C.Talk.UPDATE_DATE,
                        DBManager.C.Talk.RECORDING_DATE,
                        DBManager.C.Talk.RETREAT_ID
                });

        publishProgress();
        return null;
    }

    @Override
    protected void extraTableProcessing(DBManager dbManager, JSONObject talks) throws JSONException {
        Iterator<String> it = talks.keys();
        while (it.hasNext()) {
            JSONObject talk = talks.getJSONObject(it.next());
            int talkID = talk.getInt("id");

            // First, clear any existing entries for this talk, so that if any teachers are ever
            // removed from a talk after the talk is already added, we'll pick up that change
            // in the database.
            dbManager.deleteID(talkID, DBManager.C.TalkTeachers.TABLE_NAME);

            JSONArray teachersForTalk = talk.getJSONArray("teachers");
            for (int i = 0; i < teachersForTalk.length(); i++) {
                int teacherID = teachersForTalk.getInt(i);

                SQLiteDatabase db = dbManager.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(DBManager.C.TalkTeachers.TALK_ID, talkID);
                values.put(DBManager.C.TalkTeachers.TEACHER_ID, teacherID);
                db.insert(DBManager.C.TalkTeachers.TABLE_NAME,null, values);
            }
        }
    }
}
