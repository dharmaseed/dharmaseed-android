package org.dharmaseed.android;

import static androidx.media3.common.C.WAKE_MODE_NETWORK;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaNotification;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Vector;

public class PlaybackService extends MediaSessionService {

    private static final String LOG_TAG = "PlaybackService";
    private MediaSession mediaSession;

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
        ExoPlayer mediaPlayer = new ExoPlayer.Builder(context)
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

        // Set a custom media notification
        setMediaNotificationProvider(new NotificationProvider(this));

    }

    @Override
    public void onDestroy() {
        mediaSession.getPlayer().release();
        mediaSession.release();
        mediaSession = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
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
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addNextIntentWithParentStack(playTalkIntent);
            stackBuilder.addNextIntent(playTalkIntent);
            notification.notification.contentIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            return notification;
        }
    }

    private class SessionCallback implements MediaSession.Callback {

        @Override
        public ListenableFuture<List<MediaItem>> onAddMediaItems(MediaSession mediaSession, MediaSession.ControllerInfo controller, List<MediaItem> mediaItems) {

            Talk talk;
            Vector<MediaItem> resolvedItems = new Vector<>();

            for (MediaItem item : mediaItems) {

                // Look up talk from its ID
                int talkID = Integer.parseInt(item.mediaId);
                Cursor cursor = PlayTalkActivity.getCursor(
                        DBManager.getInstance(PlaybackService.this), talkID);
                if (cursor.moveToFirst()) {
                    // convert DB result to an object
                    talk = new Talk(cursor, getApplicationContext());
                    talk.setId(talkID);
                } else {
                    Log.e(LOG_TAG, "Could not look up talk, id=" + talkID);
                    cursor.close();
                    continue;
                }
                cursor.close();

                // Look up teacher photo
                String photoFilename = talk.getPhotoFileName();
                Bitmap photo;
                try {
                    FileInputStream photoStream = openFileInput(photoFilename);
                    photo = BitmapFactory.decodeStream(photoStream);
                } catch (FileNotFoundException e) {
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

                resolvedItems.add(item.buildUpon()
                        .setUri(mediaUri)
                        .setMediaMetadata(mediaMetadata)
                        .build());
            }

            return immediateFuture(resolvedItems);
        }

    }


}