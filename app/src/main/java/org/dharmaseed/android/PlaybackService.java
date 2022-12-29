package org.dharmaseed.android;

import android.app.Service;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.List;

public class PlaybackService extends MediaBrowserServiceCompat {
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";
    private static final String LOG_TAG = "PlaybackService";
    public static final String CHANNEL_ID_PLAYING = "playing";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(getApplicationContext(), LOG_TAG);

//        // Enable callbacks from MediaButtons and TransportControls
//        mediaSession.setFlags(
//                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
//                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(new SessionCallback());

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");

        // Given a media session and its context (usually the component containing the session)
// Create a NotificationCompat.Builder
        Context context = this;

// Get the session's metadata
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_PLAYING);

        builder
                // Add the metadata for the currently playing track
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())

                // Enable launching the player by clicking the notification
                .setContentIntent(controller.getSessionActivity())

                // Stop the service when the notification is swiped away
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                        PlaybackStateCompat.ACTION_STOP))

                // Make the transport controls visible on the lockscreen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // Add an app icon and set its accent color
                // Be careful about the color
                .setSmallIcon(R.drawable.dharmaseed_icon)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))

                // Add a pause button
                .addAction(new NotificationCompat.Action(
                        getResources().getIdentifier(
                                "ic_media_pause",
                                "drawable",
                                "android"),
                        getString(R.string.pause),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))

                // Take advantage of MediaStyle features
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)

                        // Add a cancel button
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                PlaybackStateCompat.ACTION_STOP)));

        // Display the notification and place the service in the foreground
        startForeground(R.id.notification_playing, builder.build());

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }



//    private final MediaSessionCompat.Callback sessionCallback = new MediaSessionCompat.Callback() {
    private class SessionCallback extends MediaSessionCompat.Callback {

        private static final String TAG = "SessionCallback";

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay()");
//            PlayerStatus status = getStatus();
//            if (status == PlayerStatus.PAUSED || status == PlayerStatus.PREPARED) {
//                resume();
//            } else if (status == PlayerStatus.INITIALIZED) {
//                setStartWhenPrepared(true);
//                prepare();
//            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "onPlayFromMediaId: mediaId: " + mediaId + " extras: " + extras.toString());
//            FeedMedia p = DBReader.getFeedMedia(Long.parseLong(mediaId));
//            if (p != null) {
//                startPlaying(p, false);
//            }
        }

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

        @Override
        public void onPause() {
            Log.d(TAG, "onPause()");
//            if (getStatus() == PlayerStatus.PLAYING) {
//                pause(!UserPreferences.isPersistNotify(), true);
//            }
        }

        @Override
        public void onStop() {
            Log.d(TAG, "onStop()");
//            mediaPlayer.stopPlayback(true);
        }

//        @Override
//        public void onSkipToPrevious() {
//            Log.d(TAG, "onSkipToPrevious()");
//            seekDelta(-UserPreferences.getRewindSecs() * 1000);
//        }

        @Override
        public void onRewind() {
            Log.d(TAG, "onRewind()");
//            seekDelta(-UserPreferences.getRewindSecs() * 1000);
        }

        @Override
        public void onFastForward() {
            Log.d(TAG, "onFastForward()");
//            seekDelta(UserPreferences.getFastForwardSecs() * 1000);
        }

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

        @Override
        public void onSeekTo(long pos) {
            Log.d(TAG, "onSeekTo()");
//            seekTo((int) pos);
        }

        @Override
        public void onSetPlaybackSpeed(float speed) {
            Log.d(TAG, "onSetPlaybackSpeed()");
//            setSpeed(speed);
        }

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
    };

}