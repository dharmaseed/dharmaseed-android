package org.dharmaseed.androidapp;

import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Writes a talk to a file in external storage
 * @author jakewilson
 */
public abstract class TalkManager {

    // folder where talks are stored on the device
    public static final String DIR_NAME = "dharmaseed";
    public static final String FILE_PREFIX = "ds_";

    private static final String LOG_TAG = "TalkManager";

    // the key for the "location" field in the HTTP Header
    // it looks like most of the talks on dharma seed return a 301 or 302
    // to the _actual_ download link. So we need to follow this redirect
    // and download the file at that location
    public static final int HEADER_FIELD_KEY_LOCATION = 3;

    public static final Long FAILURE = -1l;

    public TalkManager() {}

    /**
     * Writes a talk to external storage
     * @param talk the talk to download
     * @return number of bytes downloaded or 0 on failure
     */
    public static Long download(Talk talk) {
        if (!isExternalStorageWritable())
            return FAILURE;

        File dir = getDir();
        if (dir == null)
            return FAILURE;

        String talkName = FILE_PREFIX + talk.getId() + "_" + talk.getTitle() + ".mp3";
        long size = 0;

        try {
            URL talkUrl = new URL(talk.getDownloadUrl());
            HttpURLConnection connection = (HttpURLConnection) talkUrl.openConnection();

            int response = connection.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                Log.e(LOG_TAG, "Talk " + talk.getId() + " URL returned " + response);
                return FAILURE;
            }

            String talkPath = dir.getPath() + "/" + talkName;

            InputStream inputStream = connection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(talkPath);

            int len;
            byte[] buffer = new byte[4096];

            // read file 4 kb at a time
            // TODO could this be made faster?
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
                fileOutputStream.write(buffer, 0, len);
                size += len;
            }

            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();

            talk.setPath(talkPath);
            return size;
        } catch (MalformedURLException murlex) {
            Log.e(LOG_TAG, murlex.getMessage());
        } catch (IOException ioex) {
            Log.e(LOG_TAG, ioex.getMessage());
        } catch (Exception ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }

        return FAILURE;
    }

    public static File getDir() {
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
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }

        Log.e(LOG_TAG, "External storage is not writable");
        return false;
    }

    /**
     * Deletes a talk from the file system
     * @param talk the talk to delete
     * @return whether the talk was deleted or not
     */
    public static boolean deleteTalk(Talk talk) {
        boolean result = true;
        File file = new File(talk.getPath());

        if (file.exists())
            result = file.delete();

        if (result)
            talk.setPath("");

        // if the file somehow didn't exist then we'll just say it was deleted anyway
        return result;
    }

}
