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

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class TalkListViewAdapter extends CursorAdapter {

    private int layout;
    private final LayoutInflater inflater;
    private DBManager dbManager;
    private NavigationActivity navigationActivity;

    public TalkListViewAdapter(NavigationActivity context, int layout, Cursor c) {
        super(context, c, 0);
        this.layout=layout;
        this.inflater=LayoutInflater.from(context);
        dbManager = new DBManager(context);
        this.navigationActivity = context;
    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Set talk title and teacher name
        TextView title=(TextView)view.findViewById(R.id.talkViewTitle);
        TextView teacher=(TextView)view.findViewById(R.id.talkViewTeacher);
        title.setText(cursor.getString(cursor.getColumnIndex(DBManager.C.Talk.TITLE)).trim());
        teacher.setText(cursor.getString(cursor.getColumnIndex(DBManager.C.Teacher.NAME)).trim());

        // Set teacher photo
        String photoFilename = DBManager.getTeacherPhotoFilename(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TEACHER_ID)));
        ImageView photoView = (ImageView) view.findViewById(R.id.talkViewTeacherPhoto);
        try {
            FileInputStream photo = context.openFileInput(photoFilename);
            photoView.setImageBitmap(BitmapFactory.decodeStream(photo));
        } catch(FileNotFoundException e) {
            Drawable icon = ContextCompat.getDrawable(context, R.drawable.dharmaseed_icon);
            photoView.setImageDrawable(icon);
        }

        // Set talk star status
        final ImageView star = (ImageView) view.findViewById(R.id.talk_list_item_star);
        final int talkId = cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Talk.ID));
        boolean isStarred = dbManager.isTalkStarred(talkId);
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
                if(dbManager.isTalkStarred(talkId)) {
                    dbManager.unstarTalk(talkId);
                    star.setImageDrawable(ContextCompat.getDrawable(ctx,
                            ctx.getResources().getIdentifier("btn_star_big_off", "drawable", "android")));
                } else {
                    dbManager.starTalk(talkId);
                    star.setImageDrawable(ContextCompat.getDrawable(ctx,
                            ctx.getResources().getIdentifier("btn_star_big_on", "drawable", "android")));
                }
                if(navigationActivity.starFilterOn) {
                    navigationActivity.updateDisplayedData();
                }
            }
        });

    }


}
