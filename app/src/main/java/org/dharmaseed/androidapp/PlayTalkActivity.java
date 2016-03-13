package org.dharmaseed.androidapp;

import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PlayTalkActivity extends AppCompatActivity {

    TalkPlayerFragment talkPlayerFragment;
    long talkID;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_talk);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get the ID of the talk to display
        Intent i = getIntent();
        talkID = i.getLongExtra(NavigationActivity.TALK_DETAIL_EXTRA, 0);

        // Look up this talk
        DBManager dbManager = new DBManager(this);
        SQLiteDatabase db = dbManager.getReadableDatabase();
        String query = String.format(
                "SELECT %s, %s.%s, %s, %s, %s, %s, %s.%s AS teacher_name, %s.%s AS center_name, "
                        + "%s.%s FROM %s, %s, %s WHERE %s.%s=%s.%s AND %s.%s=%s.%s AND %s.%s=%s",
                DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.DESCRIPTION,
                DBManager.C.Talk.AUDIO_URL,
                DBManager.C.Talk.DURATION_IN_MINUTES,
                DBManager.C.Talk.RECORDING_DATE,
                DBManager.C.Talk.UPDATE_DATE,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.NAME,
                DBManager.C.Center.TABLE_NAME,
                DBManager.C.Center.NAME,

                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,

                // FROM
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Center.TABLE_NAME,

                // WHERE
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,

                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.VENUE_ID,
                DBManager.C.Center.TABLE_NAME,
                DBManager.C.Center.ID,

                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.ID,
                talkID
        );
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()) {
            // Set the talk title
            TextView titleView = (TextView) findViewById(R.id.play_talk_talk_title);
            titleView.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TITLE)).trim());

            // Set the teacher name
            TextView teacherView = (TextView) findViewById(R.id.play_talk_teacher);
            teacherView.setText(cursor.getString(cursor.getColumnIndexOrThrow("teacher_name")).trim());

            // Set the center name
            TextView centerView = (TextView) findViewById(R.id.play_talk_center);
            centerView.setText(cursor.getString(cursor.getColumnIndexOrThrow("center_name")).trim());

            // Set the talk description
            TextView descriptionView = (TextView) findViewById(R.id.play_talk_talk_description);
            descriptionView.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DESCRIPTION)).trim());

            // Set the talk duration
            TextView durationView = (TextView) findViewById(R.id.play_talk_talk_duration);
            double duration = cursor.getDouble(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DURATION_IN_MINUTES));
            int durationHr  = (int)Math.floor(duration/60);
            int durationMin = (int)Math.floor(duration-durationHr*60);
            int durationSec = (int)Math.floor((duration-Math.floor(duration))*60);
            String durationStr;
            if(durationHr > 0) {
                durationStr = String.format(Locale.US, "%02d:%02d:%02d", durationHr, durationMin, durationSec);
            } else {
                durationStr = String.format(Locale.US, "%02d:%02d", durationMin, durationSec);
            }
            durationView.setText(durationStr);

            // Save the URL
            url = "http://www.dharmaseed.org" + cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.AUDIO_URL));

            // Set teacher photo
            String photoFilename = DBManager.getTeacherPhotoFilename(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.ID)));
            ImageView photoView = (ImageView) findViewById(R.id.play_talk_teacher_photo);
            Log.i("PlayTalkActivity", "photoFilename: "+photoFilename);
            try {
                FileInputStream photo = getApplicationContext().openFileInput(photoFilename);
                photoView.setImageBitmap(BitmapFactory.decodeStream(photo));
            } catch(FileNotFoundException e) {
                Drawable icon = ContextCompat.getDrawable(this, R.drawable.dharmaseed_icon);
                photoView.setImageDrawable(icon);
            }

            // Set date
            TextView dateView = (TextView) findViewById(R.id.play_talk_date);
            String recDate = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.RECORDING_DATE));
            if(recDate == null) {
                recDate = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.UPDATE_DATE));
            }
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                dateView.setText(DateFormat.getDateInstance().format(parser.parse(recDate)));
            } catch(ParseException e) {
                dateView.setText("");
                Log.w("playTalk", "Could not parse talk date for talk ID " + talkID);
            }

            // Get/create a persistent fragment to manage the MediaPlayer instance
            FragmentManager fm = getSupportFragmentManager();
            talkPlayerFragment = (TalkPlayerFragment) fm.findFragmentByTag("talkPlayerFragment");
            if (talkPlayerFragment == null) {
                // add the fragment
                talkPlayerFragment = new TalkPlayerFragment(url);
                fm.beginTransaction().add(talkPlayerFragment, "talkPlayerFragment").commit();
            } else if(talkPlayerFragment.getMediaPlayer().isPlaying()) {
                setPPButton("ic_media_pause");
            }

        } else {
            Log.e("PlayTalkActivity", "Could not look up talk, id="+talkID);
        }

        cursor.close();

    }

    public void setPPButton(String drawableName) {
        ImageButton playButton = (ImageButton) findViewById(R.id.activity_play_talk_play_button);
        playButton.setImageDrawable(ContextCompat.getDrawable(this,
                getResources().getIdentifier(drawableName, "drawable", "android")));
    }

    public void playTalkButtonClicked(View view) {
        Log.d("playTalk", "button pressed");
        MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            setPPButton("ic_media_play");
        } else if(talkPlayerFragment.getMediaPrepared()) {
            mediaPlayer.start();
            setPPButton("ic_media_pause");
        } else {
            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Log.e("playTalk", e.toString());
            }
        }
    }

    public void fastForwardButtonClicked(View view) {
        MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
        int currentPosition = mediaPlayer.getCurrentPosition();
        int newPosition = Math.min(currentPosition + 30000, mediaPlayer.getDuration());
        mediaPlayer.seekTo(newPosition);
    }

    public void rewindButtonClicked(View view) {
        MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
        int currentPosition = mediaPlayer.getCurrentPosition();
        int newPosition = Math.max(currentPosition - 30000, 0);
        mediaPlayer.seekTo(newPosition);
    }

}
