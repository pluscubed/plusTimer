package com.pluscubed.plustimer.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.utils.PrefUtils;

/**
 * Base Activity with the Navigation Drawer
 */
public abstract class DrawerActivity extends ThemableActivity {

    protected static final int NAVDRAWER_ITEM_CURRENT_SESSION = 0;
    protected static final int NAVDRAWER_ITEM_HISTORY = 1;
    protected static final int NAVDRAWER_ITEM_SETTINGS = -3;
    protected static final int NAVDRAWER_ITEM_HELP = -4;
    protected static final int NAVDRAWER_ITEM_ABOUT = -5;
    protected static final int NAVDRAWER_ITEM_INVALID = -1;
    protected static final int NAVDRAWER_ITEM_SEPARATOR = -2;
    private static final int[] NAVDRAWER_ITEMS = new int[]{
            NAVDRAWER_ITEM_CURRENT_SESSION,
            NAVDRAWER_ITEM_HISTORY,
            NAVDRAWER_ITEM_SEPARATOR,
            NAVDRAWER_ITEM_SETTINGS,
            NAVDRAWER_ITEM_HELP,
            NAVDRAWER_ITEM_ABOUT
    };
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;
    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.current_session,
            R.string.history,
            0,
            R.string.settings,
            R.string.help,
            R.string.about
    };
    private static final int[] NAVDRAWER_ACTIONBAR_TITLE_RES_ID = new int[]{
            R.string.current,
            R.string.history
    };
    private DrawerLayout mDrawerLayout;
    private ScrollView mDrawerScrollView;
    private LinearLayout mDrawerListLinearLayout;

    private Handler mHandler;
    private Toolbar mActionBarToolbar;

    /**
     * Returns the navigation drawer item that corresponds to this Activity.
     * Subclasses of BaseActivity override this to indicate what nav drawer item
     * corresponds to them.
     */
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    protected void onNavDrawerSlide(float offset) {
    }

    protected void onNavDrawerClosed() {
    }

    @Override
    protected boolean hasNavDrawer() {
        return true;
    }

    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            if (mActionBarToolbar != null) {
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }

    /**
     * Sets up the navigation drawer as appropriate.
     */
    private void setupNavDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id
                .activity_drawer_drawerlayout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        Resources resources = getResources();
        if (PrefUtils.getTheme(this) != PrefUtils.Theme.BLACK) {
            mDrawerLayout.setStatusBarBackgroundColor(resources.getColor(R.color.primary_dark));
        }

        mActionBarToolbar.setNavigationIcon(R.drawable.ic_drawer);
        mActionBarToolbar.setNavigationOnClickListener(new View
                .OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.START);
            }
        });

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                onNavDrawerSlide(slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                onNavDrawerClosed();
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        mDrawerScrollView = (ScrollView) findViewById(R.id
                .activity_drawer_drawer_scrollview);
        int actionBarSize = resources.getDimensionPixelSize(R.dimen
                .navigation_drawer_margin);
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        int navDrawerWidthLimit = resources.getDimensionPixelSize(R.dimen.navigation_drawer_limit);
        int navDrawerWidth = displayMetrics.widthPixels - actionBarSize;
        if (navDrawerWidth > navDrawerWidthLimit) {
            navDrawerWidth = navDrawerWidthLimit;
        }
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) mDrawerScrollView.getLayoutParams();
        params.width = navDrawerWidth;
        mDrawerScrollView.setLayoutParams(params);

        mDrawerListLinearLayout = (LinearLayout) findViewById(R.id
                .activity_drawer_drawer_linearlayout);

        mActionBarToolbar.setNavigationIcon(R.drawable.ic_drawer);
        mActionBarToolbar.setNavigationOnClickListener(new View
                .OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.START);
            }
        });

        resetTitle();

        if (!PrefUtils.isWelcomeDone(this)) {
            PrefUtils.markWelcomeDone(this);
            mDrawerLayout.openDrawer(Gravity.START);
        }

        //INFLATE LAYOUTS AND SET CLICK LISTENERS
        for (int i = 0; i < NAVDRAWER_ITEMS.length; i++) {
            final int itemId = NAVDRAWER_ITEMS[i];
            if (itemId == NAVDRAWER_ITEM_SEPARATOR) {
                mDrawerListLinearLayout.addView(getLayoutInflater().inflate(R
                                .layout.list_item_separator,
                        mDrawerListLinearLayout, false));
            } else {
                TextView v = (TextView) getLayoutInflater().inflate(R.layout
                        .list_item_drawer, mDrawerListLinearLayout, false);
                v.setText(NAVDRAWER_TITLE_RES_ID[i]);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onNavDrawerItemClicked(itemId);
                    }
                });
                mDrawerListLinearLayout.addView(v);
            }

        }
    }

    protected void resetTitle() {
        setTitle(NAVDRAWER_ACTIONBAR_TITLE_RES_ID[getSelfNavDrawerItem()]);
        ViewTreeObserver vto = findViewById(android.R.id.content).getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                if (getActionBarToolbar().isTitleTruncated()) {
                    setTitle(null);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    findViewById(android.R.id.content).getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    findViewById(android.R.id.content).getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }

    @Override
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        resetTitle();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavDrawer();
        View mainContent = findViewById(R.id
                .activity_drawer_content_linearlayout);
        mainContent.setAlpha(0);
        mainContent.animate().alpha(1).setDuration
                (MAIN_CONTENT_FADEIN_DURATION);
    }

    private void onNavDrawerItemClicked(final int itemId) {
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(Gravity.START);
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                goToNavDrawerItem(itemId);
            }
        }, NAVDRAWER_LAUNCH_DELAY);
        if (!isSpecialItem(itemId)) {
            View mainContent = findViewById(R.id
                    .activity_drawer_content_linearlayout);
            if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration
                        (MAIN_CONTENT_FADEOUT_DURATION);
            }
        }

        mDrawerLayout.closeDrawer(Gravity.START);
    }

    private boolean isSpecialItem(int itemId) {
        return itemId == NAVDRAWER_ITEM_SETTINGS || itemId ==
                NAVDRAWER_ITEM_HELP || itemId == NAVDRAWER_ITEM_ABOUT;
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity
                .START);
    }

    protected void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(Gravity.START);
        }
    }

    private void goToNavDrawerItem(int itemId) {
        Intent i;
        switch (itemId) {
            case NAVDRAWER_ITEM_CURRENT_SESSION:
                i = new Intent(this, CurrentSessionActivity.class);
                break;
            case NAVDRAWER_ITEM_HISTORY:
                i = new Intent(this, HistorySessionListActivity.class);
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                i = new Intent(this, SettingsActivity.class);
                break;
            case NAVDRAWER_ITEM_ABOUT:
                i = new Intent(this, AboutActivity.class);
                break;
            default:
                Toast.makeText(getApplicationContext(), "Work in Progress",
                        Toast.LENGTH_SHORT).show();
                return;
        }

        startActivity(i);
        //If it is not a special item, finish this activity
        if (!isSpecialItem(itemId)) finish();
    }
}
