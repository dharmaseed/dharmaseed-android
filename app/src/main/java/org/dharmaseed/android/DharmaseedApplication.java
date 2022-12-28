package org.dharmaseed.android;

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
                    .build();
        }
        return mediaPlayer;
    }
}
