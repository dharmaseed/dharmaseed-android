package org.dharmaseed.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * A mini talk player fragment that can be embedded into other activities.
 */
public class MiniPlayerFragment extends Fragment {

    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;

    private String LOG_TAG = "MiniPlayerFragment";

    @Override
    public void onResume() {
        super.onResume();

        // Create the media controller
        Context ctx = getContext();
        SessionToken sessionToken =
                new SessionToken(ctx, new ComponentName(ctx, PlaybackService.class));
        controllerFuture = new MediaController.Builder(ctx, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                mediaController.addListener(playerListener);
                playerListener.onMediaMetadataChanged(mediaController.getMediaMetadata());
                playerListener.onPlaybackStateChanged(mediaController.getPlaybackState());
                playerListener.onIsPlayingChanged(mediaController.isPlaying());
            } catch (InterruptedException | ExecutionException | CancellationException e) {
                Log.e(LOG_TAG, "Could not create media controller. " + e.toString());
            }
        }, ContextCompat.getMainExecutor(ctx));

    }

    @Override
    public void onPause() {
        super.onPause();
        MediaController.releaseFuture(controllerFuture);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        ImageButton playButton = (ImageButton) view.findViewById(R.id.mini_player_play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButtonClicked(v);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaItem item = mediaController.getCurrentMediaItem();
                if (item != null) {
                    Intent intent = new Intent(getContext(), PlayTalkActivity.class);
                    intent.putExtra(NavigationActivity.TALK_DETAIL_EXTRA,
                            (long) Integer.parseInt(item.mediaId));
                    getContext().startActivity(intent);
                }
            }
        });

        return view;
    }

    public void playButtonClicked(View view) {
        if (mediaController != null && mediaController.getPlaybackState() != Player.STATE_IDLE) {
            if (mediaController.isPlaying()) {
                mediaController.pause();
            } else {
                mediaController.play();
            }
        }
    }

    private final Player.Listener playerListener =
            new Player.Listener() {

                private void setPPButton(String drawableName) {
                    Log.i(LOG_TAG, "setPPButton " + this);
                    ImageButton playButton = (ImageButton) getView().findViewById(R.id.mini_player_play_button);
                    playButton.setImageDrawable(ContextCompat.getDrawable(getContext(),
                            getResources().getIdentifier(drawableName, "drawable", "android")));
                }

                private boolean viewingCurrentlyPlayingTalk() {
                    FragmentActivity activity = getActivity();
                    MediaItem currentMediaItem = mediaController.getCurrentMediaItem();
                    return activity.getClass() == PlayTalkActivity.class &&
                            currentMediaItem != null &&
                            Integer.parseInt(currentMediaItem.mediaId) == ((PlayTalkActivity)activity).talkID;
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    // Update Play/Pause button
                    if (isPlaying) {
                        setPPButton("ic_media_pause");
                    } else {
                        setPPButton("ic_media_play");
                    }
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_IDLE || viewingCurrentlyPlayingTalk()) {
                        getView().setVisibility(View.GONE);
                    } else {
                        getView().setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {

                    // Set talk title
                    TextView title = (TextView) getView().findViewById(R.id.mini_player_title);
                    title.setText(mediaMetadata.title);

                    // Set talk teacher
                    TextView teacher = (TextView) getView().findViewById(R.id.mini_player_teacher);
                    teacher.setText(mediaMetadata.artist);

                    // Set talk photo
                    ImageView photo = (ImageView) getView().findViewById(R.id.mini_player_talk_photo);
                    try {
                        ByteArrayInputStream is = new ByteArrayInputStream(mediaMetadata.artworkData);
                        Drawable drw = Drawable.createFromStream(is, "articleImage");
                        photo.setImageDrawable(drw);
                    } catch (Exception e) {
                        Drawable icon = ContextCompat.getDrawable(getContext(), R.drawable.dharmaseed_icon);
                        photo.setImageDrawable(icon);
                    }

                    // Hide the player fragment if we're viewing the PlayTalkActivity of the currently-playing talk
                    if (viewingCurrentlyPlayingTalk()) {
                            getView().setVisibility(View.GONE);
                    }
                }
            };

}