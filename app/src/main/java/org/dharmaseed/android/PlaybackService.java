package org.dharmaseed.android;

import static androidx.media3.common.C.WAKE_MODE_NETWORK;

import static org.dharmaseed.android.NavigationActivity.TALK_DETAIL_EXTRA;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.Player;
import androidx.media3.session.MediaLibraryService;
import androidx.media.session.MediaButtonReceiver;

import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

public class PlaybackService extends MediaSessionService {
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";
    private static final String LOG_TAG = "PlaybackService";
    public static final String CHANNEL_ID_PLAYING = "playing";

    private MediaSession mediaSession;

    private ExoPlayer mediaPlayer;  // TODO destroy this in onDestroy
    private Talk talk;

    private Handler handler;

    // These are the available actions we advertise when the media session is active
    private final long sessionActions =
                    PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_STOP |
                    PlaybackStateCompat.ACTION_REWIND |
                    PlaybackStateCompat.ACTION_FAST_FORWARD |
                    PlaybackStateCompat.ACTION_SEEK_TO |
                    PlaybackStateCompat.ACTION_SET_PLAYBACK_SPEED |
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create the media player
        Context context = this;
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true);
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                context, httpFactory
        );
        mediaPlayer = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(context)
                                .setDataSourceFactory(dataSourceFactory))
                .setWakeMode(WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .setUseLazyPreparation(true)
                .setHandleAudioBecomingNoisy(true)
                .build();

        // Create a media session
        mediaSession = new MediaSession.Builder(this, mediaPlayer)
                .setCallback(new SessionCallback())
                .build();

//        // Enable callbacks from MediaButtons and TransportControls
//        mediaSession.setFlags(
//                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
//                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

//        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
//        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
//                .setActions(sessionActions);
//        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        // mediaSession.setCallback(new SessionCallback());

//        // Set the session's token so that client activities can communicate with it.
//        setSessionToken(mediaSession.getSessionToken());
//
//
//        // Poll the media player regularly and update our media session playback state
//        handler = new Handler();
//        exoplayerPoller.run();
    }

    private class SessionCallback implements MediaSession.Callback {

    }

//    private final Runnable exoplayerPoller = new Runnable() {
//        @Override
//        public void run() {
//            try {
//                // Update player status and post to media session
//                PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
//                if (mediaPlayer.isPlaying()) {
//                    builder.setState(PlaybackStateCompat.STATE_PLAYING,
//                            mediaPlayer.getCurrentPosition(), 1.0f);
//                } else {
//                    builder.setState(PlaybackStateCompat.STATE_PAUSED,
//                            mediaPlayer.getCurrentPosition(), 0.0f);
//                }
//                builder.setBufferedPosition(mediaPlayer.getBufferedPosition());
//                if (talk != null) {
//                    builder.setActiveQueueItemId(talk.getId());
//                }
//                builder.setActions(sessionActions);
//
//                // Add the duration of the media here
//                Bundle extras = new Bundle();
//                extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
//                        mediaPlayer.getDuration());
//                builder.setExtras(extras);
//                mediaSession.setPlaybackState(builder.build());
//
//            } finally {
//                // Run this again in a little bit
//                int mediaPlayerPollIntervalMs = 100;
//                handler.postDelayed(this, mediaPlayerPollIntervalMs);
//            }
//        }
//    };

    // Remember to release the player and media session in onDestroy
    @Override
    public void onDestroy() {
        mediaSession.getPlayer().release();
        mediaSession.release();
        mediaSession = null;
        super.onDestroy();
    }


