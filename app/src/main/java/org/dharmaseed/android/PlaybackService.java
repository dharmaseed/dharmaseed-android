package org.dharmaseed.android;

import static androidx.media3.common.C.WAKE_MODE_NETWORK;
import static androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS;
import static androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.dharmaseed.android.PlayTalkActivity.SEEK_AMOUNT_MS;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

public class PlaybackService extends MediaSessionService {

    private static final String LOG_TAG = "PlaybackService";
    private static final String COMMAND_SEEK_BACK = "seek_back";
    private static final String COMMAND_SEEK_FORWARD = "seek_forward";

    private MediaSession mediaSession;
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
                .setSeekBackIncrementMs(SEEK_AMOUNT_MS)
                .setSeekForwardIncrementMs(SEEK_AMOUNT_MS)
                .build();

        // Create seek back/forward command buttons
        CommandButton seekBackButton = new CommandButton.Builder()
                .setDisplayName("Seek Back")
                .setIconResId(R.drawable.ic_seek_back)
                .setSessionCommand(new SessionCommand(COMMAND_SEEK_BACK, new Bundle()))
                .build();

        CommandButton seekForwardButton = new CommandButton.Builder()
                .setDisplayName("Seek Forward")
                .setIconResId(R.drawable.ic_seek_forward)
                .setSessionCommand(new SessionCommand(COMMAND_SEEK_FORWARD, new Bundle()))
                .build();

        // Create a media session with custom command buttons
        mediaSession = new MediaSession.Builder(this, mediaPlayer)
                .setCallback(new SessionCallback())
                .setCustomLayout(ImmutableList.of(seekBackButton, seekForwardButton))
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

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        // Stop the MediaSessionService when the app is closed in the background
        // (work around broken notification issue)
        Player player = mediaSession.getPlayer();
        if (player.getPlayWhenReady()) {
            // Make sure the service is not in foreground.
            player.pause();
        }
        stopSelf();
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
            notification.notification.contentIntent = PendingIntent.getActivity(getApplicationContext(),
                    0, playTalkIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            return notification;
        }
    }

    private class SessionCallback implements MediaSession.Callback {

        @OptIn(markerClass = UnstableApi.class) @Override
        public MediaSession.ConnectionResult onConnect(
                MediaSession session, MediaSession.ControllerInfo controller) {

            // Add seek forward/back commands to media notification
            SessionCommands sessionCommands = new SessionCommands.Builder()
                    .add(new SessionCommand(COMMAND_SEEK_BACK, new Bundle()))
                    .add(new SessionCommand(COMMAND_SEEK_FORWARD, new Bundle()))
                    .build();

            // Remove default seek-to-previous commands from media notification
            Player.Commands playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                    .buildUpon()
                    .removeAll(COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build();

            return new MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(playerCommands)
                    .setAvailableSessionCommands(sessionCommands)
                    .build();
        }

        @Override
        public ListenableFuture<SessionResult> onCustomCommand(MediaSession session,
                MediaSession.ControllerInfo controller, SessionCommand customCommand, Bundle args) {

            final SessionResult result;
            if (customCommand.customAction.equals(COMMAND_SEEK_BACK)) {
                session.getPlayer().seekBack();
                result = new SessionResult(SessionResult.RESULT_SUCCESS);
            } else if (customCommand.customAction.equals(COMMAND_SEEK_FORWARD)) {
                session.getPlayer().seekForward();
                result = new SessionResult(SessionResult.RESULT_SUCCESS);
            } else {
                result = new SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED);
            }
            return Futures.immediateFuture(result);
        }

        @Override
        public ListenableFuture<List<MediaItem>> onAddMediaItems(MediaSession mediaSession, MediaSession.ControllerInfo controller, List<MediaItem> mediaItems) {

            Talk talk;
            Vector<MediaItem> resolvedItems = new Vector<>();

            for (MediaItem item : mediaItems) {

                // Look up talk from its ID
                int talkID = Integer.parseInt(item.mediaId);
                talk = Talk.lookup(DBManager.getInstance(PlaybackService.this),
                        getApplicationContext(), talkID);

                // Look up teacher photo
                Bitmap photo = FileManager.findPhoto(PlaybackService.this, talk.getTeacherId());
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
                DBManager dbManager = DBManager.getInstance(PlaybackService.this);
                if (talk.isDownloaded(dbManager)) {
                    mediaUri = "file://" + talk.getPath();
                } else {
                    mediaUri = talk.getAudioUrl();
                }

                Log.d(LOG_TAG, "adding media item for URI '" + mediaUri + "'");

                resolvedItems.add(item.buildUpon()
                        .setUri(mediaUri)
                        .setMediaMetadata(mediaMetadata)
                        .build());
            }

            return immediateFuture(resolvedItems);
        }

    }


}