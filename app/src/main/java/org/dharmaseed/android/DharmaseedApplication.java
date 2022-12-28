package org.dharmaseed.android;

import android.app.Application;
import android.content.Context;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

public class DharmaseedApplication extends Application {

    private ExoPlayer mediaPlayer = null;

    ExoPlayer getMediaPlayer()
    {
        if (mediaPlayer == null) {
            Context context = getApplicationContext();
            DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true);
            mediaPlayer = new ExoPlayer.Builder(context).
                    setMediaSourceFactory(
                            new DefaultMediaSourceFactory(context)
                                    .setDataSourceFactory(httpFactory))
                    .build();
        }
        return mediaPlayer;
    }
}
