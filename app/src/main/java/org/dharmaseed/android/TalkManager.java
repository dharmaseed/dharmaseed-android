package org.dharmaseed.android;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manages downloaded talks
 * @author jakewilson
 */
public abstract class TalkManager {

    // folder where talks are stored on the device
    public static final String DIR_NAME = "dharmaseed";
    public static final String FILE_PREFIX = "ds_";

    private static final String LOG_TAG = "TalkManager";

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
            URL talkUrl = new URL(talk.getAudioUrl());
            HttpURLConnection connection = (HttpURLConnection) talkUrl.openConnection();

            int response = connection.getResponseCode();
            while (response != HttpURLConnection.HTTP_OK &&
                    (response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_MOVED_TEMP)) {
                talkUrl = new URL(connection.getHeaderField("Location"));
                connection = (HttpURLConnection) talkUrl.openConnection();
                Log.i(LOG_TAG, "Following redirect to " + talkUrl);
                response = connection.getResponseCode();
            }

            if (response != HttpURLConnection.HTTP_OK) {
                Log.e(LOG_TAG, "Talk URL " + connection.getURL() + " returned " + response);
                return FAILURE;
            }

            String talkPath = dir.getPath() + "/" + talkName;

            InputStream inputStream = connection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(talkPath);

            int len;
            byte[] buffer = new byte[4096];

            while ((len = inputStream.read(buffer)) != -1) {
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

    /**
     * @return the directory where we store talks
     */
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
    public static boolean delete(Talk talk) {
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
