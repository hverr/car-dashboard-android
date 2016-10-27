package com.deliquus.cardashboard;

import android.app.*;
import android.content.res.*;
import android.os.*;
import android.support.v4.view.*;
import android.support.v4.widget.*;
import android.support.v7.app.*;
import android.view.*;
import android.widget.*;

public class MainActivity extends AppCompatActivity {
    static public final String TAG = "MainActivity";

    static private final String[] TILES = new String[] {
            "Dashboard",
            "Settings"
    };
    private DrawerLayout drawerLayout;
    private ListView drawerListView;

    private ActionBarDrawerToggle actionBarDrawerToggle;

    private CharSequence drawerTitle;
    private CharSequence fragmentTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawerListView = (ListView)findViewById(R.id.drawer_list_view);

        drawerTitle = fragmentTitle = getTitle();

        // Initialize the drawer list view
        drawerListView.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, TILES));
        drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectItem(i);
            }
        });

        // Initialize the drawer layout
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // Initialize the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

        actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                getSupportActionBar().setTitle(fragmentTitle);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(drawerTitle);
                supportInvalidateOptionsMenu();
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        // Restore
        if(savedInstanceState == null) {
            selectItem(0);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        fragmentTitle = title;
        super.setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void selectItem(int index) {
        android.util.Log.d(TAG, "selectItem(" + index + ")");
        if(index == 0) {
            loadDashboard();
        } else {
            loadSettings();
        }
    }

    private void loadDashboard() {
        DashboardFragment fragment = new DashboardFragment();
        FragmentManager manager = getFragmentManager();
        manager.beginTransaction().replace(R.id.content_layout, fragment).commit();
        setTitle("Dashboard");
    }

    private void loadSettings() {
        android.util.Log.d(TAG, "loadSettings()");
    }
}
