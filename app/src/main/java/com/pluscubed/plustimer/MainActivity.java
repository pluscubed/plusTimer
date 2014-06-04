package com.pluscubed.plustimer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Main Activity
 */
public class MainActivity extends ActionBarActivity {
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    private String[] mFragmentTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private CharSequence mCurrentTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private int mCurrentSelectedPosition = 0;

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mCurrentTitle = title;
        getSupportActionBar().setTitle(mCurrentTitle);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerListView);
        if (menu.findItem(R.id.menu_current_s_puzzletype_spinner) != null)
            menu.findItem(R.id.menu_current_s_puzzletype_spinner).setVisible(!drawerOpen);
        if (menu.findItem(R.id.menu_current_s_toggle_scramble_image_action) != null) {
            menu.findItem(R.id.menu_current_s_toggle_scramble_image_action).setVisible(!drawerOpen);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        }

        mFragmentTitles = getResources().getStringArray(R.array.drawer_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.activity_main_drawerlayout);
        mDrawerListView = (ListView) findViewById(R.id.activity_main_drawer_listview);
        mDrawerTitle = getResources().getString(R.string.app_name);

        mDrawerListView.setAdapter(new ArrayAdapter<String>(this,
                R.layout.list_item_drawer, mFragmentTitles));
        mDrawerListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mCurrentTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        selectItem(mCurrentSelectedPosition);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


    void selectItem(int pos) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        String tag = "";
        Class fragmentClass = null;
        switch (pos) {
            case 0:
                tag = "CurrentSFragment";
                fragmentClass = CurrentSFragment.class;
                break;
            default:
                Toast.makeText(getApplicationContext(), "Work in Progress", Toast.LENGTH_SHORT).show();
                return;
        }
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            if (fragmentClass != null)
                try {
                    fragment = (Fragment) fragmentClass.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            fragmentManager.beginTransaction()
                    .replace(R.id.activity_main_content_framelayout, fragment, tag)
                    .commit();
        }
        mCurrentSelectedPosition = pos;
        mDrawerListView.setItemChecked(pos, true);
        setTitle(mFragmentTitles[pos]);
        mDrawerLayout.closeDrawer(mDrawerListView);

    }

    public void lockOrientation(boolean lock) {
        if (lock) {
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int rotation = display.getRotation();
            int tempOrientation = getResources().getConfiguration().orientation;
            int orientation = 0;
            switch (tempOrientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                        orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    else
                        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                        orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    else
                        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
            setRequestedOrientation(orientation);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
}
