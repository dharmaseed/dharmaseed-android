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
import android.net.Uri;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
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
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Timer;

public class PlayTalkActivity extends AppCompatActivity
        implements SeekBar.OnSeekBarChangeListener, DeleteTalkFragment.DeleteTalkListener {

    int talkID;
    DBManager dbManager;
    boolean userDraggingSeekBar;

    boolean shouldSeekToResumePos;
    int resumePos;

    Timer timer;

    ListenableFuture<MediaController> controllerFuture;
    MediaController mediaController;

    Talk talk;

    static final String LOG_TAG = "PlayTalkActivity";
    public static final int SEEK_AMOUNT_MS = 15000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_talk);
        Toolbar toolbar = (Toolbar) findViewById(R.id.play_toolbar);
        setSupportActionBar(toolbar);
        getIntendedTalk();
        if (talkMissing())
            finish();
    }

    protected void getIntendedTalk() {
        // Get the ID of the talk to display
        Intent i = getIntent();
        Log.d(LOG_TAG, "Intent with type=" + i.getType() + " action="+i.getAction() + " data="+i.getData());
        Uri talkURI = i.getData();
        if (talkURI != null) {
            java.util.List<String> segments = talkURI.getPathSegments();
            if (segments.size() >= 2 &&
                    segments.get(0).equals("talks") &&
                    segments.get(1).matches("\\d+")) {
                talkID = Integer.parseInt(segments.get(1));
            } else
                Log.d(LOG_TAG, "Failed to get talkID from URI "+talkURI);
        } else {
            talkID = (int) i.getLongExtra(NavigationActivity.TALK_DETAIL_EXTRA, -1);
            if (talkID < 0)
                talkID = i.getIntExtra(NavigationActivity.TALK_DETAIL_EXTRA, -1);
        }

        dbManager = DBManager.getInstance(this);

        // only hit the DB again if we know the talk is different than the one
        // we have saved.
        // for example, if the user selects a talk, exits, and re-opens it, no need
        // to hit the DB again, since we already have that talk saved
        if (talk == null || talk.getId() != talkID) {
            talk = Talk.lookup(dbManager, getApplicationContext(), talkID);
        } // else we already have the talk, just re-draw the page
    }

    protected boolean talkMissing() {
        if (talk == null) {
            final String talkError = "Sorry - unable to play talk #" + talkID + "!";
            Log.d(LOG_TAG, talkError);
            showToast(talkError, Toast.LENGTH_SHORT);
        }
        return (talk == null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (talkMissing())
            return;

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Turn on action bar up/home button
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // hide back button if the activity was launched by clicking on a link
            boolean showBackArrow = (getIntent().getData() == null);
            actionBar.setDisplayHomeAsUpEnabled(showBackArrow);
        }

        // Set the talk title
        TextView titleView = (TextView) findViewById(R.id.play_talk_talk_title);
        titleView.setText(talk.getTitle());

        // Set the teacher name
        TextView teacherView = (TextView) findViewById(R.id.play_talk_teacher);
        teacherView.setText(talk.getAllTeacherNames());

        // Set the center name
        TextView centerView = (TextView) findViewById(R.id.play_talk_center);
        centerView.setText(talk.getCenterName());

        // Set the talk description
        TextView descriptionView = (TextView) findViewById(R.id.play_talk_talk_description);
        descriptionView.setText(talk.getDescription());

        // Set teacher photo
        ImageView photoView = (ImageView) findViewById(R.id.play_talk_teacher_photo);
        Bitmap photo = FileManager.findPhoto(dbManager.getContext(), talk.getTeacherId());
        photoView.setImageBitmap(photo);

        // Set date
        TextView dateView = (TextView) findViewById(R.id.play_talk_date);
        dateView.setText(talk.getDate());

        // set the image of the download button based on whether the talk is
        // downloaded or not
        toggleDownloadImage();

        // Initialise seek bar
        final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
        seekBar.setMax((int)(60*1000*talk.getDurationInMinutes()));
        userDraggingSeekBar = false;
        seekBar.setOnSeekBarChangeListener(this);

        // retrieve progress from TalkHistory DB table
        final int pos = (int) (dbManager.getTalkProgress(talkID)*60*1000);
        setTalkProgress(pos);

        // State data telling us if we should seek to the resume position of a talk when starting playback.
        // See playTalkButtonClicked comments for why this is needed.
        shouldSeekToResumePos = false;
        resumePos = pos;

        // initialise timers
        timer = new Timer();

        // - periodically update the seek bar and talk time
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updatePlayerUI();
                    }
                });
            }
        }, 0, 1000);

        Log.i(LOG_TAG,"started timers");

        // Create a MediaController to interact with the PlaybackService
        SessionToken sessionToken =
                new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        Futures.addCallback(
                controllerFuture,
                new FutureCallback<MediaController>() {
                    public void onSuccess(MediaController controller) {
                        controller.addListener(playerListener);
                        mediaController = controller;
                        playerListener.onIsPlayingChanged(controller.isPlaying());
                        updatePlayerUI();
                    }
                    public void onFailure(Throwable t) {
                        Log.e(LOG_TAG, "Could not create media controller. " + t);
                    }
                },
                MoreExecutors.directExecutor()
        );
    }

    public void updatePlayerUI()
    {
        if (mediaController != null) {

            // Disable UI controls if we're currently playing a different talk
            final int[] controlIDs = {
                R.id.play_talk_seek_bar,
                R.id.play_talk_talk_duration,
                R.id.activity_play_talk_ff_button,
                R.id.activity_play_talk_rw_button
            };
            for (int controlID : controlIDs) {
                final View view = findViewById(controlID);
                if (mediaControlsEnabled()) {
                    view.setClickable(true);
                    view.setEnabled(true);
                    view.setAlpha(1.0f);
                } else {
                    view.setClickable(false);
                    view.setEnabled(false);
                    view.setAlpha(0.5f);
                }
            }

            // Update seek bar position
            if (mediaController.getPlaybackState() == Player.STATE_READY
                && ! userDraggingSeekBar && mediaControlsEnabled()) {
                try {
                    int pos = (int) mediaController.getCurrentPosition();
                    long mpDuration = mediaController.getDuration();
                    if (mpDuration != TIME_UNSET) {
                        final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
                        seekBar.setMax((int) mpDuration);
                        seekBar.setProgress(pos);
                        updateDurationText(pos);
                    }
                } catch (IllegalStateException e) {
                }
            }
        }
    }

    public int getSeekBarProgress()
    {
        SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
        return seekBar.getProgress();
    }

    public int getTalkProgress()
    {
        if (mediaController == null || mediaController.getPlaybackState() == Player.STATE_IDLE) {
            return getSeekBarProgress();
        } else {
            return (int) mediaController.getCurrentPosition();

        }
    }

    public void onPause() {
        super.onPause();
        MediaController.releaseFuture(controllerFuture);
        Log.i(LOG_TAG, "stopping timers");
        timer.cancel();
    }


    public void updateDurationText(int pos)
    {
        final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
        String posStr = DateUtils.formatElapsedTime(pos / 1000);
        String mpDurStr = DateUtils.formatElapsedTime(seekBar.getMax() / 1000);
        TextView durationView = (TextView) findViewById(R.id.play_talk_talk_duration);
        durationView.setText(posStr + "/" + mpDurStr);
    }

    public void setTalkProgress(int pos)
    {
        final SeekBar seekBar = (SeekBar) findViewById(R.id.play_talk_seek_bar);
        final int newPos = Math.max(0,Math.min(pos, seekBar.getMax()));
        Log.i(LOG_TAG, "setting progress for talk "+talkID+" to "+newPos+" (originally "+pos+")");
        if (mediaControlsEnabled() && mediaController != null && mediaController.getPlaybackState() != Player.STATE_IDLE) {
            mediaController.seekTo(pos);
        } else {
            seekBar.setProgress(newPos);
            updateDurationText(seekBar.getProgress());
        }
    }

    private boolean mediaControlsEnabled() {
        return mediaController != null &&
                mediaController.getCurrentMediaItem() != null &&
                Integer.parseInt(mediaController.getCurrentMediaItem().mediaId) == talkID;
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
        playButton.setEnabled(true);
    }

    public void playTalkButtonClicked(View view) {
        Log.d(LOG_TAG, "button pressed");

        String talkIDString = Integer.toString(talkID);
        MediaItem currentItem = mediaController.getCurrentMediaItem();
        if (currentItem == null ||
                ! currentItem.mediaId.equals(talkIDString)) {
            MediaItem item = new MediaItem.Builder().setMediaId(talkIDString).build();
            mediaController.setMediaItem(item);

            ImageButton playButton = (ImageButton) findViewById(R.id.activity_play_talk_play_button);
            playButton.setAlpha(.5f);
            playButton.setEnabled(false);
            mediaController.prepare();
            mediaController.play();

            // Ideally, we'd like to simply call mediaController.seekTo(getTalkProgress()) to resume
            // playback at the last saved point. Unfortunately, it seems that the seek command does
            // not become available until the player is fully prepared, which can take a little while,
            // so if we call seekTo here, it'll simply be ignored. :(
            //
            // As a workaround, remember the position to seek to here, and actually seek in
            // playerListener.onAvailableCommandsChanged, which will fire once seeking becomes possible.
            shouldSeekToResumePos = true;

        } else if (mediaController.isPlaying()) {
            mediaController.pause();
        } else {
            mediaController.play();
        }

    }

    public void fastForwardButtonClicked(View view) {
        setTalkProgress(getTalkProgress() + SEEK_AMOUNT_MS);
    }

    public void rewindButtonClicked(View view) {
        setTalkProgress(getTalkProgress() - SEEK_AMOUNT_MS);
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
        setTalkProgress(getSeekBarProgress());
    }

    /**
     * Download the talk if we have permission. If we don't have permission, request it
     */
    private void downloadTalk() {
        new DownloadTalkTask().execute(talk);
    }

    public void toggleDownloadImage() {
        ImageButton downloadButton = (ImageButton) findViewById(R.id.download_button);
        if (talk.isDownloaded(dbManager)) {
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
        if (!FileManager.delete(talk)) {
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
                totalSize = FileManager.download(this.talk);
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
                public void onAvailableCommandsChanged(Player.Commands availableCommands) {
                    if (availableCommands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) && shouldSeekToResumePos) {
                        setTalkProgress(resumePos);
                        shouldSeekToResumePos = false;
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    // Update Play/Pause button
                    if (mediaController != null &&
                        mediaController.getCurrentMediaItem() != null &&
                        Integer.parseInt(mediaController.getCurrentMediaItem().mediaId) == talkID &&
                        isPlaying) {
                            setPPButton("ic_media_pause");
                    } else {
                        setPPButton("ic_media_play");
                    }
                }

            };

}
