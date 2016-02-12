package org.dharmaseed.androidapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by bbethke on 2/11/16.
 */
// This tasks will fetch the latest data from dharmaseed.org and store it in the local database
class DataFetcherTask extends AsyncTask<Void, Void, Void> {

    DBManager dbManager;
    SQLiteDatabase db;
    OkHttpClient httpClient;

    public DataFetcherTask(DBManager dbManager) {
        this.dbManager = dbManager;
        this.db = dbManager.getWritableDatabase();
        this.httpClient = new OkHttpClient();
    }

    @Override
    protected Void doInBackground(Void... params) {

        // Check the latest edition we have locally
        Cursor cursor = db.rawQuery("SELECT "+DBManager.C.Edition.EDITION+" FROM "+DBManager.C.Edition.TABLE_NAME
                +" WHERE "+DBManager.C.Edition.TABLE+"=\""+DBManager.C.Talk.TABLE_NAME+"\"", null);
        String talksEdition = null;
        if(cursor.moveToFirst()) {
            talksEdition = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Edition.EDITION));
        }
        cursor.close();
        Log.d("DataFetcherTask", "Talks edition: "+talksEdition);

        // Get the IDs (but no details) of the talks we don't yet have
        RequestBody body;
        if(talksEdition != null && (!talksEdition.equals(""))) {
            body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("edition", talksEdition)
                    .addFormDataPart("detail", "0")
                    .build();
        } else {
            body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("detail", "0")
                    .build();
        }
        Request request = new Request.Builder()
                .url("http://www.dharmaseed.org/api/1/talks/")
                .post(body)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                String newEdition = json.getString("edition");
                JSONArray talksJSON = json.getJSONArray("items");
                Log.d("dataFetcher", "Retrieved talks edition "+newEdition+". New talks: "+talksJSON.length());

                // Fetch new talks, starting with the latest ones
                ArrayList<Integer> talkIDs = new ArrayList<>();
                for(int i = 0; i < talksJSON.length(); i++) {
                    talkIDs.add(talksJSON.getInt(i));
                }
                Collections.sort(talkIDs, Collections.<Integer>reverseOrder());

                RequestAggregator agg = new RequestAggregator(100, DBManager.C.Talk.TABLE_NAME, DBManager.C.Talk.ID);
                for(Integer talkID : talkIDs) {
                    json = agg.addID(talkID);
                    if(json != null) {
                        dbManager.insertTalks(json);
                    }
                }
                // Fire any last requests still in the aggregator
                json = agg.fireRequest();
                if(json != null) {
                    dbManager.insertTalks(json);
                }

                // Mark that we've successfully cached the new edition
                ContentValues values = new ContentValues();
                values.put(DBManager.C.Edition.TABLE, DBManager.C.Talk.TABLE_NAME);
                values.put(DBManager.C.Edition.EDITION, newEdition);
                db.insertWithOnConflict(DBManager.C.Edition.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

            } else {
                Log.e("dataFetcher", "HTTP response unsuccessful, code " + response.code());
            }
        } catch (Exception e) {
            Log.e("dataFetcher", e.toString());
        }
        return null;
    }

    private class RequestAggregator {
        private int size;
        private String table;
        private String tableKeyName;
        private ArrayList<String> IDStrings;

        public RequestAggregator(int size, String table, String tableKeyName) {
            this.size = size;
            this.table = table;
            this.tableKeyName = tableKeyName;
            this.IDStrings = new ArrayList<>(size);
        }

        // Add a new ID to the request if it's not already present in the database.
        // Fire off the request if we've reached the size limit.
        // Returns the Response if the request was fired or null if the request was not fired.
        public JSONObject addID(int ID) {
            if(keyExists(ID)) {
                Log.d("addID", "use cache for " + ID);
                return null;
            } else {
                IDStrings.add(Integer.toString(ID));
                if(IDStrings.size() == size) {
                    return fireRequest();
                } else {
                    return null;
                }
            }
        }

        public JSONObject fireRequest() {
            if(IDStrings.isEmpty()) {
                return null;
            }

            Log.d("fireRequest", "getting talks "+IDStrings);
            MultipartBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("items", TextUtils.join(",", IDStrings))
                    .addFormDataPart("detail", "1")
                    .build();
            Request request = new Request.Builder()
                    .url("http://www.dharmaseed.org/api/1/talks/")
                    .post(body)
                    .build();

            // Reset state for the next request
            IDStrings.clear();

            try {
                Response response = httpClient.newCall(request).execute();
                if(response.isSuccessful()) {
                    return new JSONObject(response.body().string());
                } else {
                    return null;
                }
            } catch (Exception e) {
                Log.e("fireRequest", e.toString());
                return null;
            }
        }

        public boolean keyExists(int ID) {
            Cursor cursor = db.rawQuery("SELECT 1 FROM "+table+" WHERE "+tableKeyName+"="+ID, null);
            boolean result = cursor.getCount() == 1;
            cursor.close();
            return result;
        }
    }

}
