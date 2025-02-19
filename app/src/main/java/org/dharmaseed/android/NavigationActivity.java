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

package org.dharmaseed.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.util.Log;
import android.view.View;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.AbsListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        TextView.OnEditorActionListener,
        AdapterView.OnItemClickListener,
        View.OnFocusChangeListener {

    public final static String TALK_DETAIL_EXTRA = "org.dharmaseed.android.TALK_DETAIL";

    NavigationView navigationView;
    ListView listView;
    int savedListPosition;
    EditText searchBox;
    String extraSearchTerms;
    LinearLayout searchCluster, header;
    TextView headerPrimary, headerDescription, websiteLink, donationLink;
    Menu menu;
    DBManager dbManager;
    CursorAdapter cursorAdapter;
    SwipeRefreshLayout refreshLayout;
    TextViewFader scrollFader;

    private int viewMode;

    private static final int VIEW_MODE_TALKS          = 0;
    private static final int VIEW_MODE_TEACHERS       = 1;
    private static final int VIEW_MODE_CENTERS        = 2;
    private static final int VIEW_MODE_TEACHER_TALKS  = 3;
    private static final int VIEW_MODE_CENTER_TALKS   = 4;

    private static final int DETAIL_MODE_NONE    = 0;
    private static final int DETAIL_MODE_TEACHER = 1;
    private static final int DETAIL_MODE_CENTER  = 2;

    int detailMode;
    long detailId;

    TalkFetcherTask talkFetcherTask;
    TeacherFetcherTask teacherFetcherTask;
    CenterFetcherTask centerFetcherTask;

    private static final String LOG_TAG = "NavigationActivity";

    boolean starFilterOn;
    private boolean downloadFilterOn, historyFilterOn;

    private TalkRepository talkRepository;
    private TeacherRepository teacherRepository;
    private CenterRepository centerRepository;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("SearchClusterVisible", searchCluster.getVisibility() == View.VISIBLE);
        outState.putString("ExtraSearchTerms", extraSearchTerms);
        outState.putBoolean("HeaderVisible", header.getVisibility() == View.VISIBLE);
        outState.putBoolean("StarFilterOn", starFilterOn);
        outState.putBoolean("DownloadFilterOn", downloadFilterOn);
        outState.putBoolean("HistoryFilterOn", historyFilterOn);
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
        searchCluster.setVisibility(savedInstanceState.getBoolean("SearchClusterVisible") ? View.VISIBLE : View.GONE);
        extraSearchTerms = savedInstanceState.getString("ExtraSearchTerms");
        header.setVisibility(savedInstanceState.getBoolean("HeaderVisible") ? View.VISIBLE : View.GONE);
        setViewMode(savedInstanceState.getInt("ViewMode"));
        setDetailMode(savedInstanceState.getInt("DetailMode"), savedInstanceState.getLong("DetailId"));
        savedListPosition = savedInstanceState.getInt("ListViewPosition");

        starFilterOn = savedInstanceState.getBoolean("StarFilterOn");
        setStarFilterButton();
        downloadFilterOn = savedInstanceState.getBoolean("DownloadFilterOn");
        setDownloadFilterButton();
        historyFilterOn = savedInstanceState.getBoolean("HistoryFilterOn");
        setHistoryFilterButton();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get the latest data from dharmaseed.org if necessary
        if (dbManager.shouldSync()) {
            fetchNewDataFromServer();
        } else {
            Log.i(LOG_TAG, "onResume: don't need to fetch new data from server");
        }
        updateDisplayedData();

        // Restore list position
        listView.setSelectionFromTop(savedListPosition, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Uncomment to get StrictMode warnings in the logs
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            StrictMode.VmPolicy policy = null;
//            policy = new StrictMode.VmPolicy.Builder()
//                    .detectAll()
//                    .penaltyLog()
//                    .build();
//            StrictMode.setVmPolicy(policy);
//        }


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbManager = DBManager.getInstance(this);

        // Configure search box
        searchBox = (EditText)findViewById(R.id.nav_search_text);
        searchBox.setOnEditorActionListener(this);
        searchBox.setOnFocusChangeListener(this);
        searchCluster = (LinearLayout)findViewById(R.id.nav_search_cluster);

        // Configure header
        header = (LinearLayout)findViewById(R.id.nav_sub_header);
        headerPrimary = (TextView)findViewById(R.id.nav_sub_header_primary);
        headerDescription = (TextView)findViewById(R.id.nav_sub_header_description);
        websiteLink = (TextView)findViewById(R.id.nav_links_website);
        donationLink = (TextView)findViewById(R.id.nav_links_donate);
        websiteLink.setMovementMethod(LinkMovementMethod.getInstance());
        donationLink.setMovementMethod(LinkMovementMethod.getInstance());

        // Configure navigation view
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Configure list view and scroll label
        listView = (ListView) findViewById(R.id.talks_list_view);
        listView.setOnItemClickListener(this);
        scrollFader = new TextViewFader(findViewById(R.id.fadeLabel));

        // Initialize UI state
        starFilterOn = false;
        downloadFilterOn = false;
        historyFilterOn = false;
        extraSearchTerms = "";
        talkRepository = new TalkRepository(dbManager);
        teacherRepository = new TeacherRepository(dbManager);
        centerRepository = new CenterRepository(dbManager);
        setIntendedView();

        // Set swipe refresh listener
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.talks_list_view_swipe_refresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(LOG_TAG, "onrefresh");
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
                Log.i(LOG_TAG, "Received update broadcast");
                updateDisplayedData();
            }
        }, new IntentFilter("updateDisplayedData"));

        listView.setOnScrollListener(new ListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                View item = listView.getChildAt(0);
                if (item != null)
                    updateScrollLabel(item);
            }
        });
    }

    private void updateScrollLabel(View item) {
        int scrollId = -1;
        switch(viewMode) {
            case VIEW_MODE_TALKS:
            case VIEW_MODE_TEACHER_TALKS:
            case VIEW_MODE_CENTER_TALKS:
                // use date as a scroll label for lists of talks
                scrollId = R.id.item_view_detail3;
                break;

            case VIEW_MODE_CENTERS:
            case VIEW_MODE_TEACHERS:
                // use the teacher / center name as a scroll label
                scrollId = R.id.item_view_title;
                break;
        }
        if (scrollId > 0) {
            TextView scrollText = item.findViewById(scrollId);
            if (scrollText != null) {
                scrollFader.setText(scrollText.getText());
            }
        }
    }

    protected void setIntendedView() {
        Intent i = getIntent();
        Log.d(LOG_TAG, "Intent with type=" + i.getType() + " action="+i.getAction() + " data="+i.getData());

        int viewMode = VIEW_MODE_TALKS;
        int detailMode = DETAIL_MODE_NONE;
        long detailId = 0;

        Uri intentURI = i.getData();
        if (intentURI != null) {
            java.util.List<String> segments = intentURI.getPathSegments();
            if (segments.size() >= 2 && segments.get(1).matches("\\d+")) {
                if (segments.get(0).equals("teacher")) {
                    viewMode = VIEW_MODE_TEACHERS;
                    detailMode = DETAIL_MODE_TEACHER;
                    detailId = Integer.parseInt(segments.get(1));
                }
            }

            if (detailMode == DETAIL_MODE_NONE)
                Log.d(LOG_TAG, "Don't know what to do with intent URI "+intentURI);
        }

        searchCluster.setVisibility(View.GONE);
        header.setVisibility(View.GONE);
        setViewMode(viewMode);
        setDetailMode(detailMode, detailId);
    }

    void setViewMode(int viewMode) {
        setViewMode(viewMode, true);
    }

    void setViewMode(int viewMode, boolean setMenuCheck) {
        this.viewMode = viewMode;
        header.setVisibility(View.GONE);
        extraSearchTerms = "";
        clearSearch(false);
        scrollFader.reset();

        switch(viewMode) {

            case VIEW_MODE_TALKS:
            case VIEW_MODE_TEACHER_TALKS:
            case VIEW_MODE_CENTER_TALKS:
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

    private void setDetailMode(int detailMode) {
        setDetailMode(detailMode, 0);
    }

    private void setDetailMode(int detailMode, long id)
    {
        this.detailMode = detailMode;
        this.detailId = id;

        if (detailMode == DETAIL_MODE_NONE)
        {
            header.setVisibility(View.GONE);
        }
        else
        {
            // Clear search and filters
            starFilterOn = false;
            setStarFilterButton();
            clearSearch();
            scrollFader.reset();

            switch (detailMode)
            {
                case DETAIL_MODE_TEACHER:
                    setViewMode(VIEW_MODE_TEACHER_TALKS);
                    setTeacherHeader(id);
                    break;

                case DETAIL_MODE_CENTER:
                    setViewMode(VIEW_MODE_CENTER_TALKS);
                    setCenterHeader(id);
                    break;
            }

            header.setVisibility(View.VISIBLE);
            updateDisplayedData();
        }
    }

    private void setTeacherHeader(long id)
    {
        getSupportActionBar().setTitle("Teacher Detail");
        Cursor cursor = teacherRepository.getTeacherById(id);
        Teacher teacher = Teacher.create(cursor);
        cursor.close();

        headerPrimary.setText(teacher.getName());

        if (!teacher.getWebsite().isEmpty()) {
            websiteLink.setText(Html.fromHtml(String.format("<a href=%s>Teacher's website</a>", teacher.getWebsite())));
        } else {
            websiteLink.setText("");
        }
        if (!teacher.getDonationUrl().isEmpty()) {
            donationLink.setText(Html.fromHtml(String.format("<a href=%s>Donate to this teacher</a>", teacher.getDonationUrl())));
        } else {
            donationLink.setText("");
        }
        headerDescription.setText(teacher.getBio());
    }

    public void displayTalksByTeacher(long id)
    {
        Cursor cursor = talkRepository.getTalksByTeacher(
                getSearchTerms(), id,
                starFilterOn, downloadFilterOn, historyFilterOn
        );
        if (cursor != null)
        {
            cursorAdapter.changeCursor(cursor);
        }
        else
        {
            showToast("There was a problem fetching talks by the teacher.");
            updateDisplayedTeachers();
        }
    }

    private void setCenterHeader(long id)
    {
        getSupportActionBar().setTitle("Center Detail");
        Cursor cursor = centerRepository.getCenterById(id);
        Center center = Center.create(cursor);
        cursor.close();

        headerPrimary.setText(center.getName());

        String descriptionHtml = center.getDescription().replace("\n", "\n<p>") + "\n<p>";
        if (!center.getWebsite().isEmpty()) {
            websiteLink.setText(Html.fromHtml(String.format("<a href=%s>Center's website</a>", center.getWebsite())));
        } else {
            websiteLink.setText("");
        }
        donationLink.setText("");
        headerDescription.setText(center.getDescription());
    }

    private void displayTalksByCenter(long id)
    {
        Cursor cursor = talkRepository.getTalksByCenter(
                getSearchTerms(), id,
                starFilterOn, downloadFilterOn, historyFilterOn
        );
        if (cursor != null)
        {
            cursorAdapter.changeCursor(cursor);
        }
        else
        {
            showToast("There was a problem fetching talks by the center.");
            updateDisplayedCenters();
        }
    }

    public void fetchNewDataFromServer() {

        // Fetch new data from the server
        Log.i(LOG_TAG, "fetchNewDataFromServer()");
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
    public void onBackPressed()
    {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
            return;
        }

        switch (viewMode)
        {
            case VIEW_MODE_TALKS:
                // nowhere to go
                break;
            case VIEW_MODE_TEACHERS:
                setViewMode(VIEW_MODE_TALKS);
                break;
            case VIEW_MODE_TEACHER_TALKS:
                setViewMode(VIEW_MODE_TEACHERS);
                break;
            case VIEW_MODE_CENTERS:
                setViewMode(VIEW_MODE_TALKS);
                break;
            case VIEW_MODE_CENTER_TALKS:
                setViewMode(VIEW_MODE_CENTERS);
                break;
            default:
                super.onBackPressed();
                return;
        }

        updateDisplayedData();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        this.menu = menu;
        setStarFilterButton();
        setDownloadFilterButton();
        setHistoryFilterButton();
        return true;
    }

    public void clearSearch(boolean updateData) {
        searchCluster.setVisibility(View.GONE);
        searchBox.setText("");
        if (updateData) {
            updateDisplayedData();
            resetListToTop();
        }
    }

    public void clearSearch(View view) {
        clearSearch();
    }

    public void clearSearch() {
        clearSearch(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {

//            case R.id.action_refresh_server_data:
//                fetchNewDataFromServer();
//                return true;

            case R.id.action_search:
                Log.i(LOG_TAG, "Search!");
                EditText searchBox = (EditText) findViewById(R.id.nav_search_text);
                if (searchCluster.getVisibility() == View.GONE) {
                    searchCluster.setVisibility(View.VISIBLE);
                    searchBox.requestFocus();
                    searchBox.setCursorVisible(true);
                    InputMethodManager keyboard = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.showSoftInput(searchBox, 0);
                } else {
                    clearSearch();
                }
                return true;

            case R.id.action_toggle_starred:
                starFilterOn = ! starFilterOn;
                setStarFilterButton();
                updateDisplayedData();
                resetListToTop();
                return true;

            case R.id.action_toggle_downloaded:
                downloadFilterOn = ! downloadFilterOn;
                setDownloadFilterButton();
                updateDisplayedData();
                resetListToTop();
                return true;

            case R.id.action_toggle_history:
                historyFilterOn = ! historyFilterOn;
                setHistoryFilterButton();
                updateDisplayedData();
                resetListToTop();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Called when an item in the main list view is clicked
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG, "onItemClick: selected " + position + ", " + id);
        Context ctx = parent.getContext();

        switch(viewMode) {
            case VIEW_MODE_TALKS:
            case VIEW_MODE_TEACHER_TALKS:
            case VIEW_MODE_CENTER_TALKS:
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

    public void setStarFilterButton() {
        int icon  = starFilterOn ?
                R.drawable.star_circle_outline_on : R.drawable.star_circle_outline_off;
        if(menu != null) {
            MenuItem starButton = menu.findItem(R.id.action_toggle_starred);
            starButton.setIcon(ContextCompat.getDrawable(this, icon));
        }
    }

    public void setDownloadFilterButton() {
        int icon  = downloadFilterOn ?
                R.drawable.download_circle_outline_on : R.drawable.download_circle_outline_off;
        if(menu != null) {
            MenuItem dlButton = menu.findItem(R.id.action_toggle_downloaded);
            dlButton.setIcon(ContextCompat.getDrawable(this, icon));
        }
    }

    public void setHistoryFilterButton() {
        int icon  = historyFilterOn ?
                R.drawable.history_on : R.drawable.history_off;
        if(menu != null) {
            MenuItem dlButton = menu.findItem(R.id.action_toggle_history);
            dlButton.setIcon(ContextCompat.getDrawable(this, icon));
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
        Log.i(LOG_TAG, "onEditorAction: " + v.getText().toString());

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
        Log.i(LOG_TAG, "focusChange: " + hasFocus);
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
            case VIEW_MODE_TEACHER_TALKS:
                displayTalksByTeacher(detailId);
                break;
            case VIEW_MODE_CENTER_TALKS:
                displayTalksByCenter(detailId);
                break;
        }
    }

    void updateDisplayedTeachers() {
        Cursor cursor = teacherRepository.getTeachers(
                getSearchTerms(),
                starFilterOn, downloadFilterOn, historyFilterOn
        );
        if (cursor != null)
        {
            cursorAdapter.changeCursor(cursor);
        }
        else
        {
            showToast("There was a problem displaying teachers.");
            setViewMode(VIEW_MODE_TALKS);
            updateDisplayedData();
        }
    }

    void updateDisplayedCenters()
    {
        Cursor cursor = centerRepository.getCenters(
                getSearchTerms(),
                starFilterOn, downloadFilterOn, historyFilterOn
        );
        if (cursor != null)
        {
            cursorAdapter.changeCursor(cursor);
        }
        else
        {
            showToast("There was a problem with the query");
            setViewMode(VIEW_MODE_TALKS);
            updateDisplayedData();
        }
    }

    /**
     * Updates the view with talks by search term, and whether talks are downloaded/starred
     */
    private void updateDisplayedTalks()
    {
        Cursor cursor = talkRepository.getTalkAdapterData(
                getSearchTerms(),
                starFilterOn, downloadFilterOn, historyFilterOn
        );
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
     * @return the list of search terms in the search box or null if empty
     */
    private List<String> getSearchTerms()
    {
        ArrayList<String> searchTerms = null;
        if (searchBox.getText().length() > 0)
        {
            searchTerms = new ArrayList<>(Arrays.asList(searchBox.getText().toString().trim().split("\\s+")));
            if (!extraSearchTerms.equals(""))
            {
                searchTerms.add(extraSearchTerms);
            }
        }

        return searchTerms;
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
