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
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class TalkCursorAdapter extends StarCursorAdapter {

    public TalkCursorAdapter(DBManager dbManager, NavigationActivity context, int layout, Cursor c) {
        super(dbManager, DBManager.C.TalkStars.TABLE_NAME, context, layout, c);
    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, null);
    }

    private String getString(Cursor cursor, String fullyQualifiedColumn) {
        return cursor.getString(cursor.getColumnIndexOrThrow(
                DBManager.getAlias(fullyQualifiedColumn))).trim();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        final int talkID = cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Talk.ID));
        final Talk talk = Talk.lookup(dbManager, context, talkID);

        // Set talk title and teacher name
        final TextView title=(TextView)view.findViewById(R.id.item_view_title);
        final TextView teacher=(TextView)view.findViewById(R.id.item_view_detail1);
        final TextView center=(TextView)view.findViewById(R.id.item_view_detail2);
        title.setText(talk.getTitle());
        teacher.setText(talk.getAllTeachers());
        center.setText(talk.getCenterName());

        // Set date
        final TextView date=(TextView)view.findViewById(R.id.item_view_detail3);
        date.setText(talk.getDate());

        // Set the talk duration
        final TextView durationView = (TextView)view.findViewById(R.id.item_view_detail4);
        durationView.setText(talk.getFormattedDuration());

        // Set teacher photo
        String photoFilename = DBManager.getTeacherPhotoFilename(cursor.getInt(cursor.getColumnIndexOrThrow(
                DBManager.getAlias(DBManager.C.Talk.TABLE_NAME + "." + DBManager.C.Talk.TEACHER_ID))));
        ImageView photoView = (ImageView) view.findViewById(R.id.item_view_photo);
        try {
            FileInputStream photo = context.openFileInput(photoFilename);
            photoView.setImageBitmap(BitmapFactory.decodeStream(photo));
            photo.close();
        } catch(IOException e) {
            Drawable icon = ContextCompat.getDrawable(context, R.drawable.dharmaseed_icon);
            photoView.setImageDrawable(icon);
        }

        // Show talk progress
        ProgressBar talk_progress = (ProgressBar) view.findViewById(R.id.item_view_progress);
        int duration_s = (int) (60*talk.getDurationInMinutes());
        int progress_s = (int) (60*dbManager.getTalkProgress(talkID));
        if (progress_s > 0) {
            talk_progress.setMax(duration_s);
            talk_progress.setProgress(progress_s);
            talk_progress.setVisibility(View.VISIBLE);
        } else {
            talk_progress.setVisibility(View.GONE);
        }

        // Set talk stars
        handleStars(view, talkID);
    }


}
