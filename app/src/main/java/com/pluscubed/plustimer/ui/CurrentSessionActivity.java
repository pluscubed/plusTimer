package com.pluscubed.plustimer.ui;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.crashlytics.android.Crashlytics;
import com.pluscubed.plustimer.BuildConfig;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.widget.SlidingTabLayout;

import io.fabric.sdk.android.Fabric;

/**
 * Current Session Activity
 */
public class CurrentSessionActivity extends BaseActivity implements SolveDialogFragment.OnDialogDismissedListener, CreateDialogCallback, CurrentSessionTimerFragment.MenuItemsEnableCallback {

    public static final String DIALOG_SOLVE_TAG = "SOLVE_DIALOG";

    private static final String STATE_MENU_ITEMS_ENABLE_BOOLEAN = "menu_items_enable_boolean";

    private boolean mMenuItemsEnable;

    private int mSelectedPosition;

    public static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_CURRENT_SESSION;
    }

    @Override
    public void onDialogDismissed() {
        if (getCurrentSessionTimerFragment() != null) {
            getCurrentSessionTimerFragment().onSessionSolvesChanged();
        }
        if (getSolveListFragment() != null) {
            getSolveListFragment().onSessionSolvesChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //update puzzle spinner in case settings were changed
        invalidateOptionsMenu();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PuzzleType.initialize(this);

        if (BuildConfig.USE_CRASHLYTICS) {
            Fabric.with(this, new Crashlytics());
        }

        if (savedInstanceState != null) {
            mMenuItemsEnable = savedInstanceState.getBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN);
        }

        //Set up ViewPager with CurrentSessionAdapter
        ViewPager viewPager = (ViewPager) findViewById(R.id.activity_current_session_viewpager);
        viewPager.setAdapter(new CurrentSessionPagerAdapter(getFragmentManager(),
                getResources().getStringArray(R.array.current_s_page_titles)));

        //Set up SlidingTabLayout
        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.activity_current_session_slidingtablayout);
        slidingTabLayout.setSelectedIndicatorColors(Color.parseColor("white"));
        slidingTabLayout.setDistributeEvenly(true);
        slidingTabLayout.setCustomTabView(R.layout.sliding_tab_textview, android.R.id.text1);
        slidingTabLayout.setViewPager(viewPager);
        slidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
                mSelectedPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING
                        || state == ViewPager.SCROLL_STATE_SETTLING) {
                    getCurrentSessionTimerFragment().onSessionSolvesChanged();
                    getCurrentSessionTimerFragment().stopHoldTimer();
                    getSolveListFragment().onSessionSolvesChanged();
                    getSolveListFragment().finishActionMode();
                }
            }
        });
        viewPager.setCurrentItem(0);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_current_session;
    }

    private CurrentSessionTimerFragment getCurrentSessionTimerFragment() {
        return (CurrentSessionTimerFragment) getFragmentManager()
                .findFragmentByTag(makeFragmentName(R.id.activity_current_session_viewpager, 0));
    }

    private SolveListFragment getSolveListFragment() {
        return (SolveListFragment) getFragmentManager()
                .findFragmentByTag(makeFragmentName(R.id.activity_current_session_viewpager, 1));
    }

    @Override
    public void enableMenuItems(boolean enable) {
        mMenuItemsEnable = enable;
        invalidateOptionsMenu();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN, mMenuItemsEnable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isNavDrawerOpen()) {
            return true;
        }
        getMenuInflater().inflate(R.menu.menu_current_session, menu);

        Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_activity_current_session_puzzletype_spinner));
        ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter = new SpinnerPuzzleTypeAdapter(getLayoutInflater(), getSupportActionBar().getThemedContext());
        menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
        menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(PuzzleType.getCurrent()), true);
        menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PuzzleType.setCurrent((PuzzleType) parent.getItemAtPosition(position), CurrentSessionActivity.this);
                ((CurrentSessionTimerFragment) getFragmentManager().findFragmentByTag(makeFragmentName(R.id.activity_current_session_viewpager, 0))).onSessionChanged();
                ((SolveListFragment) getFragmentManager().findFragmentByTag(makeFragmentName(R.id.activity_current_session_viewpager, 1))).onSessionChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuDisplayScramble = menu
                .findItem(R.id.menu_activity_current_session_scramble_image_menuitem);
        if (menuDisplayScramble != null) {
            menuDisplayScramble.setEnabled(mMenuItemsEnable);
            menuDisplayScramble.getIcon().setAlpha(mMenuItemsEnable ? 255 : 96);
        }
        if (menu.findItem(R.id.menu_solvelist_share_menuitem) != null
                && menu.findItem(R.id.menu_activity_current_session_scramble_image_menuitem) != null
                && menu.findItem(R.id.menu_solvelist_add_menuitem) != null) {
            if (mSelectedPosition == 0) {
                menu.findItem(R.id.menu_solvelist_share_menuitem).setVisible(false);
                menu.findItem(R.id.menu_activity_current_session_scramble_image_menuitem)
                        .setVisible(true);
                menu.findItem(R.id.menu_solvelist_add_menuitem).setVisible(false);
            } else {
                menu.findItem(R.id.menu_activity_current_session_scramble_image_menuitem)
                        .setVisible(false);
                menu.findItem(R.id.menu_solvelist_share_menuitem).setVisible(true);
                //TODO: Set visible to true once dialog editing is fully implemented
                menu.findItem(R.id.menu_solvelist_add_menuitem).setVisible(false);
            }
        }

        if (menu.findItem(R.id.menu_activity_current_session_puzzletype_spinner) != null) {
            menu.findItem(R.id.menu_activity_current_session_puzzletype_spinner).setVisible(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    protected ActionBarWrappedDrawerToggle getWrappedDrawerToggle() {
        return new ActionBarWrappedDrawerToggle() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                getSolveListFragment().finishActionMode();
                getCurrentSessionTimerFragment().stopHoldTimer();
            }
        };
    }

    @Override
    protected int[] getNavDrawerHideActionBarItemIds() {
        return new int[]{R.id.menu_activity_current_session_scramble_image_menuitem,
                R.id.menu_solvelist_share_menuitem,
                R.id.menu_activity_current_session_puzzletype_spinner};
    }


    @Override
    public void createSolveDialog(String displayName, int sessionIndex, int solveIndex) {
        DialogFragment dialog = (DialogFragment) getFragmentManager()
                .findFragmentByTag(DIALOG_SOLVE_TAG);
        if (dialog == null) {
            SolveDialogFragment d = SolveDialogFragment.newInstance(PuzzleType.getCurrent().name(), PuzzleType.CURRENT_SESSION, solveIndex);
            d.show(getFragmentManager(), DIALOG_SOLVE_TAG);
        }
    }

    public class CurrentSessionPagerAdapter extends FragmentPagerAdapter {

        private final String[] mPageTitles;

        public CurrentSessionPagerAdapter(FragmentManager fm, String[] pageTitles) {
            super(fm);
            mPageTitles = pageTitles;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new CurrentSessionTimerFragment();
                case 1:
                    return SolveListFragment.newInstance(true, PuzzleType.getCurrent().name(), PuzzleType.CURRENT_SESSION);
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (mPageTitles.length == 2) {
                return mPageTitles[position];
            }
            return null;
        }
    }
}
