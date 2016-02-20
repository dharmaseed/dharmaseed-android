package org.dharmaseed.androidapp;

import android.support.v4.widget.SimpleCursorAdapter;

/**
 * Created by bbethke on 2/19/16.
 */
public class CenterFetcherTask extends DataFetcherTask {

    public CenterFetcherTask(DBManager dbManager, SimpleCursorAdapter cursorAdapter) {
        super(dbManager, cursorAdapter);
    }

    @Override
    protected Void doInBackground(Void... params) {

        // Update the list view immediately
        publishProgress();

        updateTable(DBManager.C.Center.TABLE_NAME, DBManager.C.Center.ID,
                "venues/",
                new String[]{
                        DBManager.C.Center.WEBSITE,
                        DBManager.C.Center.DESCRIPTION,
                        DBManager.C.Center.NAME
                });

        publishProgress();
        return null;
    }
}
