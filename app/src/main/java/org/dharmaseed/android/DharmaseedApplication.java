package org.dharmaseed.android;

// TODO Delete me

import static androidx.media3.common.C.USAGE_MEDIA;
import static androidx.media3.common.C.WAKE_MODE_NETWORK;

import android.app.Application;
import android.content.Context;

import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;

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
