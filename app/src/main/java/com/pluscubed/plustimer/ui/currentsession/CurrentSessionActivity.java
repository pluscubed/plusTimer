package com.pluscubed.plustimer.ui.currentsession;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.FragmentStatePagerAdapter;
import com.pluscubed.plustimer.ui.SpinnerPuzzleTypeAdapter;
import com.pluscubed.plustimer.ui.basedrawer.DrawerActivity;
import com.pluscubed.plustimer.ui.currentsessiontimer.CurrentSessionTimerFragment;
import com.pluscubed.plustimer.ui.currentsessiontimer.CurrentSessionTimerRetainedFragment;
import com.pluscubed.plustimer.ui.solvelist.SolveListFragment;
import com.pluscubed.plustimer.ui.solvelist.SolveListPresenter;
import com.pluscubed.plustimer.ui.widget.LockingViewPager;
import com.pluscubed.plustimer.ui.widget.SlidingTabLayout;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.List;

/**
 * Current Session Activity
 */
public class CurrentSessionActivity extends DrawerActivity<CurrentSessionPresenter, CurrentSessionView> implements
        CurrentSessionTimerFragment.ActivityCallback, CurrentSessionView {

    private static final String STATE_MENU_ITEMS_ENABLE_BOOLEAN = "menu_items_enable_boolean";

    private static final String CURRENT_SESSION_TIMER_RETAINED_TAG = "CURRENT_SESSION_TIMER_RETAINED";
    private static final String TAG = "CurrentSessionActivity";
    private boolean mScrambleImageActionEnable;
    private int mSelectedPage;
    private boolean mInvalidateActionBarOnDrawerClosed;

    private SlidingTabLayout mSlidingTabLayout;
    private LockingViewPager mViewPager;
    private LinearLayout mAppBar;
    private FrameLayout mContentFrame;

    private int mContentFrameLayoutHeight;

    private SpinnerPuzzleTypeAdapter mPuzzleSpinnerAdapter;
    private Spinner mPuzzleSpinner;
    private int mPuzzleSpinnerPosition;
    private List<PuzzleType> mPuzzleSpinnerList;

    @Override
    public FrameLayout getContentFrameLayout() {
        return (FrameLayout) findViewById(R.id
                .activity_current_session_framelayout);
    }

    @Override
    public void playToolbarExitAnimation() {
        mContentFrameLayoutHeight = mContentFrame.getHeight();

        ObjectAnimator exit = ObjectAnimator.ofFloat(mAppBar, View.TRANSLATION_Y,
                -mAppBar.getHeight() - Utils.convertDpToPx(this, 8));
        exit.setDuration(300);
        exit.setInterpolator(new FastOutSlowInInterpolator());
        exit.addUpdateListener(animation -> {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) mContentFrame.getLayoutParams();
            params.height =
                    mContentFrameLayoutHeight - (int) (float) animation.getAnimatedValue();
            params.weight = 0;
            mContentFrame.setLayoutParams(params);
            mContentFrame.setTranslationY((int) (float) animation.getAnimatedValue());
        });

        AnimatorSet scrambleAnimatorSet = new AnimatorSet();
        scrambleAnimatorSet.play(exit);
        scrambleAnimatorSet.start();
    }

    @Override
    public void playToolbarEnterAnimation() {
        ObjectAnimator exit = ObjectAnimator.ofFloat(mAppBar, View.TRANSLATION_Y, 0f);
        exit.setDuration(300);
        exit.setInterpolator(new FastOutSlowInInterpolator());
        exit.addUpdateListener(animation -> {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) mContentFrame.getLayoutParams();
            params.height =
                    mContentFrameLayoutHeight - (int) (float) animation.getAnimatedValue();
            params.weight = 0;
            mContentFrame.setLayoutParams(params);
            mContentFrame.setTranslationY((int) (float) animation.getAnimatedValue());
        });

        AnimatorSet scrambleAnimatorSet = new AnimatorSet();
        scrambleAnimatorSet.play(exit);
        mAppBar.setVisibility(View.VISIBLE);
        scrambleAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAppBar.getTranslationY() == 0) {
                    LinearLayout.LayoutParams params =
                            (LinearLayout.LayoutParams) mContentFrame.getLayoutParams();
                    params.height = 0;
                    params.weight = 1;
                    mContentFrame.setLayoutParams(params);
                }
            }
        });
        scrambleAnimatorSet.start();
    }

    @Override
    public void lockDrawerAndViewPager(boolean lock) {
        mSlidingTabLayout.setClickEnabled(!lock);
        if (!PrefUtils.isLockSwipingEnabled(this))
            mViewPager.setPagingEnabled(!lock);
        lockDrawer(lock);
    }

    @Override
    public CurrentSessionTimerRetainedFragment getTimerRetainedFragment() {
        return (CurrentSessionTimerRetainedFragment)
                getFragmentManager().findFragmentByTag(CURRENT_SESSION_TIMER_RETAINED_TAG);
    }

    @Override
    public int getSelfNavDrawerItem() {
        return R.id.nav_current;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_PlusTimer_WithNavDrawer);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_session);

        if (savedInstanceState != null) {
            mScrambleImageActionEnable = savedInstanceState.getBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN);
        }

        Fragment retainedFragment =
                getFragmentManager().findFragmentByTag(CURRENT_SESSION_TIMER_RETAINED_TAG);
        // If the Fragment is null, create and add it
        if (retainedFragment == null) {
            retainedFragment = new CurrentSessionTimerRetainedFragment();
            getFragmentManager().beginTransaction().add(retainedFragment,
                    CURRENT_SESSION_TIMER_RETAINED_TAG).commit();
        }

        mAppBar = (LinearLayout) findViewById(R.id.activity_current_session_appbar);
        mContentFrame = (FrameLayout) findViewById(R.id.activity_current_session_framelayout);

        //Set up ViewPager with CurrentSessionAdapter
        mViewPager = (LockingViewPager) findViewById(R.id.activity_current_session_viewpager);
        mViewPager.setAdapter(
                new CurrentSessionPagerAdapter(
                        getFragmentManager(),
                        getResources().getStringArray(R.array.current_session_page_titles)
                )
        );

        //Set up SlidingTabLayout
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.activity_current_session_slidingtablayout);
        int[] attrs = {R.attr.colorAccent};
        mSlidingTabLayout.setSelectedIndicatorColors(obtainStyledAttributes(attrs).getColor(0, Color.BLACK));
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setCustomTabView(R.layout.sliding_tab_textview, android.R.id.text1);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mSelectedPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING || state == ViewPager.SCROLL_STATE_SETTLING) {
                    getCurrentSessionTimerFragment().stopHoldTimer();
                    getSolveListFragment().finishActionMode();
                }
            }
        });
        mViewPager.setCurrentItem(0);

        mPuzzleSpinner = (Spinner) findViewById(R.id.activity_current_session_puzzlespinner);
        //noinspection ConstantConditions
        mPuzzleSpinnerAdapter = new SpinnerPuzzleTypeAdapter(getLayoutInflater(), getSupportActionBar().getThemedContext());
        mPuzzleSpinner.setAdapter(mPuzzleSpinnerAdapter);

        if (mPuzzleSpinnerList != null) {
            mPuzzleSpinnerAdapter.addAll(mPuzzleSpinnerList);
            mPuzzleSpinner.setSelection(mPuzzleSpinnerPosition);
        }

        //noinspection ConstantConditions
        getSupportActionBar().setElevation(0);
    }

    @Override
    protected PresenterFactory<CurrentSessionPresenter> getPresenterFactory() {
        return new CurrentSessionPresenter.Factory();
    }

    @Override
    protected void onPresenterPrepared(CurrentSessionPresenter presenter) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: Only update Spinner, not invalidate whole action bar
        // When puzzle types are enabled/disabled, update Spinner
        queueInvalidateOptionsMenu();
        mViewPager.setPagingEnabled(!PrefUtils.isLockSwipingEnabled(this));
    }

    public CurrentSessionTimerFragment getCurrentSessionTimerFragment() {
        return (CurrentSessionTimerFragment) getFragmentManager()
                .findFragmentByTag(FragmentStatePagerAdapter.makeFragmentName(0));
    }

    public SolveListFragment getSolveListFragment() {
        return (SolveListFragment) getFragmentManager()
                .findFragmentByTag(FragmentStatePagerAdapter.makeFragmentName(1));
    }

    void queueInvalidateOptionsMenu() {
        if (!isNavDrawerOpen()) {
            supportInvalidateOptionsMenu();
        } else {
            // Workaround for weird bug where calling invalidateOptionsMenu()
            // while nav drawer is open doesn't call onCreateOptionsMenu()
            mInvalidateActionBarOnDrawerClosed = true;
        }
    }

    @Override
    public void enableMenuItems(boolean enable) {
        mScrambleImageActionEnable = enable;
        queueInvalidateOptionsMenu();
    }

    @Override
    protected void onNavDrawerClosed() {
        if (mInvalidateActionBarOnDrawerClosed) {
            invalidateOptionsMenu();
            mInvalidateActionBarOnDrawerClosed = false;
        }
    }

    @Override
    protected void onNavDrawerSlide(float offset) {
        getSolveListFragment().finishActionMode();
        getCurrentSessionTimerFragment().stopHoldTimer();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN, mScrambleImageActionEnable);
    }


    @Override
    public void initPuzzleSpinner(List<PuzzleType> list, int selectedPosition) {
        mPuzzleSpinnerList = list;
        mPuzzleSpinnerPosition = selectedPosition;

        mPuzzleSpinnerAdapter.clear();
        mPuzzleSpinnerAdapter.addAll(list);
        mPuzzleSpinnerAdapter.notifyDataSetChanged();
        mPuzzleSpinner.setSelection(selectedPosition, true);
        mPuzzleSpinner.setOnItemSelectedListener(new AdapterView
                .OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                presenter.onPuzzleSelected((PuzzleType) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isNavDrawerOpen()) {
            return true;
        }
        getMenuInflater().inflate(R.menu.menu_current_session, menu);

        MenuItem displayScrambleImage = menu.findItem(
                R.id.menu_activity_current_session_scramble_image_menuitem);
        MenuItem share = menu.findItem(R.id.menu_solvelist_share_menuitem);
        MenuItem addSolve = menu.findItem(R.id.menu_solvelist_add_menuitem);

        if (displayScrambleImage != null) {
            displayScrambleImage.setEnabled(mScrambleImageActionEnable);
            displayScrambleImage.getIcon()
                    .setAlpha(mScrambleImageActionEnable ? 255 : 96);
        }

        if (share != null && displayScrambleImage != null && addSolve != null) {
            if (mSelectedPage == 0) {
                share.setVisible(false);
                displayScrambleImage.setVisible(true);
                addSolve.setVisible(false);
            } else {
                displayScrambleImage.setVisible(false);
                share.setVisible(true);
                addSolve.setVisible(true);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    public class CurrentSessionPagerAdapter extends FragmentStatePagerAdapter {

        private final String[] mPageTitles;

        public CurrentSessionPagerAdapter(FragmentManager fm,
                                          String[] pageTitles) {
            super(fm);
            mPageTitles = pageTitles;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new CurrentSessionTimerFragment();
                case 1:
                    return SolveListPresenter.newInstance(true);
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
