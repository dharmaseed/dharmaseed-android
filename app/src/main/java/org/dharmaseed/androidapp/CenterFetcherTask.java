package org.dharmaseed.androidapp;


/**
 * Created by bbethke on 2/19/16.
 */
public class CenterFetcherTask extends DataFetcherTask {

    public CenterFetcherTask(DBManager dbManager, NavigationActivity navigationActivity) {
        super(dbManager, navigationActivity);
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
