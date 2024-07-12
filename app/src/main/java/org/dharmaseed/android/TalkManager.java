package org.dharmaseed.android;

import android.content.Context;
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
    public static final String DIR_NAME = "downloads";
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

        File dir = getDir(talk.getContext());
        if (dir == null)
            return FAILURE;

        File talkFile = getFile(talk);
        long size = 0;

        try {
            URL talkUrl = new URL(talk.getAudioUrl());
            Log.i(LOG_TAG, "Downloading " + talkUrl);
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

            InputStream inputStream = connection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(talkFile);

            int len;
            byte[] buffer = new byte[4096];

            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
                size += len;
            }

            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();

            Log.i(LOG_TAG, "Downloaded talk to " + talkFile.getPath());

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
    public static File getDir(Context context) {
        File file = new File(
                context.getFilesDir(),
                DIR_NAME
        );

        if (!file.exists() && !file.mkdirs()) {
            Log.e(LOG_TAG, "Failed to create all dirs needed to download talk");
            return null;
        }

        return file;
    }


    public static File getTalkFile(Context context, int talkId, String talkTitle)
    {
        // old file convention, which includes the full talk title
        File talkFile = new File(
                getDir(context),
                FILE_PREFIX + talkId + "_" + talkTitle + ".mp3"
        );

        // new file convention, which is the default for newly downloaded talks, does
        // not include the title to avoid issues with special characters (e.g. ?, * and whitespace)
        if (!talkFile.exists())
            talkFile = new File(
                    getDir(context),
                    FILE_PREFIX + talkId + ".mp3"
            );
        return talkFile;
    }

    /**
     * @return the file object for a downloaded talk
     */
    public static File getFile(Talk talk) {
        return getTalkFile(talk.getContext(),talk.getId(), talk.getTitle());
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
        File file = getFile(talk);

        if (file.exists())
            result = file.delete();

        // if the file somehow didn't exist then we'll just say it was deleted anyway
        return result;
    }

}
