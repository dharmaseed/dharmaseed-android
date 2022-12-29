package org.dharmaseed.android;

// TODO Delete me

import static com.google.android.exoplayer2.C.USAGE_MEDIA;
import static com.google.android.exoplayer2.C.WAKE_MODE_NETWORK;

import android.app.Application;
import android.content.Context;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

public class DharmaseedApplication extends Application {

    private ExoPlayer mediaPlayer = null;

    ExoPlayer getMediaPlayer()
    {
        if (mediaPlayer == null) {
            Context context = getApplicationContext();
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
                    .build();
        }
        return mediaPlayer;
    }
}
