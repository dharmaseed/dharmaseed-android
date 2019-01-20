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


import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by bbethke on 5/15/16.
 */
public abstract class StarCursorAdapter extends CursorAdapter {

    int layout;
    final LayoutInflater inflater;
    DBManager dbManager;
    NavigationActivity navigationActivity;
    String starTableName;

    public StarCursorAdapter(DBManager dbManager, String starTableName, NavigationActivity context, int layout, Cursor c) {
        super(context, c, 0);

        this.starTableName = starTableName;
        this.layout=layout;
        this.inflater=LayoutInflater.from(context);
        this.dbManager = dbManager;
        this.navigationActivity = context;
    }

    void handleStars(View view, final int itemId) {
        // Set star status
        final ImageView star = (ImageView) view.findViewById(R.id.item_view_star);
        boolean isStarred = dbManager.isStarred(starTableName, itemId);
        final Context ctx = view.getContext();
        if(isStarred) {
            star.setImageDrawable(ContextCompat.getDrawable(ctx,
                    ctx.getResources().getIdentifier("btn_star_big_on", "drawable", "android")));
        } else {
            star.setImageDrawable(ContextCompat.getDrawable(ctx,
                    ctx.getResources().getIdentifier("btn_star_big_off", "drawable", "android")));
        }

        // Set click handler
        star.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dbManager.isStarred(starTableName, itemId)) {
                    dbManager.removeStar(starTableName, itemId);
                    star.setImageDrawable(ContextCompat.getDrawable(ctx,
                            ctx.getResources().getIdentifier("btn_star_big_off", "drawable", "android")));
                } else {
                    dbManager.addStar(starTableName, itemId);
                    star.setImageDrawable(ContextCompat.getDrawable(ctx,
                            ctx.getResources().getIdentifier("btn_star_big_on", "drawable", "android")));
                }
                if(navigationActivity.starFilterOn) {
                    navigationActivity.updateDisplayedData();
                }
            }
        });

    }

    void clearView(View view) {
        int ids[] = {R.id.item_view_title, R.id.item_view_detail1, R.id.item_view_detail2,
                R.id.item_view_detail3, R.id.item_view_detail4};
        for(int id : ids) {
            TextView textView=(TextView)view.findViewById(id);
            textView.setText("");
        }
    }
}
