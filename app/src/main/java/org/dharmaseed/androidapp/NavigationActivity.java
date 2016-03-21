/*
 *     Dharmaseed Android app
 *     Copyright (C) 2016  Brett Bethke
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.dharmaseed.androidapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        TextView.OnEditorActionListener,
        View.OnFocusChangeListener {

    public final static String TALK_DETAIL_EXTRA = "org.dharmaseed.androidapp.TALK_DETAIL";

    ListView talkListView;
    EditText searchBox;
    boolean starFilterOn;
    DBManager dbManager;
    TalkListViewAdapter talkListCursorAdapter;
    SwipeRefreshLayout refreshLayout;

    TalkFetcherTask talkFetcherTask;
    TeacherFetcherTask teacherFetcherTask;
    CenterFetcherTask centerFetcherTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Talks");

        dbManager = new DBManager(this);

        searchBox = (EditText)findViewById(R.id.nav_search_text);
        searchBox.setOnEditorActionListener(this);
        searchBox.setOnFocusChangeListener(this);
        starFilterOn = false;

        // Configure talks list view
        talkListView = (ListView) findViewById(R.id.talks_list_view);
        talkListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Log.d("onItemClick", "selected " + position + ", " + id);
                        Context ctx = parent.getContext();
                        Intent intent = new Intent(ctx, PlayTalkActivity.class);
                        intent.putExtra(TALK_DETAIL_EXTRA, id);
                        ctx.startActivity(intent);
                    }
                }
        );
        talkListCursorAdapter = new TalkListViewAdapter(
                this,
                R.layout.talk_list_view_item,
                null
        );
        talkListView.setAdapter(talkListCursorAdapter);

        // Set swipe refresh listener
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.talks_list_view_swipe_refresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i("refresh", "onrefresh");
                fetchNewDataFromServer();
            }
        });

        // Get the latest data from dharmaseed.org
        fetchNewDataFromServer();
        updateDisplayedData();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("navigationActivity", "Received update broadcast");
                updateDisplayedData();
            }
        }, new IntentFilter("updateDisplayedData"));

    }

    public void fetchNewDataFromServer() {

        // Fetch new data from the server
        Log.i("navigationActivity", "fetchNewDataFromServer()");
        if(teacherFetcherTask == null || teacherFetcherTask.getStatus() == AsyncTask.Status.FINISHED) {
            refreshLayout.setRefreshing(true);
            teacherFetcherTask = new TeacherFetcherTask(dbManager, this, this);
            teacherFetcherTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        if(centerFetcherTask == null || centerFetcherTask.getStatus() == AsyncTask.Status.FINISHED) {
            refreshLayout.setRefreshing(true);
            centerFetcherTask = new CenterFetcherTask(dbManager, this);
            centerFetcherTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        if(talkFetcherTask == null || talkFetcherTask.getStatus() == AsyncTask.Status.FINISHED) {
            refreshLayout.setRefreshing(true);
            talkFetcherTask = new TalkFetcherTask(dbManager, this);
            talkFetcherTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_settings:
                Log.i("nav", "Settings!");
                return true;

            case R.id.action_refresh_server_data:
                fetchNewDataFromServer();
                return true;

            case R.id.action_search:
                Log.i("nav", "Search!");
                EditText searchBox = (EditText) findViewById(R.id.nav_search_text);
                if (searchBox.getVisibility() == View.GONE) {
                    searchBox.setVisibility(View.VISIBLE);
                    searchBox.requestFocus();
                    searchBox.setCursorVisible(true);
                    InputMethodManager keyboard = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.showSoftInput(searchBox, 0);
                } else {
                    searchBox.setVisibility(View.GONE);
                    searchBox.setText("");
                    updateDisplayedData();
                }
                return true;

            case R.id.action_toggle_starred:
                int starOn  = getResources().getIdentifier("btn_star_big_on", "drawable", "android");
                int starOff = getResources().getIdentifier("btn_star_big_off", "drawable", "android");
                starFilterOn = ! starFilterOn;
                if (starFilterOn) {
                    item.setIcon(ContextCompat.getDrawable(this, starOn));
                } else {
                    item.setIcon(ContextCompat.getDrawable(this, starOff));
                }
                updateDisplayedData();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_talks) {
            getSupportActionBar().setTitle("Talks");
        } else if (id == R.id.nav_teachers) {
            getSupportActionBar().setTitle("Teachers");
        } else if (id == R.id.nav_centers) {
            getSupportActionBar().setTitle("Centers");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        Log.i("onEditorAction", v.getText().toString());

        // Close keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        // Give up focus (which will invoke onFocusChange below to hide the cursor and keyboard)
        v.clearFocus();

        // Search for talks meeting the new criteria
        updateDisplayedData();

        return false;
    }

    // Search box edit text focus listener
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        Log.i("focusChange", hasFocus+"");
        if (hasFocus) {
            ((EditText)v).setCursorVisible(true);
        } else {
            ((EditText)v).setCursorVisible(false);
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public void updateDisplayedData() {
        String[] searchTerms = searchBox.getText().toString().trim().split("\\s+");
        String[] subqueries = new String[searchTerms.length];
        for(int i=0; i < searchTerms.length; i++) {
            String subquery = String.format(" (%s.%s LIKE '%%%s%%' OR %s.%s LIKE '%%%s%%' OR %s.%s LIKE '%%%s%%') ",
                    DBManager.C.Talk.TABLE_NAME,
                    DBManager.C.Talk.TITLE,
                    searchTerms[i],

                    // OR
                    DBManager.C.Talk.TABLE_NAME,
                    DBManager.C.Talk.DESCRIPTION,
                    searchTerms[i],

                    // OR
                    DBManager.C.Teacher.TABLE_NAME,
                    DBManager.C.Teacher.NAME,
                    searchTerms[i]);

            subqueries[i] = subquery;
        }
        String searchSubquery = TextUtils.join(" AND ", subqueries);

        String starFilterTable = "";
        String starFilterSubquery = "";
        if(starFilterOn) {
            starFilterTable = String.format(" , %s ", DBManager.C.TalkStars.TABLE_NAME);
            starFilterSubquery = String.format(" AND %s.%s=%s.%s ",
                    DBManager.C.Talk.TABLE_NAME,
                    DBManager.C.Talk.ID,
                    DBManager.C.TalkStars.TABLE_NAME,
                    DBManager.C.TalkStars.TALK_ID
            );
        }

        final String query = String.format(
                "SELECT %s.%s, %s.%s, %s.%s, %s.%s " +
                        "FROM %s, %s %s " +
                        "WHERE %s.%s=%s.%s " +
                        "AND %s %s " +
                        "ORDER BY %s.%s DESC",
                // SELECT
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.ID,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.NAME,

                // FROM
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Teacher.TABLE_NAME,
                starFilterTable,

                // WHERE
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,

                // AND
                // Search filter sub-query
                searchSubquery,

                // Star filter sub-query
                starFilterSubquery,

                // ORDER BY
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.UPDATE_DATE
        );

        Cursor cursor = dbManager.getReadableDatabase().rawQuery(query, null);
        talkListCursorAdapter.changeCursor(cursor);

    }

}
