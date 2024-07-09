package org.dharmaseed.android;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.ResponseBody;

/**
 * Manages downloaded talks
 * @author jakewilson
 */
public abstract class FileManager {

    // folder where talks are stored on the device
    public static final String TALK_DIR_NAME = "downloads";
    public static final String TALK_FILE_PREFIX = "ds_";

    public static final String TEACHER_PHOTO_ASSET_DIR = "teacher_photos";

    private static final String LOG_TAG = "FileManager";

    public static final Long FAILURE = -1l;

    public FileManager() {}


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
                TALK_DIR_NAME
        );

        if (!file.exists() && !file.mkdirs()) {
            Log.e(LOG_TAG, "Failed to create all dirs needed to download talk");
            return null;
        }

        return file;
    }


    public static File getTalkFile(Context context, int talkId, String talkTitle)
    {
        return new File(
                getDir(context),
                TALK_FILE_PREFIX + talkId + "_" + talkTitle + ".mp3"
        );
    }

    /**
     * @return the directory where we store talks
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

    protected static String getPhotoFileName(int teacherID)
    {
        return "teacher-"+teacherID+".png";
    }

    protected interface StreamFactory {
        InputStream run() throws IOException;
    }

    protected static Bitmap getPhoto(StreamFactory f, int teacherID) {
        Bitmap photo;
        try {
            InputStream input = f.run();
            photo = BitmapFactory.decodeStream(input);
            input.close();
        } catch (java.io.IOException e) {
            photo = null;
        }
        return photo;
    }

    public static Bitmap getPhoto(Context context, int teacherID) {
        File photoFile = new File(context.getFilesDir(), getPhotoFileName(teacherID));
        return getPhoto(() -> new FileInputStream(photoFile), teacherID);
    }

    public static Bitmap getAssetPhoto(Context context, int teacherID)
    {
        File teacher_photo = new File(TEACHER_PHOTO_ASSET_DIR, getPhotoFileName(teacherID));
        return getPhoto(() -> context.getAssets().open(teacher_photo.getPath()), teacherID);
    }

    public static Bitmap findPhoto(Context context, int teacherID, boolean defaultIcon) {
        Bitmap photo = getPhoto(context, teacherID);

        // try to get the picture from the assets folder
        if (photo == null)
            photo = getAssetPhoto(context, teacherID);

        // default to the DS icon
        if (photo == null && defaultIcon)
            photo = BitmapFactory.decodeResource(
                    context.getResources(),
                    R.drawable.dharmaseed_icon
            );

        return photo;
    }

    public static Bitmap findPhoto(Context context, int teacherID) {
        return findPhoto(context, teacherID, true);
    }

    public static boolean deletePhoto(Context context, int teacherID) {
        File photoFile = new File(context.getFilesDir(), getPhotoFileName(teacherID));
        return photoFile.delete();
    }

    public static void setPhoto(Context context, int teacherID, byte[] photo) throws java.io.IOException {
        FileOutputStream outputStream = context.openFileOutput(
                getPhotoFileName(teacherID),
                Context.MODE_PRIVATE
        );
        outputStream.write(photo);
        outputStream.close();
    }
}
