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
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class TeacherCursorAdapter extends StarCursorAdapter {


    public TeacherCursorAdapter(DBManager dbManager, NavigationActivity context, int layout, Cursor c) {
        super(dbManager, DBManager.C.TeacherStars.TABLE_NAME, context, layout, c);
    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        // Set teacher name
        clearView(view);
        TextView title=(TextView)view.findViewById(R.id.item_view_title);
        title.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.NAME)).trim());

        // Get number of talks by this teacher
        TextView numTalks=(TextView)view.findViewById(R.id.item_view_detail1);
        numTalks.setText(cursor.getString(cursor.getColumnIndexOrThrow("talk_count")) + " talks");

        // Set teacher photo
        Bitmap photo = FileManager.findPhoto(
                dbManager.getContext(),
                cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.ID))
        );
        ImageView photoView = view.findViewById(R.id.item_view_photo);
        photoView.setImageBitmap(photo);

        // Set stars
        handleStars(view, cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.ID)));
    }


}
