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

import static androidx.media3.common.C.TIME_UNSET;

import android.app.DialogFragment;
import android.content.ComponentName;
import android.graphics.drawable.Animatable;
import android.os.AsyncTask;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.GregorianCalendar;
import java.util.Timer;

public class PlayTalkActivity extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener, DeleteTalkFragment.DeleteTalkListener {
    
    int talkID;
    DBManager dbManager;
    boolean userDraggingSeekBar;

    Timer timer;

    MediaController mediaController;

    Talk talk;

    static final String LOG_TAG = "PlayTalkActivity";

    protected static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // request code for writing external storage (the number is arbitrary)
    static final int PERMISSIONS_WRITE_EXTERNAL_STORAGE = 9087;

    protected void prepareTalkPlayerFragment()
    {
        if (talkPlayerFragment == null)
            Log.e(LOG_TAG, "talkPlayerFragment must be non-null!");

        final MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
        try {
            mediaPlayer.reset();
            if (talk.isDownloaded()) {
                Log.d(LOG_TAG, "Playing from " + talk.getPath());
                mediaPlayer.setDataSource("file://" + talk.getPath());
            } else {
                mediaPlayer.setDataSource(talk.getAudioUrl());
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.toString());
        }
        Log.i(LOG_TAG,"preparing media player for "+talkID);
        mediaPlayer.prepareAsync();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_talk);

        // Turn on action bar up/home button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get the ID of the talk to display
        Intent i = getIntent();
        talkID = (int) i.getLongExtra(NavigationActivity.TALK_DETAIL_EXTRA, 0);

        dbManager = DBManager.getInstance(this);

        // only hit the DB again if we know the talk is different than the one
        // we have saved.
        // for example, if the user selects a talk, exits, and re-opens it, no need
        // to hit the DB again, since we already have that talk saved
        if (talk == null || talk.getId() != talkID) {
            Cursor cursor = getCursor(dbManager, talkID);
            if (cursor.moveToFirst()) {
                // convert DB result to an object
                talk = new Talk(cursor, getApplicationContext());
                talk.setId(talkID);
            } else {
                Log.e(LOG_TAG, "Could not look up talk, id=" + talkID);
                cursor.close();
                return;
            }
            cursor.close();
        } // else we already have the talk, just re-draw the page

        // Set the talk title
        TextView titleView = (TextView) findViewById(R.id.play_talk_talk_title);
        titleView.setText(talk.getTitle());

        // Set the teacher name
        TextView teacherView = (TextView) findViewById(R.id.play_talk_teacher);
        teacherView.setText(talk.getTeacherName());

        // Set the center name
        TextView centerView = (TextView) findViewById(R.id.play_talk_center);
        centerView.setText(talk.getCenterName());

        // Set the talk description
        TextView descriptionView = (TextView) findViewById(R.id.play_talk_talk_description);
        descriptionView.setText(talk.getDescription());