//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        super.onStartCommand(intent, flags, startId);
//        Log.d(LOG_TAG, "onStartCommand");
//
//        // Given a media session and its context (usually the component containing the session)
//        // Create a NotificationCompat.Builder
//        Context context = getApplicationContext();
//
//        // Create a notification channel
//        NotificationChannelCompat chan = new NotificationChannelCompat.Builder(
//                CHANNEL_ID_PLAYING, NotificationManagerCompat.IMPORTANCE_LOW)
//                .setName(getString(R.string.notification_channel_playing))
//                //.setDescription(getString(R.string.notification_channel_playing_description))
//                .setShowBadge(false)
//                .build();
//        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
//        mNotificationManager.createNotificationChannel(chan);
//
//        // Get the session's metadata
//        MediaControllerCompat controller = mediaSession.getController();
//        MediaMetadataCompat mediaMetadata = controller.getMetadata();
//        MediaDescriptionCompat description = mediaMetadata.getDescription();
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_PLAYING);
//
//        // Create an intent to launch the play talk activity when the notification is clicked
//        Intent playTalkIntent = new Intent(getApplicationContext(), PlayTalkActivity.class);
//        playTalkIntent.putExtra(TALK_DETAIL_EXTRA, (long) talk.getId());
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
//        stackBuilder.addNextIntentWithParentStack(playTalkIntent);
//        stackBuilder.addNextIntent(playTalkIntent);
//        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        builder
//                // Add the metadata for the currently playing track
//                .setContentTitle(description.getTitle())
//                .setContentText(description.getSubtitle())
//                .setSubText(description.getDescription())
//                .setLargeIcon(description.getIconBitmap())
//
//                // Enable launching the player by clicking the notification
//                .setContentIntent(resultPendingIntent)
//
//                // Stop the service when the notification is swiped away
//                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
//                        PlaybackStateCompat.ACTION_STOP))
//
//                // Make the transport controls visible on the lockscreen
//                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//
//                // Add an app icon and set its accent color
//                // Be careful about the color
//                .setSmallIcon(R.drawable.ic_notification_icon)
//                .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))
//
//                // Add a rewind button
//                .addAction(new NotificationCompat.Action(
//                        android.R.drawable.ic_media_rew,
//                        getString(R.string.rewind),
//                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
//                                PlaybackStateCompat.ACTION_REWIND)))
//
//                // Add a pause button
//                .addAction(new NotificationCompat.Action(
//                        android.R.drawable.ic_media_pause,
//                        getString(R.string.pause),
//                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
//                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))
//
//                // Add a fast forward button
//                .addAction(new NotificationCompat.Action(
//                        android.R.drawable.ic_media_ff,
//                        getString(R.string.fast_forward),
//                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
//                                PlaybackStateCompat.ACTION_FAST_FORWARD)))
//
//                // Take advantage of MediaStyle features
//                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
//                        .setMediaSession(mediaSession.getSessionToken())
//                        .setShowActionsInCompactView(0, 1, 2)
//
//                        // Add a cancel button
//                        .setShowCancelButton(true)
//                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
//                                PlaybackStateCompat.ACTION_STOP)));
//
//        // Display the notification and place the service in the foreground
//        startForeground(R.id.notification_playing, builder.build());
//
//        return START_STICKY;
//    }

//    @Nullable
//    @Override
//    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
//        return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
//    }

//    @Override
//    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
//        result.sendResult(null);
//    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }


    //    private final MediaSessionCompat.Callback sessionCallback = new MediaSessionCompat.Callback() {
//    private class SessionCallback implements MediaSession.Callback {
//
//        private static final String TAG = "SessionCallback";
//
//        @Override
//        public void onPlay() {
//            Log.d(TAG, "onPlay()");
//            mediaPlayer.prepare();
//            mediaPlayer.play();
//
//            // Start playback service
//            Intent intent = new Intent(PlaybackService.this, PlaybackService.class);
//            startService(intent);
//        }

//        @Override
//        public void onPlayFromMediaId(String mediaId, Bundle extras) {
//            Log.d(TAG, "onPlayFromMediaId: mediaId: " + mediaId + " extras: " + extras.toString());
//
//            // Look up talk metadata
//            int talkID = Integer.parseInt(mediaId);
//            Cursor cursor = PlayTalkActivity.getCursor(
//                    DBManager.getInstance(PlaybackService.this), talkID);
//            if (cursor.moveToFirst()) {
//                // convert DB result to an object
//                talk = new Talk(cursor, getApplicationContext());
//                talk.setId(talkID);
//            } else {
//                Log.e(LOG_TAG, "Could not look up talk, id="+talkID);
//                cursor.close();
//                return;
//            }
//            cursor.close();
//
//            // Set media session metadata
//            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
//            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, talk.getTeacherName());
//            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, talk.getTitle());
//            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, talk.getCenterName());
//            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (long)talk.getDurationInMinutes()*60*1000);
//            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, talk.getTitle());
//            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, talk.getTeacherName());
//            String photoFilename = talk.getPhotoFileName();
//            Log.i(LOG_TAG, "photoFilename: "+photoFilename);
//            try {
//                FileInputStream photo = openFileInput(photoFilename);
//                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeStream(photo));
//            } catch(FileNotFoundException e) {
//                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.dharmaseed_icon);
//                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, icon);
//            }
//            MediaMetadataCompat mediaMetadata = builder.build();
//            mediaSession.setMetadata(mediaMetadata);
//
//            // Start media session
//            mediaSession.setActive(true);
//
//            // Look up the URI of the media to play
//            String mediaUri;
//            if (talk.isDownloaded()) {
//                mediaUri = "file://" + talk.getPath();
//            } else {
//                mediaUri = talk.getAudioUrl();
//            }
//            MediaItem mediaItem = MediaItem.fromUri(mediaUri);
//            mediaPlayer.setMediaItem(mediaItem);
//
//            // Start playback service
//            Intent intent = new Intent(PlaybackService.this, PlaybackService.class);
//            startService(intent);
//
//            // Start playing the talk
//            mediaPlayer.prepare();
//            mediaPlayer.play();
//
//            // Set media session state
//            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
//                    .setState(PlaybackStateCompat.STATE_PLAYING,
//                            mediaPlayer.getCurrentPosition(),
//                            1)
//                    .setActions(sessionActions)
//                    .build()
//            );
//        }

