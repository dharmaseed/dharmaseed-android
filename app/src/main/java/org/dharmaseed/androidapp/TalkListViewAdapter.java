package org.dharmaseed.androidapp;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class TalkListViewAdapter extends SimpleCursorAdapter {

    private Context mContext;
    private Context appContext;
    private int layout;
    private Cursor cr;
    private final LayoutInflater inflater;

    public TalkListViewAdapter(Context context,int layout, Cursor c) {
        super(context,layout,c,new String[] {},new int[] {},0);
        this.layout=layout;
        this.mContext = context;
        this.inflater=LayoutInflater.from(context);
        this.cr=c;
    }

    @Override
    public View newView (Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(layout, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
        TextView title=(TextView)view.findViewById(R.id.talkViewTitle);
        TextView teacher=(TextView)view.findViewById(R.id.talkViewTeacher);

        title.setText(cursor.getString(cursor.getColumnIndex(DBManager.C.Talk.TITLE)));
        teacher.setText(cursor.getString(cursor.getColumnIndex(DBManager.C.Teacher.NAME)));

        // Set teacher photo
        String photoFilename = DBManager.getTeacherPhotoFilename(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TEACHER_ID)));
        try {
            FileInputStream photo = context.openFileInput(photoFilename);
            ImageView photoView = (ImageView) view.findViewById(R.id.talkViewTeacherPhoto);
            photoView.setImageBitmap(BitmapFactory.decodeStream(photo));
        } catch(FileNotFoundException e) {

        }

    }

}