        // Set teacher photo
        String photoFilename = talk.getPhotoFileName();
        ImageView photoView = (ImageView) findViewById(R.id.play_talk_teacher_photo);
        Log.i(LOG_TAG, "photoFilename: " + photoFilename);
        try {
            FileInputStream photo = openFileInput(photoFilename);
            photoView.setImageBitmap(BitmapFactory.decodeStream(photo));
        } catch (FileNotFoundException e) {
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.dharmaseed_icon);
            photoView.setImageDrawable(icon);
        }

        // Set date
        TextView dateView = (TextView) findViewById(R.id.play_talk_date);
        String recDate = talk.getDate();
        SimpleDateFormat parser = new SimpleDateFormat(DATE_FORMAT);
        try {
            dateView.setText(DateFormat.getDateInstance().format(parser.parse(recDate)));
        } catch (ParseException e) {
            dateView.setText("");
            Log.w(LOG_TAG, "Could not parse talk date for talk ID " + talkID);
        }

        // set the image of the download button based on whether the talk is
        // downloaded or not
        toggleDownloadImage();

        // Initialise seek bar
        final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
        seekBar.setMax((int)(60*1000*talk.getDurationInMinutes()));
        userDraggingSeekBar = false;
        seekBar.setOnSeekBarChangeListener(this);

        // Get/create a persistent fragment to manage the MediaPlayer instance
        FragmentManager fm = getSupportFragmentManager();
        talkPlayerFragment = (TalkPlayerFragment) fm.findFragmentByTag("talkPlayerFragment");
        if (talkPlayerFragment == null) {
            // add the fragment
            talkPlayerFragment = new TalkPlayerFragment();
            fm.beginTransaction().add(talkPlayerFragment, "talkPlayerFragment").commit();

            // retrieve progress from TalkHistory DB table
            final int pos = (int) (dbManager.getTalkProgress(talkID)*60*1000);
            setTalkProgress(pos, false);
        } else if(talkPlayerFragment.getMediaPlayer().isPlaying()) {
            setPPButton("ic_media_pause");
            updateSeekBar();
            Log.i(LOG_TAG,"talk "+talkID+" already playing!");
        }

        // initialise timers
        timer = new Timer();

        // - periodically update the seek bar and talk time
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSeekBar();
                    }
                });
            }
        }, 1000, 1000);

        // - periodically update play progress information
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logTalkProgress();
                    }
                });
            }
        }, 10000, 10000);

        Log.i(LOG_TAG,"started timers");
    }

    @Override
    public void onStart() {
        super.onStart();

        SessionToken sessionToken =
                new SessionToken(this, new ComponentName(this, PlaybackService.class));
        ListenableFuture<MediaController> controllerFuture =
                new MediaController.Builder(this, sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
                try {
                    mediaController = controllerFuture.get();
                    mediaController.addListener(playerListener);
                } catch (InterruptedException | ExecutionException e) {
                    Log.e(LOG_TAG, "Could not create media controller. " + e.toString());
                }
            }, ContextCompat.getMainExecutor(this));

        }

    @Override
    public void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "stopping timers");
        timer.cancel();
    }

    public void updateSeekBar()
    {
        if (talkPlayerFragment.getMediaPrepared() && ! userDraggingSeekBar) {
            Log.d(LOG_TAG,"talk "+talkID+": updating seekBar");
            final MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
            final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
            try {
                int pos = mediaPlayer.getCurrentPosition();
                int mpDuration = mediaPlayer.getDuration();
                seekBar.setMax(mpDuration);
                seekBar.setProgress(pos);
                updateDurationText(pos);
            } catch(IllegalStateException e) {}
        }
    }

    public int getSeekBarProgress()
    {
        SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
        return seekBar.getProgress();
    }

    public int getTalkProgress()
    {
        int pos = getSeekBarProgress();
        if (talkPlayerFragment.getMediaPrepared()) {
            final MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
            try {
                pos = mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {}
        }
        return pos;
    }

    public void logTalkProgress()
    {
        SimpleDateFormat parser = new SimpleDateFormat(DATE_FORMAT);
        final String now = parser.format(GregorianCalendar.getInstance().getTime());
        final int pos = getTalkProgress();
        Log.d(LOG_TAG, "talk "+talkID+": progress POS="+pos+" on DATE="+now);
        dbManager.setTalkProgress(talkID, now, pos/(1000*60.0));
    }

    public void onStop() {
        super.onStop();
        mediaController.release();
    }


    public void updateDurationText(int pos)
    {
        final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
        String posStr = DateUtils.formatElapsedTime(pos / 1000);
        String mpDurStr = DateUtils.formatElapsedTime(seekBar.getMax() / 1000);
        TextView durationView = (TextView) findViewById(R.id.play_talk_talk_duration);
        durationView.setText(posStr + "/" + mpDurStr);
    }

    public void setTalkProgress(int pos, boolean logProgress)
    {
        final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
        final int newPos = Math.max(0,Math.min(pos, seekBar.getMax()));
        Log.i(LOG_TAG, "setting progress for talk "+talkID+" to "+newPos+" (originally "+pos+")");
        if (talkPlayerFragment.getMediaPrepared()) {
            final MediaPlayer mediaPlayer = talkPlayerFragment.getMediaPlayer();
            try {
                mediaPlayer.seekTo(newPos);
            } catch (IllegalStateException e) {}
        } else {
            seekBar.setProgress(newPos);
            updateDurationText(seekBar.getProgress());
        }

        if (logProgress)
            logTalkProgress();
    }

    public static Cursor getCursor(DBManager dbManager, int talkID) {

        SQLiteDatabase db = dbManager.getReadableDatabase();
        String query = String.format(
                "SELECT %s, %s.%s, %s, %s, %s, %s, %s, %s, %s, %s.%s AS teacher_name, %s.%s AS center_name, "
                        + "%s.%s FROM %s, %s, %s WHERE %s.%s=%s.%s AND %s.%s=%s.%s AND %s.%s=%s",
                DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.DESCRIPTION,
                DBManager.C.Talk.AUDIO_URL,
                DBManager.C.Talk.DURATION_IN_MINUTES,
                DBManager.C.Talk.RECORDING_DATE,
                DBManager.C.Talk.UPDATE_DATE,
                DBManager.C.Talk.RETREAT_ID,
                DBManager.C.Talk.FILE_PATH,
                DBManager.C.Talk.TEACHER_ID,
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

        return db.rawQuery(query, null);
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
        playButton.setAlpha(1f);
        playButton.setClickable(true);
    }

    public void playTalkButtonClicked(View view) {
        Log.d(LOG_TAG, "button pressed");

        String talkIDString = Integer.toString(talkID);
        MediaItem currentItem = mediaController.getCurrentMediaItem();
        if (currentItem == null ||
                ! currentItem.mediaId.equals(talkIDString)) {
            MediaItem item = new MediaItem.Builder().setMediaId(talkIDString).build();
            mediaController.setMediaItem(item);
            playButton.setAlpha(.5f);
            playButton.setClickable(false);
            mediaController.prepare();

            // TODO: Set these somewhere else?
            playButton.setAlpha(1f);
            playButton.setClickable(true);

            mediaController.play();
        } else if (mediaController.isPlaying()) {
            mediaController.pause();
            logTalkProgress();
        } else {
            mediaController.play();
        }

    }

    public void fastForwardButtonClicked(View view) {
        setTalkProgress(getTalkProgress() + 15000, true);
    }

    public void rewindButtonClicked(View view) {
        setTalkProgress(getTalkProgress() - 15000, true);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser)
            updateDurationText(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        userDraggingSeekBar = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        userDraggingSeekBar = false;
        setTalkProgress(getSeekBarProgress(), true);
    }

    /**
     * Download the talk if we have permission. If we don't have permission, request it
     */
    private void downloadTalk() {
        new DownloadTalkTask().execute(talk);
    }

    public void toggleDownloadImage() {
        ImageButton downloadButton = (ImageButton) findViewById(R.id.download_button);
        if (talk.isDownloaded()) {
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_check_circle_green_24dp);
            downloadButton.setImageDrawable(icon);
            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onDeleteTalkClicked(view);
                }
            });
        } else {
            Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_file_download_green_24dp);
            downloadButton.setImageDrawable(icon);
            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onDownloadButtonClicked(view);
                }
            });
        }
    }

    public void onDownloadButtonClicked(View view) {
        Log.d(LOG_TAG, "Downloading talk " + talk.getId());
        downloadTalk();
    }

    public void onDeleteTalkClicked(View view) {
        DialogFragment dialog = new DeleteTalkFragment();
        dialog.show(getFragmentManager(), "DeleteTalkFragment");
    }

    /**
     * Share a link to the dharmaseed website for this talk
     */
    public void onShareButtonClicked(View view) {
        String url = String.format("https://dharmaseed.org/teacher/%d/talk/%d/", talk.getTeacherId(), talk.getId());
        Log.d(LOG_TAG,"Sharing link to " + url);

        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // Add data to the intent, the receiving app will decide
        // what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, talk.getTitle());
        share.putExtra(Intent.EXTRA_TEXT, url);

        startActivity(Intent.createChooser(share, "Share talk"));
    }

    /**
     * Deletes the talk from the FS and removes the path from the DB row
     */
    public void deleteTalk() {
        if (!TalkManager.delete(talk)) {
            showToast("Unable to delete '" + talk.getTitle() + "'.", Toast.LENGTH_SHORT);
        } else {
            dbManager.removeDownload(talk);
            showToast("Deleted '" + talk.getTitle() + "'.", Toast.LENGTH_SHORT);
            toggleDownloadImage();
            Log.d(LOG_TAG, "Deleted talk " + talk.getId());
        }
    }

    public void startLoadingAnim() {
        ImageButton downloadButton = (ImageButton) findViewById(R.id.download_button);
        // override the current listener so the user can't accidentally download the talk
        // twice
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // do nothing
            }
        });
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_downloading);
        downloadButton.setImageDrawable(icon);
        if (icon instanceof Animatable)
            ((Animatable) icon).start();
    }

    public void stopLoadingAnim() {
        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_downloading);
        if (icon instanceof Animatable)
            ((Animatable) icon).stop();
    }

    public void showToast(String message, int length) {
        Toast.makeText(getApplicationContext(), message, length).show();
    }


    @Override
    public void onDeleteTalkPositiveClick(DialogFragment dialogFragment) {
        deleteTalk();
    }

    @Override
    public void onDeleteTalkNegativeClick(DialogFragment dialogFragment) {
        // do nothing
    }

    class DownloadTalkTask extends AsyncTask<Talk, Integer, Long> {

        private static final String LOG_TAG = "DownloadTalkTask";
        private Talk talk;

        public DownloadTalkTask() {}

        @Override
        protected void onPreExecute() {
            startLoadingAnim();
        }

        @Override
        protected Long doInBackground(Talk... talks) {
            long totalSize = 0;
            if (talks.length > 0) {
                // only download the first one if we get passed multiple
                this.talk = talks[0];
                totalSize = TalkManager.download(this.talk);
            }
            return totalSize;
        }

        @Override
        protected void onPostExecute(Long size) {
            if (size > 0) {
                if (dbManager.addDownload(this.talk)) {
                    showToast("'" + talk.getTitle() + "' downloaded.", Toast.LENGTH_SHORT);
                } else {
                    // remove talk from fs because we couldn't update the DB
                    deleteTalk();
                    Log.d(LOG_TAG, "failed to update db with talk path. deleting talk");
                }
            } else {
                showToast("Failed to download '" + talk.getTitle() + "'.", Toast.LENGTH_SHORT);
            }
            stopLoadingAnim();
            toggleDownloadImage();
        }
    }

    private final Player.Listener playerListener =
            new Player.Listener() {

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    // Update Play/Pause button
                    if (isPlaying) {
                        setPPButton("ic_media_pause");
                    } else {
                        setPPButton("ic_media_play");
                    }
                }

            };

}