//        @Override
//        public void onPlayFromSearch(String query, Bundle extras) {
//            Log.d(TAG, "onPlayFromSearch  query=" + query + " extras=" + extras.toString());
//
//            List<FeedItem> results = FeedSearcher.searchFeedItems(query, 0);
//            if (results.size() > 0 && results.get(0).getMedia() != null) {
//                FeedMedia media = results.get(0).getMedia();
//                startPlaying(media, false);
//                return;
//            }
//            onPlay();
//        }

//        @Override
//        public void onPause() {
//            Log.d(TAG, "onPause()");
//            mediaPlayer.pause();
//            stopForeground(false);
////            if (getStatus() == PlayerStatus.PLAYING) {
////                pause(!UserPreferences.isPersistNotify(), true);
////            }
//        }

//        @Override
//        public void onStop() {
//            Log.d(TAG, "onStop()");
//            mediaPlayer.stop();
//            stopSelf();
//            stopForeground(false);
//            mediaSession.setActive(false);
//        }

//        @Override
//        public void onSkipToPrevious() {
//            Log.d(TAG, "onSkipToPrevious()");
//            seekDelta(-UserPreferences.getRewindSecs() * 1000);
//        }

//        @Override
//        public void onRewind() {
//            Log.d(TAG, "onRewind()");
//            mediaPlayer.seekBack();
//        }
//
//        @Override
//        public void onFastForward() {
//            Log.d(TAG, "onFastForward()");
//            mediaPlayer.seekForward();
//        }

//        @Override
//        public void onSkipToNext() {
//            Log.d(TAG, "onSkipToNext()");
//            UiModeManager uiModeManager = (UiModeManager) getApplicationContext()
//                    .getSystemService(Context.UI_MODE_SERVICE);
//            if (UserPreferences.getHardwareForwardButton() == KeyEvent.KEYCODE_MEDIA_NEXT
//                    || uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
//                mediaPlayer.skip();
//            } else {
//                seekDelta(UserPreferences.getFastForwardSecs() * 1000);
//            }
//        }

//        @Override
//        public void onSeekTo(long pos) {
//            Log.d(TAG, "onSeekTo()");
//            mediaPlayer.seekTo(pos);
//        }
//
//        @Override
//        public void onSetPlaybackSpeed(float speed) {
//            Log.d(TAG, "onSetPlaybackSpeed()");
//            mediaPlayer.setPlaybackSpeed(speed);
//        }

//        @Override
//        public boolean onMediaButtonEvent(final Intent mediaButton) {
//            Log.d(TAG, "onMediaButtonEvent(" + mediaButton + ")");
//            if (mediaButton != null) {
//                KeyEvent keyEvent = mediaButton.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
//                if (keyEvent != null &&
//                        keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
//                        keyEvent.getRepeatCount() == 0) {
//                    return handleKeycode(keyEvent.getKeyCode(), false);
//                }
//            }
//            return false;
//        }
//
//        @Override
//        public void onCustomAction(String action, Bundle extra) {
//            Log.d(TAG, "onCustomAction(" + action + ")");
//            if (CUSTOM_ACTION_FAST_FORWARD.equals(action)) {
//                onFastForward();
//            } else if (CUSTOM_ACTION_REWIND.equals(action)) {
//                onRewind();
//            }
//        }
//    }

}