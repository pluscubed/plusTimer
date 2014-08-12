package com.pluscubed.plustimer.ui;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentPagerAdapter;
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
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.widget.SlidingTabLayout;

/**
 * Current Session Activity
 */
public class CurrentSessionActivity extends BaseActivity implements CurrentSessionTimerFragment.GetRetainedFragmentCallback, SolveDialog.OnDialogDismissedListener, CreateDialogCallback, CurrentSessionTimerFragment.MenuItemsEnableCallback {
    public static final String DIALOG_SOLVE_TAG = "SOLVE_DIALOG";
    private static final String STATE_MENU_ITEMS_ENABLE_BOOLEAN = "menu_items_enable_boolean";
    private static final String CURRENT_SESSION_TIMER_RETAINED_TAG = "CURRENT_SESSION_TIMER_RETAINED";
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
    public void onDialogDismissed(String displayName, int sessionIndex, int solveIndex, int penalty) {
        Solve solve = PuzzleType.get(displayName).getSession(sessionIndex, this).getSolveByPosition(solveIndex);
        switch (penalty) {
            case SolveDialog.DIALOG_PENALTY_NONE:
                solve.setPenalty(Solve.Penalty.NONE);
                break;
            case SolveDialog.DIALOG_PENALTY_PLUSTWO:
                solve.setPenalty(Solve.Penalty.PLUSTWO);
                break;
            case SolveDialog.DIALOG_PENALTY_DNF:
                solve.setPenalty(Solve.Penalty.DNF);
                break;
            case SolveDialog.DIALOG_RESULT_DELETE:
                PuzzleType.get(displayName).getSession(sessionIndex, this).deleteSolve(solveIndex);
                break;
        }
        if (getCurrentSessionTimerFragment() != null)
            getCurrentSessionTimerFragment().onSessionSolvesChanged();
        if (getSolveListFragment() != null) getSolveListFragment().onSessionSolvesChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_current_session);
        super.onCreate(savedInstanceState);

        if (BuildConfig.USE_CRASHLYTICS) Crashlytics.start(this);

        if (savedInstanceState != null) {
            mMenuItemsEnable = savedInstanceState.getBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN);
        } else {
            for (PuzzleType p : PuzzleType.values()) {
                p.getHistorySessions(this);
            }
        }

        Fragment currentSessionRetainedFragment = getFragmentManager().findFragmentByTag(CURRENT_SESSION_TIMER_RETAINED_TAG);
        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (currentSessionRetainedFragment == null) {
            currentSessionRetainedFragment = new CurrentSessionTimerRetainedFragment();
            getFragmentManager().beginTransaction().add(currentSessionRetainedFragment, CURRENT_SESSION_TIMER_RETAINED_TAG).commit();
        }


        //Set up ViewPager with CurrentSessionAdapter
        ViewPager viewPager = (ViewPager) findViewById(R.id.activity_current_session_viewpager);
        viewPager.setAdapter(new CurrentSessionPagerAdapter(getFragmentManager(), getResources().getStringArray(R.array.current_s_page_titles)));

        //Set up SlidingTabLayout
        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.activity_current_session_slidingtablayout);
        slidingTabLayout.setSelectedIndicatorColors(Color.parseColor("white"));
        slidingTabLayout.setDistributeEvenly(true);
        slidingTabLayout.setCustomTabView(R.layout.sliding_tab_textview, android.R.id.text1);
        slidingTabLayout.setViewPager(viewPager);
        slidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                invalidateOptionsMenu();
                mSelectedPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING || state == ViewPager.SCROLL_STATE_SETTLING) {
                    getCurrentSessionTimerFragment().onSessionSolvesChanged();
                    getCurrentSessionTimerFragment().stopHoldTimer();
                    getSolveListFragment().onSessionSolvesChanged();
                    getSolveListFragment().finishActionMode();
                }
            }
        });
        viewPager.setCurrentItem(0);
    }

    private CurrentSessionTimerFragment getCurrentSessionTimerFragment() {
        return (CurrentSessionTimerFragment) getFragmentManager().findFragmentByTag(makeFragmentName(R.id.activity_current_session_viewpager, 0));
    }

    private SolveListFragment getSolveListFragment() {
        return (SolveListFragment) getFragmentManager().findFragmentByTag(makeFragmentName(R.id.activity_current_session_viewpager, 1));
    }

    @Override
    public CurrentSessionTimerRetainedFragment getCurrentSessionTimerRetainedFragment() {
        return (CurrentSessionTimerRetainedFragment) getFragmentManager().findFragmentByTag(CURRENT_SESSION_TIMER_RETAINED_TAG);
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
        setUpPuzzleSpinner(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuDisplayScramble = menu.findItem(R.id.menu_activity_current_session_scramble_image_menuitem);
        if (menuDisplayScramble != null) {
            menuDisplayScramble.setEnabled(mMenuItemsEnable);
            menuDisplayScramble.getIcon().setAlpha(mMenuItemsEnable ? 255 : 96);
        }
        if (menu.findItem(R.id.menu_activity_current_session_share_menuitem) != null && menu.findItem(R.id.menu_activity_current_session_scramble_image_menuitem) != null) {
            if (mSelectedPosition == 0) {
                menu.findItem(R.id.menu_activity_current_session_share_menuitem).setVisible(false);
                menu.findItem(R.id.menu_activity_current_session_scramble_image_menuitem).setVisible(true);
            } else {
                menu.findItem(R.id.menu_activity_current_session_scramble_image_menuitem).setVisible(false);
                menu.findItem(R.id.menu_activity_current_session_share_menuitem).setVisible(true);
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
        return new int[]{R.id.menu_activity_current_session_scramble_image_menuitem, R.id.menu_activity_current_session_share_menuitem, R.id.menu_activity_current_session_puzzletype_spinner};
    }

    public void setUpPuzzleSpinner(Menu menu) {
        final Spinner menuPuzzleSpinner = (Spinner) menu.findItem(R.id.menu_activity_current_session_puzzletype_spinner).getActionView();

        final ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        getActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
        menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(PuzzleType.get(PuzzleType.CURRENT)), true);
        menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PuzzleType.setCurrentPuzzleType((PuzzleType) parent.getItemAtPosition(position));
                ((CurrentSessionTimerFragment) getFragmentManager().findFragmentByTag(makeFragmentName(R.id.activity_current_session_viewpager, 0))).onSessionChanged();
                ((SolveListFragment) getFragmentManager().findFragmentByTag(makeFragmentName(R.id.activity_current_session_viewpager, 1))).onSessionChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void createSolveDialog(String displayName, int sessionIndex, int solveIndex) {
        DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(DIALOG_SOLVE_TAG);
        if (dialog == null) {
            SolveDialog d = SolveDialog.newInstance(PuzzleType.CURRENT, PuzzleType.CURRENT_SESSION, solveIndex);
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
                    return SolveListFragment.newInstance(true, PuzzleType.CURRENT, PuzzleType.CURRENT_SESSION);
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (mPageTitles.length == 2)
                return mPageTitles[position];
            return null;
        }
    }
}
