package org.dharmaseed.androidapp;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

public class NavigationActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        TextView.OnEditorActionListener,
        View.OnFocusChangeListener {

    public final static String TALK_DETAIL_EXTRA = "org.dharmaseed.androidapp.TALK_DETAIL";

    ListView talkListView;
    EditText searchBox;
    DBManager dbManager;
    TalkListViewAdapter talkListCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Talks");

        dbManager = new DBManager(this);

        searchBox = (EditText)findViewById(R.id.nav_search_text);
        searchBox.setVisibility(View.GONE);
        searchBox.setOnEditorActionListener(this);
        searchBox.setOnFocusChangeListener(this);

        talkListView = (ListView) findViewById(R.id.talksListView);
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
                getApplicationContext(),
                R.layout.talk_list_view_item,
                null
        );
        talkListView.setAdapter(talkListCursorAdapter);

        // Fetch new data from the server
        new TeacherFetcherTask(dbManager, this, getApplicationContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new CenterFetcherTask(dbManager, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new TalkFetcherTask(dbManager, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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

        Log.i("nav", "selected " + id);



        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.i("nav", "Settings!");
            return true;
        } else if (id == R.id.action_search) {
            Log.i("nav", "Search!");
            EditText searchBox = (EditText)findViewById(R.id.nav_search_text);
            if(searchBox.getVisibility() == View.GONE) {
                searchBox.setVisibility(View.VISIBLE);
            } else {
                searchBox.setVisibility(View.GONE);
                searchBox.setText("");
                updateDisplayedData();
            }
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

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        updateDisplayedData();

        return true;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (! hasFocus) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    public void updateDisplayedData() {
        String searchTerms = searchBox.getText().toString().trim();

        String query = String.format(
                "SELECT %s.%s, %s.%s, %s.%s, %s.%s FROM %s, %s " +
                        "WHERE %s.%s=%s.%s AND " +
                        "(%s.%s LIKE '%%%s%%' OR %s.%s LIKE '%%%s%%' OR %s.%s LIKE '%%%s%%') " +
                        "ORDER BY %s.%s DESC LIMIT 200",
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.ID,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TITLE,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.NAME,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TEACHER_ID,
                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.ID,

                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.TITLE,
                searchTerms,

                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.DESCRIPTION,
                searchTerms,

                DBManager.C.Teacher.TABLE_NAME,
                DBManager.C.Teacher.NAME,
                searchTerms,

                DBManager.C.Talk.TABLE_NAME,
                DBManager.C.Talk.UPDATE_DATE
        );
        Cursor cursor = dbManager.getReadableDatabase().rawQuery(query, null);
        talkListCursorAdapter.changeCursor(cursor);

    }
}
