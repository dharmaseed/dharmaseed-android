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
    private int userSeekBarPosition;

    public TalkPlayerFragment() {

    }

    public TalkPlayerFragment(String url) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPrepared = false;
        userSeekBarPosition = 0;
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

    public void setUserSeekBarPosition(int userSeekBarPosition) {
        this.userSeekBarPosition = userSeekBarPosition;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i("talkPlayerFragment", "playing talk");
        mediaPrepared = true;
        mediaPlayer.seekTo(userSeekBarPosition);
        mediaPlayer.start();
        PlayTalkActivity activity = (PlayTalkActivity) getActivity();
        activity.setPPButton("ic_media_pause");
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
