package org.dharmaseed.androidapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by bbethke on 2/11/16.
 */
// This tasks will fetch the latest data from dharmaseed.org and store it in the local database
abstract class DataFetcherTask extends AsyncTask<Void, Void, Void> {

    DBManager dbManager;
    SQLiteDatabase db;
    OkHttpClient httpClient;
    NavigationActivity navigationActivity;

    public DataFetcherTask(DBManager dbManager, NavigationActivity navigationActivity) {
        this.dbManager = dbManager;
        this.db = dbManager.getWritableDatabase();
        this.httpClient = new OkHttpClient();
        this.navigationActivity = navigationActivity;
    }

    protected void updateTable(String tableName, String tableID, String apiUrl, String[] itemKeys) {

        Cursor cursor = db.rawQuery("SELECT "+DBManager.C.Edition.EDITION+" FROM "
                +DBManager.C.Edition.TABLE_NAME
                +" WHERE "+DBManager.C.Edition.TABLE+"=\""+tableName+"\""
                , null);
        cursor.moveToFirst();
        String edition = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Edition.EDITION));
        cursor.close();
        Log.d("DataFetcherTask", "We have "+tableName+" edition: "+edition);

        // Get the IDs (but no details) of the items we don't yet have
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("detail", "0");
        if(edition != null && (!edition.equals(""))) {
            builder.addFormDataPart("edition", edition);
        }
        Request request = new Request.Builder()
                .url("http://www.dharmaseed.org/api/1/"+apiUrl)
                .post(builder.build())
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                JSONObject json = new JSONObject(response.body().string());
                String newEdition = json.getString("edition");
                JSONArray items = json.getJSONArray("items");
                Log.d("dataFetcher", "Retrieved "+tableName+" edition "+newEdition+". New items: "+items.length());

                // Fetch new items, starting with the latest ones
                ArrayList<Integer> itemIDs = new ArrayList<>();
                for(int i = 0; i < items.length(); i++) {
                    itemIDs.add(items.getInt(i));
                }
                Collections.sort(itemIDs, Collections.<Integer>reverseOrder());

                RequestAggregator agg = new RequestAggregator(100, tableName, tableID, apiUrl);
                JSONObject itemsWithDetails;
                for(Integer id : itemIDs) {
                    itemsWithDetails = agg.addID(id);
                    if(itemsWithDetails != null) {
                        dbManager.insertItems(itemsWithDetails, tableName, itemKeys);
                        publishProgress();
                    }
                }
                // Fire any last requests still in the aggregator
                itemsWithDetails = agg.fireRequest();
                if(itemsWithDetails != null) {
                    dbManager.insertItems(itemsWithDetails, tableName, itemKeys);
                }

                // Mark that we've successfully cached the new edition
                ContentValues values = new ContentValues();
                values.put(DBManager.C.Edition.TABLE, tableName);
                values.put(DBManager.C.Edition.EDITION, newEdition);
                db.insertWithOnConflict(DBManager.C.Edition.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                Log.i("dataFetcher", "Saved "+tableName+" edition "+newEdition);

            } else {
                Log.e("dataFetcher", "HTTP response unsuccessful, code " + response.code());
            }
        } catch (Exception e) {
            Log.e("dataFetcher", e.toString());
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        navigationActivity.updateDisplayedData();
    }

    private class RequestAggregator {
        private int size;
        private String table;
        private String tableID;
        private ArrayList<String> IDStrings;
        private String apiUrl;

        public RequestAggregator(int size, String table, String tableID, String apiUrl) {
            this.size = size;
            this.table = table;
            this.tableID = tableID;
            this.IDStrings = new ArrayList<>(size);
            this.apiUrl = apiUrl;
        }

        // Add a new ID to the request if it's not already present in the database.
        // Fire off the request if we've reached the size limit.
        // Returns the Response if the request was fired or null if the request was not fired.
        public JSONObject addID(int ID) {
            if(keyExists(ID)) {
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

            Log.d("fireRequest", "getting "+table+": "+IDStrings);
            MultipartBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("items", TextUtils.join(",", IDStrings))
                    .addFormDataPart("detail", "1")
                    .build();
            Request request = new Request.Builder()
                    .url("http://www.dharmaseed.org/api/1/"+apiUrl)
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
            Cursor cursor = db.rawQuery("SELECT 1 FROM "+table+" WHERE "+ tableID +"="+ID, null);
            boolean result = cursor.getCount() == 1;
            cursor.close();
            return result;
        }
    }

}
