package com.deliquus.cardashboard;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.os.*;
import android.preference.*;
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
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;

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

        // Load preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setKeepScreenOn(prefs.getBoolean(PreferenceKeys.SCREEN_ON_PREF, true));
        setRPiAddress(prefs.getString(PreferenceKeys.RPI_ADDRESS_PREF, null));
        preferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String s) {
                if(s.equals(PreferenceKeys.SCREEN_ON_PREF)) {
                    setKeepScreenOn(prefs.getBoolean(PreferenceKeys.SCREEN_ON_PREF, true));
                } else {
                    setRPiAddress(prefs.getString(PreferenceKeys.RPI_ADDRESS_PREF, null));
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener);

        // Restore
        if(savedInstanceState == null) {
            selectItem(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
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
            drawerLayout.closeDrawers();
        } else {
            loadSettings();
            drawerLayout.closeDrawers();
        }
    }

    private void loadDashboard() {
        DashboardFragment fragment = new DashboardFragment();
        FragmentManager manager = getFragmentManager();
        manager.beginTransaction().replace(R.id.content_layout, fragment).commit();
        setTitle("Dashboard");
    }

    private void loadSettings() {
        SettingsFragment fragment = new SettingsFragment();
        FragmentManager manager = getFragmentManager();
        manager.beginTransaction().replace(R.id.content_layout, fragment).commit();
        setTitle("Settings");
    }

    private void setKeepScreenOn(boolean flag) {
        android.util.Log.d(TAG, "setKeepScreenOn(" + flag + ")");
        if(flag) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void setRPiAddress(String address) {
        android.util.Log.d(TAG, "setRPiAddress(" + address + ")");
    }
}
