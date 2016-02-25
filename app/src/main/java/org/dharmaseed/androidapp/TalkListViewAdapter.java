package org.dharmaseed.androidapp;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
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

    public TalkListViewAdapter(Context context,int layout, Cursor c) {
        super(context, c, 0);
        this.layout=layout;
        this.inflater=LayoutInflater.from(context);
    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
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

    }

}
