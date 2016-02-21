package org.dharmaseed.androidapp;

import android.support.v4.widget.CursorAdapter;

/**
 * Created by bbethke on 2/19/16.
 */
public class TalkFetcherTask extends DataFetcherTask {

    public TalkFetcherTask(DBManager dbManager, CursorAdapter cursorAdapter) {
        super(dbManager, cursorAdapter);
    }

    @Override
    protected Void doInBackground(Void... params) {

        // Update the list view immediately
        publishProgress();

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
}
