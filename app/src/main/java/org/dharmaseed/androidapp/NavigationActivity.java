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
import android.support.v4.widget.CursorAdapter;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        TextView.OnEditorActionListener,
        AdapterView.OnItemClickListener,
        View.OnFocusChangeListener {

    public final static String TALK_DETAIL_EXTRA = "org.dharmaseed.androidapp.TALK_DETAIL";

    NavigationView navigationView;
    ListView listView;
    EditText searchBox;
    String extraSearchTerms;
    LinearLayout searchCluster, header;
    TextView headerPrimary, headerDescription;
    boolean starFilterOn;
    Menu menu;
    DBManager dbManager;
    CursorAdapter cursorAdapter;
    SwipeRefreshLayout refreshLayout;

    static final int VIEW_MODE_TALKS = 0, VIEW_MODE_TEACHERS = 1, VIEW_MODE_CENTERS = 2;
    int viewMode;

    TalkFetcherTask talkFetcherTask;
    TeacherFetcherTask teacherFetcherTask;
    CenterFetcherTask centerFetcherTask;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("SearchClusterVisible", searchCluster.getVisibility() == View.VISIBLE);
        outState.putString("ExtraSearchTerms", extraSearchTerms);
        outState.putBoolean("HeaderVisible", header.getVisibility() == View.VISIBLE);
        outState.putBoolean("StarFilterOn", starFilterOn);
        outState.putInt("ViewMode", viewMode);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        starFilterOn = savedInstanceState.getBoolean("StarFilterOn");
        searchCluster.setVisibility(savedInstanceState.getBoolean("SearchClusterVisible") ? View.VISIBLE : View.GONE);
        extraSearchTerms = savedInstanceState.getString("ExtraSearchTerms");
        header.setVisibility(savedInstanceState.getBoolean("HeaderVisible") ? View.VISIBLE : View.GONE);
        setViewMode(savedInstanceState.getInt("ViewMode"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbManager = new DBManager(this);

        // Configure search box
        searchBox = (EditText)findViewById(R.id.nav_search_text);
        searchBox.setOnEditorActionListener(this);
        searchBox.setOnFocusChangeListener(this);
        searchCluster = (LinearLayout)findViewById(R.id.nav_search_cluster);

        // Configure header
        header = (LinearLayout)findViewById(R.id.nav_sub_header);
        headerPrimary = (TextView)findViewById(R.id.nav_sub_header_primary);
        headerDescription = (TextView)findViewById(R.id.nav_sub_header_description);

        // Configure navigation view
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Configure list view
        listView = (ListView) findViewById(R.id.talks_list_view);
        listView.setOnItemClickListener(this);

        // Initialize UI state
        starFilterOn = false;
        searchCluster.setVisibility(View.GONE);
        header.setVisibility(View.GONE);
        setViewMode(VIEW_MODE_TALKS);
        extraSearchTerms = "";

        // Set swipe refresh listener
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.talks_list_view_swipe_refresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i("refresh", "onrefresh");
                fetchNewDataFromServer();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("navigationActivity", "Received update broadcast");
                updateDisplayedData();
            }
        }, new IntentFilter("updateDisplayedData"));

        // Get the latest data from dharmaseed.org
        fetchNewDataFromServer();
        updateDisplayedData();

    }

    void setViewMode(int viewMode) {
        this.viewMode = viewMode;
        header.setVisibility(View.GONE);
        extraSearchTerms = "";
        switch(viewMode) {

            case VIEW_MODE_TALKS:
                getSupportActionBar().setTitle("Talks");
                cursorAdapter = new TalkCursorAdapter(this, R.layout.main_list_view_item, null);
                navigationView.getMenu().findItem(R.id.nav_talks).setChecked(true);
                break;

            case VIEW_MODE_TEACHERS:
                getSupportActionBar().setTitle("Teachers");
                cursorAdapter = new TeacherCursorAdapter(this, R.layout.main_list_view_item, null);
                navigationView.getMenu().findItem(R.id.nav_teachers).setChecked(true);
                break;

            case VIEW_MODE_CENTERS:
                getSupportActionBar().setTitle("Centers");
                cursorAdapter = new CenterCursorAdapter(this, R.layout.main_list_view_item, null);
                navigationView.getMenu().findItem(R.id.nav_centers).setChecked(true);
                break;

        }
        listView.setAdapter(cursorAdapter);
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
        }
        else if (getSupportActionBar().getTitle().equals("Teacher Detail")) {
            setViewMode(VIEW_MODE_TEACHERS);
            updateDisplayedData();
        }
        else if (getSupportActionBar().getTitle().equals("Center Detail")) {
            setViewMode(VIEW_MODE_CENTERS);
            updateDisplayedData();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        this.menu = menu;
        setStarButton();
        return true;
    }

    public void clearSearch(View v) {
        searchCluster.setVisibility(View.GONE);
        searchBox.setText("");
        updateDisplayedData();
        resetListToTop();
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
                if (searchCluster.getVisibility() == View.GONE) {
                    searchCluster.setVisibility(View.VISIBLE);
                    searchBox.requestFocus();
                    searchBox.setCursorVisible(true);
                    InputMethodManager keyboard = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.showSoftInput(searchBox, 0);
                } else {
                    clearSearch(searchCluster);
                }
                return true;

            case R.id.action_toggle_starred:
                starFilterOn = ! starFilterOn;
                setStarButton();
                updateDisplayedData();
                resetListToTop();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Called when an item in the main list view is clicked
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("onItemClick", "selected " + position + ", " + id);
        Context ctx = parent.getContext();

        switch(viewMode) {
            case VIEW_MODE_TALKS:
                Intent intent = new Intent(ctx, PlayTalkActivity.class);
                intent.putExtra(TALK_DETAIL_EXTRA, id);
                ctx.startActivity(intent);
                break;

            case VIEW_MODE_TEACHERS:
                setViewMode(VIEW_MODE_TALKS);
                getSupportActionBar().setTitle("Teacher Detail");
                header.setVisibility(View.VISIBLE);

                String query = String.format("SELECT * FROM %s WHERE %s=%s",
                        DBManager.C.Teacher.TABLE_NAME,
                        DBManager.C.Teacher.ID, id);
                Cursor cursor = dbManager.getReadableDatabase().rawQuery(query, null);
                if(cursor.moveToFirst()) {
                    String teacherName = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.NAME));
                    String teacherBio = cursor.getString(cursor.getColumnIndexOrThrow(DBManager.C.Teacher.BIO));
                    headerPrimary.setText(teacherName);
                    headerDescription.setText(teacherBio);
                    extraSearchTerms = teacherName;
                }
                cursor.close();
                updateDisplayedData();
                break;  // TODO

            case VIEW_MODE_CENTERS:
                setViewMode(VIEW_MODE_TALKS);
                getSupportActionBar().setTitle("Center Detail");
                header.setVisibility(View.VISIBLE);
                break;  // TODO
        }
    }

    // Reset the list to show the first item. Still not entirely sure why it's necessary to do it
    // this way, but see https://groups.google.com/forum/#!topic/android-developers/EnyldBQDUwE
    // and http://stackoverflow.com/questions/1446373/android-listview-setselection-does-not-seem-to-work
    private void resetListToTop() {
        listView.clearFocus();
        listView.post(new Runnable() {
            @Override
            public void run() {
                listView.setSelection(0);
            }
        });
    }

    public void setStarButton() {
        int icon;
        if(starFilterOn) {
            icon  = getResources().getIdentifier("btn_star_big_on", "drawable", "android");
        } else {
            icon = getResources().getIdentifier("btn_star_big_off", "drawable", "android");
        }
        MenuItem starButton = menu.findItem(R.id.action_toggle_starred);
        starButton.setIcon(ContextCompat.getDrawable(this, icon));
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_talks) {
            setViewMode(VIEW_MODE_TALKS);
        } else if (id == R.id.nav_teachers) {
            setViewMode(VIEW_MODE_TEACHERS);
        } else if (id == R.id.nav_centers) {
            setViewMode(VIEW_MODE_CENTERS);
        }

        updateDisplayedData();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void headingDetailCollapseExpandButtonClicked(View view) {
        ScrollView scrollView = (ScrollView) findViewById(R.id.nav_sub_header_description_scroll);
        ImageButton button = (ImageButton) findViewById(R.id.heading_detail_collapse_expand_button);
        if(scrollView.getVisibility() == View.VISIBLE) {
            scrollView.setVisibility(View.GONE);
            button.setImageDrawable(ContextCompat.getDrawable(this,
                    getResources().getIdentifier("arrow_down_float", "drawable", "android")));
        } else {
            scrollView.setVisibility(View.VISIBLE);
            button.setImageDrawable(ContextCompat.getDrawable(this,
                    getResources().getIdentifier("arrow_up_float", "drawable", "android")));
        }
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

        // Scroll to the top of the list
        resetListToTop();

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
        switch (viewMode) {
            case VIEW_MODE_TALKS:
                updateDisplayedTalks();
                break;
            case VIEW_MODE_TEACHERS:
                updateDisplayedTeachers();
                break;
            case VIEW_MODE_CENTERS:
                updateDisplayedCenters();
                break;
        }
    }

    void updateDisplayedTeachers() {
        // TODO: refactor common logic in updateDisplayedXXX methods
        String[] searchTerms = searchBox.getText().toString().trim().split("\\s+");
        String[] subqueries = new String[searchTerms.length];
        for(int i=0; i < searchTerms.length; i++) {
            String subquery = String.format(" (%s LIKE '%%%s%%') ",
                    DBManager.C.Teacher.NAME,
                    searchTerms[i]);

            subqueries[i] = subquery;
        }
        String searchSubquery = TextUtils.join(" AND ", subqueries);

        // TODO: add star filter
        /*
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
        */

        final String query = String.format(
                "SELECT %s, %s " +
                        "FROM %s " +
                        "WHERE %s " +
                        "ORDER BY %s ASC",
                // SELECT
                DBManager.C.Teacher.ID,
                DBManager.C.Teacher.NAME,

                // FROM
                DBManager.C.Teacher.TABLE_NAME,

                // WHERE
                searchSubquery,

                // ORDER BY
                DBManager.C.Teacher.NAME
        );

        Cursor cursor = dbManager.getReadableDatabase().rawQuery(query, null);
        cursorAdapter.changeCursor(cursor);

    }

    void updateDisplayedCenters() {
        String[] searchTerms = searchBox.getText().toString().trim().split("\\s+");
        String[] subqueries = new String[searchTerms.length];
        for(int i=0; i < searchTerms.length; i++) {
            String subquery = String.format(" (%s LIKE '%%%s%%') ",
                    DBManager.C.Center.NAME,
                    searchTerms[i]);

            subqueries[i] = subquery;
        }
        String searchSubquery = TextUtils.join(" AND ", subqueries);

        // TODO: add star filter
        /*
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
        */

        final String query = String.format(
                "SELECT %s, %s " +
                        "FROM %s " +
                        "WHERE %s " +
                        "ORDER BY %s ASC",
                // SELECT
                DBManager.C.Center.ID,
                DBManager.C.Center.NAME,

                // FROM
                DBManager.C.Center.TABLE_NAME,

                // WHERE
                searchSubquery,

                // ORDER BY
                DBManager.C.Center.NAME
        );

        Cursor cursor = dbManager.getReadableDatabase().rawQuery(query, null);
        cursorAdapter.changeCursor(cursor);

    }


    void updateDisplayedTalks() {
        ArrayList<String> searchTerms = new ArrayList<>(Arrays.asList(searchBox.getText().toString().trim().split("\\s+")));
        if(!extraSearchTerms.equals("")) searchTerms.add(extraSearchTerms);
        String[] subqueries = new String[searchTerms.size()];
        for(int i=0; i < searchTerms.size(); i++) {
            String subquery = String.format(" (%s.%s LIKE '%%%s%%' OR %s.%s LIKE '%%%s%%' OR %s.%s LIKE '%%%s%%') ",
                    DBManager.C.Talk.TABLE_NAME,
                    DBManager.C.Talk.TITLE,
                    searchTerms.get(i),

                    // OR
                    DBManager.C.Talk.TABLE_NAME,
                    DBManager.C.Talk.DESCRIPTION,
                    searchTerms.get(i),

                    // OR
                    DBManager.C.Teacher.TABLE_NAME,
                    DBManager.C.Teacher.NAME,
                    searchTerms.get(i));

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
        cursorAdapter.changeCursor(cursor);

    }

}
