package org.dharmaseed.android;

import static androidx.media3.common.C.WAKE_MODE_NETWORK;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSession.ControllerInfo;
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition;
import androidx.media3.session.MediaLibraryService;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.ArrayList;

public class PlaybackService extends MediaLibraryService {

    private static final String LOG_TAG = "PlaybackService";
    private MediaLibrarySession mediaSession;
    private Runnable updateTalkProgressTask;
    private Handler handler;

    @Override
    @OptIn(markerClass = UnstableApi.class)
    public void onCreate() {
        super.onCreate();

        // Create the media player
        Context context = this;
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true);
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                context, httpFactory
        );

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .build();

        ExoPlayer mediaPlayer = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(context)
                                .setDataSourceFactory(dataSourceFactory))
                .setWakeMode(WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .setUseLazyPreparation(true)
                .setAudioAttributes(audioAttributes, true)
                .build();

        // Create a media session
        mediaSession = new MediaLibrarySession.Builder(this, mediaPlayer, new SessionCallback())
                .build();

        // Set a custom media notification
        setMediaNotificationProvider(new NotificationProvider(this));

        // Set a timer to periodically update the talk progress in the database
        handler = new Handler();
        updateTalkProgressTask = new Runnable() {
            @Override
            public void run() {
                updateTalkProgress();
            }
        };
        handler.postDelayed(updateTalkProgressTask, 10000);
    }

    // Periodic task that saves talk progress in the database
    private void updateTalkProgress() {
        if (mediaSession != null) {
            final Player player = mediaSession.getPlayer();
            final MediaItem item = player.getCurrentMediaItem();
            if (item != null && player.getPlaybackState() != Player.STATE_IDLE) {
                final int talkID = Integer.parseInt(item.mediaId);
                final double progress = player.getCurrentPosition() / (1000 * 60.0);
                SimpleDateFormat parser = new SimpleDateFormat(Talk.DATE_FORMAT);
                final String now = parser.format(GregorianCalendar.getInstance().getTime());
                DBManager.getInstance(this).setTalkProgress(talkID, now, progress);
            }

            // Only run this task again if the mediaSession is not null. If the service has been destroyed,
            // we'll set mediaSession = null, so we'll stop running the task in that case.
            handler.postDelayed(updateTalkProgressTask, 10000);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.getPlayer().release();
        mediaSession.release();
        mediaSession = null;
    }

    /*
    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        Player player = mediaSession.getPlayer();
        if (player.getPlayWhenReady()) {
            // Make sure the service is not in foreground.
            player.pause();
        }
        stopSelf();
    }
    */

    @Nullable
    @Override
    public MediaLibrarySession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @OptIn(markerClass = UnstableApi.class)
    private class NotificationProvider implements MediaNotification.Provider {

        private DefaultMediaNotificationProvider defaultProvider;

        @OptIn(markerClass = UnstableApi.class)
        public NotificationProvider(Context context) {
            defaultProvider = new DefaultMediaNotificationProvider.Builder(context).build();
            defaultProvider.setSmallIcon(R.drawable.ic_notification_icon);
        }

        @Override
        public boolean handleCustomCommand(MediaSession session, String action, Bundle extras) {
            return defaultProvider.handleCustomCommand(session, action, extras);
        }

        @Override
        public MediaNotification createNotification(MediaSession mediaSession, ImmutableList<CommandButton> customLayout, MediaNotification.ActionFactory actionFactory, Callback onNotificationChangedCallback) {
            MediaNotification notification = defaultProvider.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback);

            // Create an intent to launch the play talk activity when the notification is clicked
            Intent playTalkIntent = new Intent(getApplicationContext(), PlayTalkActivity.class);
            playTalkIntent.putExtra(NavigationActivity.TALK_DETAIL_EXTRA,
                    (long) Integer.parseInt(mediaSession.getPlayer().getCurrentMediaItem().mediaId));
            notification.notification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                    0, playTalkIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            return notification;
        }
    }

    private class SessionCallback implements MediaLibrarySession.Callback {

        protected MediaItem resolveMediaItem(MediaItem item)
        {
            // Look up talk from its ID
            int talkID = Integer.parseInt(item.mediaId);
            Talk talk = Talk.lookup(DBManager.getInstance(PlaybackService.this),
                    getApplicationContext(), talkID);

            // Look up teacher photo
            String photoFilename = talk.getPhotoFileName();
            Bitmap photo;
            try {
                FileInputStream photoStream = openFileInput(photoFilename);
                photo = BitmapFactory.decodeStream(photoStream);
                photoStream.close();
            } catch (IOException e) {
                photo = BitmapFactory.decodeResource(getResources(), R.drawable.dharmaseed_icon);
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            // Set media session metadata
            MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                    .setArtist(talk.getTeacherName())
                    .setTitle(talk.getTitle())
                    .setAlbumTitle(talk.getCenterName())
                    .setDisplayTitle(talk.getTitle())
                    .setSubtitle(talk.getTeacherName())
                    .setArtworkData(byteArray, MediaMetadata.PICTURE_TYPE_ARTIST_PERFORMER)
                    .build();

            // Look up the URI of the media to play
            String mediaUri;
            if (talk.isDownloaded()) {
                mediaUri = "file://" + talk.getPath();
            } else {
                mediaUri = talk.getAudioUrl();
            }

            return item.buildUpon()
                    .setUri(mediaUri)
                    .setMediaMetadata(mediaMetadata)
                    .build();
        }

        @Override
        public ListenableFuture<List<MediaItem>> onAddMediaItems(MediaSession mediaSession, MediaSession.ControllerInfo controller, List<MediaItem> mediaItems) {

            ArrayList<MediaItem> resolvedItems = new ArrayList<>();

            for (MediaItem item : mediaItems)
                resolvedItems.add(resolveMediaItem(item));

            return immediateFuture(resolvedItems);
        }

        @OptIn(markerClass = UnstableApi.class)
        protected MediaItemsWithStartPosition restorePlaylist()
        {
            DBManager dbManager = DBManager.getInstance(PlaybackService.this);
            int talkId = dbManager.getLastTalkId();
            String talkIDString = Integer.toString(talkId);
            MediaItem item = new MediaItem.Builder().setMediaId(talkIDString).build();
            List<MediaItem> items = new ArrayList<>();
            items.add(resolveMediaItem(item));
            int startPositionMs = (int)(60*1000*dbManager.getTalkProgress(talkId));
            return new MediaItemsWithStartPosition(items, 0, startPositionMs);
        }

        @OptIn(markerClass = UnstableApi.class)
        @Override
        public ListenableFuture<MediaItemsWithStartPosition> onPlaybackResumption(
                MediaSession mediaSession,
                ControllerInfo controller
        ) {
            Log.d(LOG_TAG, "entered onPlaybackResumption");
            SettableFuture<MediaItemsWithStartPosition> settableFuture = SettableFuture.create();
            settableFuture.addListener(() -> {
                Log.d(LOG_TAG, "creating resumption playlist");
                MediaItemsWithStartPosition resumptionPlaylist = restorePlaylist();
                settableFuture.set(resumptionPlaylist);
            }, MoreExecutors.directExecutor());
            return settableFuture;
        }
    }


}