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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        TextView.OnEditorActionListener,
        AdapterView.OnItemClickListener,
        View.OnFocusChangeListener {

    public final static String TALK_DETAIL_EXTRA = "org.dharmaseed.androidapp.TALK_DETAIL";

    NavigationView navigationView;
    ListView listView;
    int savedListPosition;
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

    static final int DETAIL_MODE_NONE = 0, DETAIL_MODE_TEACHER = 1, DETAIL_MODE_CENTER = 2;
    int detailMode;
    long detailId;

    TalkFetcherTask talkFetcherTask;
    TeacherFetcherTask teacherFetcherTask;
    CenterFetcherTask centerFetcherTask;

    private static final String LOG_TAG = "NavigationActivity";

    private boolean downloadedOnly;

    private TalkRepository talkRepository;
    private TeacherRepository teacherRepository;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("SearchClusterVisible", searchCluster.getVisibility() == View.VISIBLE);
        outState.putString("ExtraSearchTerms", extraSearchTerms);
        outState.putBoolean("HeaderVisible", header.getVisibility() == View.VISIBLE);
        outState.putBoolean("StarFilterOn", starFilterOn);
        outState.putInt("ViewMode", viewMode);
        outState.putInt("DetailMode", detailMode);
        outState.putLong("DetailId", detailId);

        // Save list position in the object as well to handle the case when the activity is
        // only being paused, not destroyed. This happens, for example, when navigating to
        // the play talk activity
        savedListPosition = listView.getFirstVisiblePosition();
        outState.putInt("ListViewPosition", savedListPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        starFilterOn = savedInstanceState.getBoolean("StarFilterOn");
        searchCluster.setVisibility(savedInstanceState.getBoolean("SearchClusterVisible") ? View.VISIBLE : View.GONE);
        extraSearchTerms = savedInstanceState.getString("ExtraSearchTerms");
        header.setVisibility(savedInstanceState.getBoolean("HeaderVisible") ? View.VISIBLE : View.GONE);
        setViewMode(savedInstanceState.getInt("ViewMode"));
        setDetailMode(savedInstanceState.getInt("DetailMode"), savedInstanceState.getLong("DetailId"));
        savedListPosition = savedInstanceState.getInt("ListViewPosition");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get the latest data from dharmaseed.org if necessary
        if(editionOutOfDate()) {
            fetchNewDataFromServer();
        } else {
            Log.i("onResume", "Don't need to fetch new data from server");
        }
        updateDisplayedData();

        // Restore list position
        listView.setSelectionFromTop(savedListPosition, 0);
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
        downloadedOnly = false;
        setViewMode(VIEW_MODE_TALKS);
        setDetailMode(DETAIL_MODE_NONE);
        extraSearchTerms = "";
        talkRepository = new TalkRepository(dbManager);
        teacherRepository = new TeacherRepository(dbManager);

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

    }

    boolean editionOutOfDate() {
        String query = String.format("SELECT %s FROM %s WHERE %s=\"%s\"",
                DBManager.C.Edition.EDITION,
                DBManager.C.Edition.TABLE_NAME,
                DBManager.C.Edition.TABLE,
                DBManager.C.Edition.LAST_SYNC);
        Cursor cursor = dbManager.getReadableDatabase().rawQuery(query, null);
        boolean outOfDate = false;
        if(cursor.moveToFirst()) {
            long lastSync = cursor.getLong(cursor.getColumnIndexOrThrow(DBManager.C.Edition.EDITION));
            Date nowDate = new Date();
            long now = nowDate.getTime();
            if(now - lastSync > 1000*60*60*12) { // Update every 12 hours
                outOfDate = true;
            }
        }
        cursor.close();
        return outOfDate;
    }

    void setViewMode(int viewMode) {
        setViewMode(viewMode, true);
    }

    void setViewMode(int viewMode, boolean setMenuCheck) {
        this.viewMode = viewMode;
        header.setVisibility(View.GONE);
        extraSearchTerms = "";
        switch(viewMode) {

            case VIEW_MODE_TALKS:
                getSupportActionBar().setTitle("Talks");
                cursorAdapter = new TalkCursorAdapter(dbManager, this, R.layout.main_list_view_item, null);
                if(setMenuCheck) navigationView.getMenu().findItem(R.id.nav_talks).setChecked(true);
                break;

            case VIEW_MODE_TEACHERS:
                getSupportActionBar().setTitle("Teachers");
                cursorAdapter = new TeacherCursorAdapter(dbManager, this, R.layout.main_list_view_item, null);
                if(setMenuCheck) navigationView.getMenu().findItem(R.id.nav_teachers).setChecked(true);
                break;

            case VIEW_MODE_CENTERS:
                getSupportActionBar().setTitle("Centers");
                cursorAdapter = new CenterCursorAdapter(dbManager, this, R.layout.main_list_view_item, null);
                if(setMenuCheck) navigationView.getMenu().findItem(R.id.nav_centers).setChecked(true);
                break;
        }
        listView.setAdapter(cursorAdapter);
    }

    void setDetailMode(int detailMode) {
        setDetailMode(detailMode, 0);
    }
    void setDetailMode(int detailMode, long id) {
        this.detailMode = detailMode;
        this.detailId = id;

        if(detailMode == DETAIL_MODE_NONE) {
            header.setVisibility(View.GONE);
        } else {

            setViewMode(VIEW_MODE_TALKS, false);
            header.setVisibility(View.VISIBLE);

            // Clear search and star filters
            starFilterOn = false;
            setStarButton();
            clearSearch(searchCluster);

            String query="", header, detail;
            String headerIdx="", detailIdx="";
            Cursor cursor;

            switch (detailMode) {

                case DETAIL_MODE_TEACHER:
                    displayTalksByTeacher((int) id);
                    return;

                case DETAIL_MODE_CENTER:
                    getSupportActionBar().setTitle("Center Detail");
                    query = String.format("SELECT * FROM %s WHERE %s=%s",
                            DBManager.C.Center.TABLE_NAME,
                            DBManager.C.Center.ID, id);
                    headerIdx = DBManager.C.Center.NAME;
                    detailIdx = DBManager.C.Center.DESCRIPTION;
                    break;

            }

            cursor = dbManager.getReadableDatabase().rawQuery(query, null);
            if (cursor.moveToFirst()) {
                header = cursor.getString(cursor.getColumnIndexOrThrow(headerIdx));
                detail = cursor.getString(cursor.getColumnIndexOrThrow(detailIdx));
                headerPrimary.setText(header);
                headerDescription.setText(detail);
                extraSearchTerms = header;
            }
            cursor.close();
            updateDisplayedData();

        }
    }

    public void displayTalksByTeacher(int id)
    {
        getSupportActionBar().setTitle("Teacher Detail");
        Cursor cursor = teacherRepository.getTeacherById((int)id);
        Teacher teacher = Teacher.create(cursor);
        headerPrimary.setText(teacher.getName());
        headerDescription.setText(teacher.getBio());

        cursor = talkRepository.getTalksByTeacher(id, starFilterOn, downloadedOnly);
        if (cursor != null)
        {
            cursorAdapter.changeCursor(cursor);
        }
        else
        {
            showToast("There was a problem fetching talks by " + teacher.getName() + ".");
            updateDisplayedTeachers();
        }
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
        else if (detailMode == DETAIL_MODE_TEACHER) {
            setViewMode(VIEW_MODE_TEACHERS);
            updateDisplayedData();
        }
        else if (detailMode == DETAIL_MODE_CENTER) {
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
                setDetailMode(DETAIL_MODE_TEACHER, id);
                break;

            case VIEW_MODE_CENTERS:
                setDetailMode(DETAIL_MODE_CENTER, id);
                break;
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
        if(menu != null) {
            MenuItem starButton = menu.findItem(R.id.action_toggle_starred);
            starButton.setIcon(ContextCompat.getDrawable(this, icon));
        }
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
//        else if (id == R.id.nav_retreats) {
//            Intent intent = new Intent(this, RetreatSearchActivity.class);
//            this.startActivity(intent);
//        }
        setDetailMode(DETAIL_MODE_NONE);

        updateDisplayedData();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Called when the "Downloaded only" switch in the nav drawer is pressed
     * @param view
     */
    public void downloadOnlySwitchClicked(View view) {
        Switch downloadSwitch = (Switch) view;
        if (downloadSwitch.isChecked()) {
            downloadedOnly = true;
        } else {
            downloadedOnly = false;
        }
        if (viewMode == VIEW_MODE_TALKS)
            updateDisplayedTalks();
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

        String starFilterTable = "";
        String starFilterSubquery = "";
        if(starFilterOn) {
            starFilterTable = String.format(" , %s ", DBManager.C.TeacherStars.TABLE_NAME);
            starFilterSubquery = String.format(" AND %s.%s=%s.%s ",
                    DBManager.C.Teacher.TABLE_NAME,
                    DBManager.C.Teacher.ID,
                    DBManager.C.TeacherStars.TABLE_NAME,
                    DBManager.C.TeacherStars.ID
            );
        }


        final String query = String.format(
                "SELECT %s.%s, %s.%s " +
                        "FROM %s %s " +
                        "WHERE %s %s " +
                        "AND %s='true' " +
                        "ORDER BY %s DESC, %s ASC",
                // SELECT
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.NAME,

                // FROM
                DBManager.C.Teacher.TABLE_NAME,
                starFilterTable,

                // WHERE
                searchSubquery,
                starFilterSubquery,

                // AND
                DBManager.C.Teacher.PUBLIC,

                // ORDER BY
                DBManager.C.Teacher.MONASTIC,
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

        String starFilterTable = "";
        String starFilterSubquery = "";
        if(starFilterOn) {
            starFilterTable = String.format(" , %s ", DBManager.C.CenterStars.TABLE_NAME);
            starFilterSubquery = String.format(" AND %s.%s=%s.%s ",
                    DBManager.C.Center.TABLE_NAME,
                    DBManager.C.Center.ID,
                    DBManager.C.CenterStars.TABLE_NAME,
                    DBManager.C.CenterStars.ID
            );
        }

        final String query = String.format(
                "SELECT %s.%s, %s.%s " +
                        "FROM %s %s " +
                        "WHERE %s %s " +
                        "ORDER BY %s ASC",
                // SELECT
                DBManager.C.Center.TABLE_NAME,
                DBManager.C.Center.ID,
                DBManager.C.Center.TABLE_NAME,
                DBManager.C.Center.NAME,

                // FROM
                DBManager.C.Center.TABLE_NAME,
                starFilterTable,

                // WHERE
                searchSubquery,
                starFilterSubquery,

                // ORDER BY
                DBManager.C.Center.NAME
        );

        Cursor cursor = dbManager.getReadableDatabase().rawQuery(query, null);
        cursorAdapter.changeCursor(cursor);

    }

    /**
     * Updates the view with talks by search term, and whether talks are downloaded/starred
     */
    void updateDisplayedTalks()
    {
        ArrayList<String> searchTerms = null;
        if (searchBox.getText().length() > 0)
        {
            searchTerms = new ArrayList<>(Arrays.asList(searchBox.getText().toString().trim().split("\\s+")));
            if(!extraSearchTerms.equals("")) searchTerms.add(extraSearchTerms);
        }
        Cursor cursor = talkRepository.getTalkAdapterData(searchTerms, starFilterOn, downloadedOnly);
        if (cursor != null)
        {
            cursorAdapter.changeCursor(cursor);
        }
        else
        {
            showToast("There was a problem with the query");
        }
    }

    /**
     * Shows a short toast with text=message
     * @param message
     */
    public void showToast(String message)
    {
        Toast.makeText(
                getApplicationContext(),
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

}
