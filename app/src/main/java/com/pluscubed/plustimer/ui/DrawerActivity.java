package com.pluscubed.plustimer.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.ui.currentsession.CurrentSessionActivity;
import com.pluscubed.plustimer.ui.historysessions.HistorySessionsActivity;
import com.pluscubed.plustimer.utils.PrefUtils;

/**
 * Base Activity with the Navigation Drawer
 */
public abstract class DrawerActivity extends ThemableActivity {

    private static final String EXTRA_FADEIN = "com.pluscubed.plustimer.EXTRA_FADEIN";


    private static final int NAVDRAWER_LAUNCH_DELAY = 250;
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;
    private static final int[] NAVDRAWER_TOOLBAR_TITLE_RES_ID = new int[]{
            R.string.current,
            R.string.history
    };
    private DrawerLayout mDrawerLayout;

    private Handler mHandler;
    private Toolbar mActionBarToolbar;

    protected abstract int getSelfNavDrawerItem();

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
        Resources resources = getResources();
        if (PrefUtils.getTheme(this) != PrefUtils.Theme.BLACK) {
            mDrawerLayout.setStatusBarBackgroundColor(resources.getColor(R.color.primary_dark));
        }

        mActionBarToolbar.setNavigationIcon(R.drawable.ic_drawer);
        mActionBarToolbar.setNavigationOnClickListener(view ->
                mDrawerLayout.openDrawer(GravityCompat.START));

        mDrawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                onNavDrawerSlide(slideOffset);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                onNavDrawerClosed();
            }
        });

        NavigationView view = (NavigationView) findViewById(R.id.activity_drawer_drawer_navview);
        view.setCheckedItem(getSelfNavDrawerItem());
        view.setNavigationItemSelectedListener(item -> {
            onNavDrawerItemClicked(item.getItemId(), item.getGroupId());

            return isNormalItem(item.getItemId());
        });

        int actionBarSize = resources.getDimensionPixelSize(R.dimen
                .navigation_drawer_margin);
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        int navDrawerWidthLimit = resources.getDimensionPixelSize(R.dimen.navigation_drawer_limit);
        int navDrawerWidth = displayMetrics.widthPixels - actionBarSize;
        if (navDrawerWidth > navDrawerWidthLimit) {
            navDrawerWidth = navDrawerWidthLimit;
        }
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) view.getLayoutParams();
        params.width = navDrawerWidth;
        view.setLayoutParams(params);

        resetTitle();

        if (!PrefUtils.isWelcomeDone(this)) {
            PrefUtils.markWelcomeDone(this);
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    void resetTitle() {
        setTitle(NAVDRAWER_TOOLBAR_TITLE_RES_ID[getSelfNavDrawerItem() == R.id.nav_current ? 0 : 1]);
        final View root = findViewById(android.R.id.content);
        ViewTreeObserver vto = root.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                if (getActionBarToolbar().isTitleTruncated()) {
                    setTitle(null);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    root.getViewTreeObserver()
                            .removeOnGlobalLayoutListener(this);
                } else {
                    root.getViewTreeObserver()
                            .removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    protected void lockDrawer(boolean lock) {
        if (lock) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
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

        if (getIntent().getBooleanExtra(EXTRA_FADEIN, false)) {
            View mainContent = findViewById(R.id.activity_drawer_content_linearlayout);
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        }
    }

    private void onNavDrawerItemClicked(@IdRes int itemId, @IdRes int groupId) {
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        mHandler.postDelayed(() -> goToNavDrawerItem(itemId), NAVDRAWER_LAUNCH_DELAY);
        if (isNormalItem(itemId)) {
            View mainContent = findViewById(R.id
                    .activity_drawer_content_linearlayout);
            if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration
                        (MAIN_CONTENT_FADEOUT_DURATION);
            }
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private boolean isNormalItem(@IdRes int itemId) {
        return itemId != R.id.nav_settings && itemId != R.id.nav_about;
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private void goToNavDrawerItem(@IdRes int itemId) {
        Intent i;
        switch (itemId) {
            case R.id.nav_current:
                i = new Intent(this, CurrentSessionActivity.class);
                i.putExtra(EXTRA_FADEIN, true);
                break;
            case R.id.nav_history:
                i = new Intent(this, HistorySessionsActivity.class);
                i.putExtra(EXTRA_FADEIN, true);
                break;
            case R.id.nav_settings:
                i = new Intent(this, SettingsActivity.class);
                break;
            case R.id.nav_about:
                i = new Intent(this, AboutActivity.class);
                break;
            default:
                Toast.makeText(getApplicationContext(), "Work in Progress",
                        Toast.LENGTH_SHORT).show();
                return;
        }

        startActivity(i);
        //If it is not a special item, finish this activity
        if (isNormalItem(itemId)) finish();
    }
}
