package org.dharmaseed.androidapp;

import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;

/**
 * Writes a talk to a file in external storage
 * @author jakewilson
 */
public class TalkDownloader {

    // folder where talks are stored on the device
    public static final String DIR_NAME = "dharmaseed";

    private static final String LOG_TAG = "TalkDownloader";

    public TalkDownloader() {}

    /**
     * Writes a talk to external storage
     * @param talk the talk to download
     * @return whether the download was successful or not
     */
    public boolean download(Talk talk) {
        if (!isExternalStorageWritable())
            return false;

        File file = getDir();
        if (file == null)
            return false;

        return true;
    }

    public File getDir() {
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                DIR_NAME
        );

        if (!file.exists() && !file.mkdirs()) {
            Log.e(LOG_TAG, "Failed to create all dirs needed to download talk");
            return null;
        }

        return file;
    }

    /**
     * Make sure we can write to external storage
     * @return if external storage is writable
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }

        Log.e(LOG_TAG, "External storage is not writable");
        return false;
    }

}
