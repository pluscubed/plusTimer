package com.pluscubed.plustimer.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pluscubed.plustimer.R;

/**
 * Base Activity with the Navigation Drawer
 */
public abstract class BaseActivity extends Activity {
    protected static final int NAVDRAWER_ITEM_CURRENT_SESSION = 0;
    protected static final int NAVDRAWER_ITEM_HISTORY = 1;
    protected static final int NAVDRAWER_ITEM_INVALID = -1;
    private static final String PREF_WELCOME_DONE = "welcome_done";
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;
    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    private static final int MAIN_CONTENT_FADEIN_DURATION = 200;

    private static int[] sAbItemIds;
    private static String[] sSectionTitles;
    private static CharSequence sDrawerTitle;
    private static String[] sSectionAbTitles;
    private boolean mUserLearnedDrawer;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private ActionBarDrawerToggle mDrawerToggle;
    private Handler mHandler;
    private boolean mFadeIn = true;

    public static boolean isWelcomeDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_WELCOME_DONE, false);
    }

    public static void markWelcomeDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_WELCOME_DONE, true).apply();
    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses
     * of BaseActivity override this to indicate what nav drawer item corresponds to them
     * Return NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    /**
     * Returns the Action Bar item ids that corresponds to this Activity. Subclasses
     * of BaseActivity override this to indicate what items should be hidden when
     * the nav dawer is open.
     */
    protected int[] getNavDrawerHideActionBarItemIds() {
        return new int[0];
    }

    protected ActionBarWrappedDrawerToggle getWrappedDrawerToggle() {
        return new ActionBarWrappedDrawerToggle();
    }

    /**
     * Sets up the navigation drawer as appropriate.
     */
    private void setupNavDrawer() {
        // What nav drawer item should be selected?
        int selfItem = getSelfNavDrawerItem();


        mDrawerListView = (ListView) findViewById(R.id.activity_base_drawer_listview);
        sSectionTitles = getResources().getStringArray(R.array.drawer_array);
        sDrawerTitle = getResources().getString(R.string.app_name);
        sSectionAbTitles = getResources().getStringArray(R.array.drawer_actionbar_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.activity_base_drawerlayout);


        if (mDrawerLayout == null) {
            return;
        }
        if (selfItem == NAVDRAWER_ITEM_INVALID) {
            // do not show a nav drawer
            View navDrawer = mDrawerLayout.findViewById(R.id.activity_base_drawer_listview);
            if (navDrawer != null) {
                ((ViewGroup) navDrawer.getParent()).removeView(navDrawer);
            }
            mDrawerLayout = null;
            return;
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            private ActionBarWrappedDrawerToggle mWrapped = getWrappedDrawerToggle();

            /** Called when a drawer has settled in a completely closed state. */
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                mWrapped.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mWrapped.onDrawerOpened(drawerView);
                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    PreferenceManager.getDefaultSharedPreferences(BaseActivity.this).edit().putBoolean(PREF_WELCOME_DONE, true).commit();
                }

                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                mWrapped.onDrawerSlide(drawerView, slideOffset);
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        @SuppressLint("InflateParams")
        View nav_drawer_footer = getLayoutInflater().inflate(R.layout.nav_drawer_footer, null);
        LinearLayout nav_drawer_settings = (LinearLayout) nav_drawer_footer.findViewById(R.id.nav_drawer_footer_settings_linearlayout);
        nav_drawer_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(BaseActivity.this, SettingsActivity.class));
                    }
                }, NAVDRAWER_LAUNCH_DELAY);
                mDrawerLayout.closeDrawer(mDrawerListView);
            }
        });
        mDrawerListView.addFooterView(nav_drawer_footer, null, false);
        mDrawerListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onNavDrawerItemClicked(position);
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle.syncState();

        mDrawerListView.setAdapter(new NavDrawerAdapater());
        setTitle(sSectionAbTitles[getSelfNavDrawerItem()]);

        // When the user runs the app for the first time, we want to land them with the
        // navigation drawer open. But just the first time.
        if (!isWelcomeDone(this)) {
            // first run of the app starts with the nav drawer open
            markWelcomeDone(this);
            mDrawerLayout.openDrawer(Gravity.START);
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupNavDrawer();
        mHandler = new Handler();

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = isNavDrawerOpen();
        if (drawerOpen) {
            for (int i : getNavDrawerHideActionBarItemIds()) {
                MenuItem menuItem = menu.findItem(i);
                if (menuItem != null) {
                    menuItem.setVisible(false);
                }
            }
        }
        if (drawerOpen) {
            getActionBar().setTitle(sDrawerTitle);
        } else {
            getActionBar().setTitle(sSectionAbTitles[getSelfNavDrawerItem()]);
        }
        return super.onPrepareOptionsMenu(menu);
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
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    private void onNavDrawerItemClicked(final int position) {
        if (position == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if (position != getSelfNavDrawerItem()) {
            goToNavDrawerItem(position);
            return;
        }

        // launch the target Activity after a short delay, to allow the close animation to play
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                goToNavDrawerItem(position);
            }
        }, NAVDRAWER_LAUNCH_DELAY);

        // fade out the main content
        View mainContent = findViewById(R.id.activity_base_content_framelayout);
        if (mainContent != null) {
            mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
        }

        mDrawerListView.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(Gravity.START);
    }

    protected void setFadeIn(boolean fadeIn) {
        mFadeIn = fadeIn;
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);


        View mainContent = findViewById(R.id.activity_base_content_framelayout);
        if (mFadeIn && mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        }
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START);
    }

    private void goToNavDrawerItem(int position) {
        Intent i;
        switch (position) {
            case NAVDRAWER_ITEM_CURRENT_SESSION:
                i = new Intent(this, CurrentSessionActivity.class);
                break;
            case NAVDRAWER_ITEM_HISTORY:
            default:
                Toast.makeText(getApplicationContext(), "Work in Progress", Toast.LENGTH_SHORT).show();
                return;
        }
        startActivity(i);
        finish();
    }

    public class ActionBarWrappedDrawerToggle {
        public void onDrawerClosed(View view) {
        }

        public void onDrawerOpened(View drawerView) {
        }

        public void onDrawerSlide(View drawerView, float slideOffset) {
        }
    }

    public class NavDrawerAdapater extends ArrayAdapter<String> {
        public NavDrawerAdapater() {
            super(BaseActivity.this, 0, sSectionTitles);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_drawer, parent, false);
            }

            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(getItem(position));
            if (position == getSelfNavDrawerItem()) {
                text.setTypeface(null, Typeface.BOLD);
            } else {
                text.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            }
            return convertView;
        }
    }
}
