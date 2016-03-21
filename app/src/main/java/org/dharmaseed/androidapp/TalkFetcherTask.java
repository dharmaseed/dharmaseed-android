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

package org.dharmaseed.androidapp;

/**
 * Created by bbethke on 2/19/16.
 */
public class TalkFetcherTask extends DataFetcherTask {

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
}
