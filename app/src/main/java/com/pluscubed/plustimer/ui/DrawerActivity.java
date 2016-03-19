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

    private static final String EXTRA_CLOSE_DRAWER = "com.pluscubed.plustimer.EXTRA_CLOSE_DRAWER";

    private static final int[] NAVDRAWER_TOOLBAR_TITLE_RES_ID = new int[]{
            R.string.current,
            R.string.history
    };
    private DrawerLayout mDrawerLayout;

    private Handler mHandler;
    private Toolbar mToolbar;

    protected abstract int getSelfNavDrawerItem();

    protected void onNavDrawerSlide(float offset) {
    }

    protected void onNavDrawerClosed() {
    }

    @Override
    protected boolean hasNavDrawer() {
        return true;
    }

    protected Toolbar getToolbar() {
        if (mToolbar == null) {
            mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            if (mToolbar != null) {
                setSupportActionBar(mToolbar);
            }
        }
        return mToolbar;
    }

    /**
     * Sets up the navigation drawer as appropriate.
     */
    private void setupNavDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id
                .activity_drawer_drawerlayout);
        Resources resources = getResources();

        mToolbar.setNavigationIcon(R.drawable.ic_drawer);
        mToolbar.setNavigationOnClickListener(view ->
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
                if (getToolbar().isTitleTruncated()) {
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
        getToolbar();
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

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_CLOSE_DRAWER, false)) {
            mDrawerLayout.openDrawer(GravityCompat.START);

            mDrawerLayout.post(() -> {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            });
        }
    }

    private void onNavDrawerItemClicked(@IdRes int itemId, @IdRes int groupId) {
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        goToNavDrawerItem(itemId);
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
                i.putExtra(EXTRA_CLOSE_DRAWER, true);
                break;
            case R.id.nav_history:
                i = new Intent(this, HistorySessionsActivity.class);
                i.putExtra(EXTRA_CLOSE_DRAWER, true);
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

        if (isNormalItem(itemId)) {
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        } else {
            mHandler.postDelayed(() -> startActivity(i), 250);
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }
}
