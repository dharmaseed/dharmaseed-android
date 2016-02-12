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
import android.widget.Toast;

public class PlayTalkActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
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
            TextView title = (TextView) findViewById(R.id.play_talk);
            title.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.TITLE)));

            // Set the talk description
            TextView description = (TextView) findViewById(R.id.activity_play_talk_talk_title);
            description.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DESCRIPTION)));

            // Save the URL
            url = "http://www.dharmaseed.org" + cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.AUDIO_URL));
        } else {
            Log.e("PlayTalkActivity", "Could not look up talk, id="+talkID);
        }

    }

    public void playTalk(View view) {
        Log.d("playTalk", "button pressed");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            Log.i("playTalk", "playing talk: " + url);
            mediaPlayer.setDataSource(url); //"https://archive.org/download/testmp3testfile/mpthreetest.mp3");
            // TODO: use prepareAsync instead to not block the UI thread
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e("playTalk", e.toString());
        }
    }
}
