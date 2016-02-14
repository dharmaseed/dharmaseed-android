package org.dharmaseed.androidapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class PlayTalkActivity extends AppCompatActivity
        implements MediaPlayer.OnPreparedListener {

    MediaPlayer mediaPlayer;
    boolean mediaPrepared;
    long talkID;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_talk);

        // Get the ID of the talk to display
        Intent i = getIntent();
        talkID = i.getLongExtra(NavigationActivity.TALK_DETAIL_EXTRA, 0);

        // Look up this talk
        DBManager dbManager = new DBManager(this);
        SQLiteDatabase db = dbManager.getReadableDatabase();
        String query = String.format(
                "SELECT %s, %s, %s, %s, %s FROM %s, %s WHERE %s.%s=%s.%s AND %s.%s=%s",
                DBManager.C.Talk.TITLE,
                DBManager.C.Talk.DESCRIPTION,
                DBManager.C.Talk.AUDIO_URL,
                DBManager.C.Talk.DURATION_IN_MINUTES,
                DBManager.C.Teacher.NAME,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.ID,
                talkID
        );
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToFirst()) {
            // Set the talk title
            TextView titleView = (TextView) findViewById(R.id.play_talk_talk_title);
            titleView.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TITLE)));

            // Set the teacher name
            TextView teacherView = (TextView) findViewById(R.id.play_talk_teacher);
            teacherView.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.NAME)));

            // Set the talk description
            TextView descriptionView = (TextView) findViewById(R.id.play_talk_talk_description);
            descriptionView.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DESCRIPTION)));

            // Set the talk duration
            TextView durationView = (TextView) findViewById(R.id.play_talk_talk_duration);
            double duration = cursor.getDouble(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DURATION_IN_MINUTES));
            int durationHr  = (int)Math.floor(duration/60);
            int durationMin = (int)Math.floor(duration-durationHr*60);
            int durationSec = (int)Math.floor((duration-Math.floor(duration))*60);
            String durationStr;
            if(durationHr > 0) {
                durationStr = String.format("%02d:%02d:%02d", durationHr, durationMin, durationSec);
            } else {
                durationStr = String.format("%02d:%02d", durationMin, durationSec);
            }
            durationView.setText(durationStr);

            // Save the URL
            url = "http://www.dharmaseed.org" + cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.AUDIO_URL));
        } else {
            Log.e("PlayTalkActivity", "Could not look up talk, id="+talkID);
        }

        cursor.close();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPrepared = false;
    }

    public void playTalk(View view) {
        Log.d("playTalk", "button pressed");
        if(mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else if(mediaPrepared) {
            mediaPlayer.start();
        } else {
            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Log.e("playTalk", e.toString());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i("playTalk", "playing talk");
        mediaPrepared = true;
        mediaPlayer.start();
    }

}
