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

import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class PlayTalkActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    TalkPlayerFragment talkPlayerFragment;
    String url;
    int talkID;
    DBManager dbManager;
    boolean userDraggingSeekBar;
    int userSeekBarPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_talk);

        dbManager = new DBManager(this);

        // Turn on action bar up/home button
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get the ID of the talk to display
        Intent i = getIntent();
        talkID = (int) i.getLongExtra(NavigationActivity.TALK_DETAIL_EXTRA, 0);

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

            // Save the URL
            url = "http://www.dharmaseed.org" + cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Talk.AUDIO_URL));

            // Set teacher photo
            String photoFilename = DBManager.getTeacherPhotoFilename(cursor.getInt(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.ID)));
            ImageView photoView = (ImageView) findViewById(R.id.play_talk_teacher_photo);
            Log.i("PlayTalkActivity", "photoFilename: "+photoFilename);
            try {
                FileInputStream photo = openFileInput(photoFilename);
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
                talkPlayerFragment = new TalkPlayerFragment();
                talkPlayerFragment.setUrl(url);
                fm.beginTransaction().add(talkPlayerFragment, "talkPlayerFragment").commit();
            } else if(talkPlayerFragment.getMediaPlayer().isPlaying()) {
                setPPButton("ic_media_pause");
            }

            // Set the talk duration
            final TextView durationView = (TextView) findViewById(R.id.play_talk_talk_duration);
            double duration = cursor.getDouble(cursor.getColumnIndexOrThrow(DBManager.C.Talk.DURATION_IN_MINUTES));
            String durationStr = DateUtils.formatElapsedTime((long)(duration*60));
            durationView.setText(durationStr);

            // Start a handler to periodically update the seek bar and talk time
            final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
            seekBar.setMax((int)(duration*60*1000));
            userDraggingSeekBar = false;
            userSeekBarPosition = 0;
            seekBar.setOnSeekBarChangeListener(this);
            final Handler handler = new Handler();
            final MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(this, 1000);
                    if (talkPlayerFragment.getMediaPrepared() && ! userDraggingSeekBar) {
                        try {
                            int pos = mediaPlayer.getCurrentPosition();
                            int mpDuration = mediaPlayer.getDuration();
                            seekBar.setMax(mpDuration);
                            seekBar.setProgress(pos);
                            String posStr = DateUtils.formatElapsedTime(pos / 1000);
                            String mpDurStr = DateUtils.formatElapsedTime(mpDuration / 1000);
                            durationView.setText(posStr + "/" + mpDurStr);
                        } catch(IllegalStateException e) {}
                    }
                }
            });

        } else {
            Log.e("PlayTalkActivity", "Could not look up talk, id="+talkID);
        }

        cursor.close();

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.play_talk, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        MenuItem star = menu.findItem(R.id.play_talk_action_toggle_starred);
        if(dbManager.isStarred(DBManager.C.TalkStars.TABLE_NAME, talkID)) {
            star.setIcon(ContextCompat.getDrawable(this,
                    getResources().getIdentifier("btn_star_big_on", "drawable", "android")));
        } else {
            star.setIcon(ContextCompat.getDrawable(this,
                    getResources().getIdentifier("btn_star_big_off", "drawable", "android")));
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.play_talk_action_toggle_starred:
                if(dbManager.isStarred(DBManager.C.TalkStars.TABLE_NAME, talkID)) {
                    dbManager.removeStar(DBManager.C.TalkStars.TABLE_NAME, talkID);
                    item.setIcon(getResources().getIdentifier("btn_star_big_off", "drawable", "android"));
                } else {
                    dbManager.addStar(DBManager.C.TalkStars.TABLE_NAME, talkID);
                    item.setIcon(getResources().getIdentifier("btn_star_big_on", "drawable", "android"));
                }

                // Notify main activity to update its data
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("updateDisplayedData"));

                return true;
        }

        return super.onOptionsItemSelected(item);
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
        int newPosition = Math.min(currentPosition + 15000, mediaPlayer.getDuration());
        mediaPlayer.seekTo(newPosition);
    }

    public void rewindButtonClicked(View view) {
        MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
        int currentPosition = mediaPlayer.getCurrentPosition();
        int newPosition = Math.max(currentPosition - 15000, 0);
        mediaPlayer.seekTo(newPosition);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser) {
            userSeekBarPosition = progress;
            String posStr = DateUtils.formatElapsedTime(progress / 1000);
            String mpDurStr = DateUtils.formatElapsedTime(seekBar.getMax() / 1000);
            TextView durationView = (TextView) findViewById(R.id.play_talk_talk_duration);
            durationView.setText(posStr + "/" + mpDurStr);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        userDraggingSeekBar = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        userDraggingSeekBar = false;
        talkPlayerFragment.setUserSeekBarPosition(userSeekBarPosition);
        MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
        try {
            mediaPlayer.seekTo(userSeekBarPosition);
        } catch (IllegalStateException e) {}
    }
}
