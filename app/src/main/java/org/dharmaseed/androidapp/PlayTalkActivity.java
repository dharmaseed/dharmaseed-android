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
        Cursor cursor = db.rawQuery("SELECT * FROM "+DBManager.C.Talk.TABLE_NAME+" WHERE "
                +DBManager.C.Talk.ID+"="+talkID, null);
        if(cursor.moveToFirst()) {
            // Set the talk title
            TextView title = (TextView) findViewById(R.id.play_talk_talk_title);
            title.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TITLE)));

            // Set the talk description
            TextView description = (TextView) findViewById(R.id.play_talk_talk_description);
            description.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DESCRIPTION)));

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
