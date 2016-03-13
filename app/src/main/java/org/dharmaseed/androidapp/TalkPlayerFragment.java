package org.dharmaseed.androidapp;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * Created by bbethke on 3/12/16.
 * This fragment retains a reference to the MediaPlayer used to play the talk,
 * so that the talk will keep playing on configuration changes like screen rotation
 * See http://developer.android.com/guide/topics/resources/runtime-changes.html#RetainingAnObject
 */
public class TalkPlayerFragment extends Fragment
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private MediaPlayer mediaPlayer;
    private boolean mediaPrepared;

    public TalkPlayerFragment() {

    }

    public TalkPlayerFragment(String url) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPrepared = false;
    }

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public boolean getMediaPrepared() {
        return mediaPrepared;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i("talkPlayerFragment", "playing talk");
        mediaPrepared = true;
        mediaPlayer.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e("mediaError", what + "," + extra);
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("talkPlayerFragment", "Destroying mediaPlayer");
        mediaPlayer.stop();
        mediaPlayer.release();
    }
}
