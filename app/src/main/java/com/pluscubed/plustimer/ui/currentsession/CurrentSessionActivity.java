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
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.DrawerActivity;
import com.pluscubed.plustimer.ui.SolveListFragment;
import com.pluscubed.plustimer.ui.SpinnerPuzzleTypeAdapter;
import com.pluscubed.plustimer.ui.widget.LockingViewPager;
import com.pluscubed.plustimer.ui.widget.SlidingTabLayout;
import com.pluscubed.plustimer.utils.PrefUtils;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Current Session Activity
 */
public class CurrentSessionActivity extends DrawerActivity implements
        CurrentSessionTimerFragment.ActivityCallback {

    private static final String STATE_MENU_ITEMS_ENABLE_BOOLEAN =
            "menu_items_enable_boolean";

    private static final String CURRENT_SESSION_TIMER_RETAINED_TAG
            = "CURRENT_SESSION_TIMER_RETAINED";

    private boolean mScrambleImageActionEnable;

    private int mSelectedPage;

    private boolean mInvalidateActionBarOnDrawerClosed;
    private SlidingTabLayout mSlidingTabLayout;
    private LockingViewPager mViewPager;
    private int mContentFrameLayoutHeight;

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

    public Toolbar getActionBarToolbar() {
        return super.getActionBarToolbar();
    }

    @Override
    public FrameLayout getContentFrameLayout() {
        return (FrameLayout) findViewById(R.id
                .activity_current_session_framelayout);
    }

    @Override
    public void playToolbarExitAnimation() {
        final LinearLayout toolbar = (LinearLayout) findViewById(R.id
                .activity_current_session_headerbar);
        final FrameLayout layout = (FrameLayout) findViewById(R.id
                .activity_current_session_framelayout);
        mContentFrameLayoutHeight = layout.getHeight();

        ObjectAnimator exit = ObjectAnimator.ofFloat(toolbar, View.TRANSLATION_Y,
                -toolbar.getHeight());
        exit.setDuration(300);
        exit.setInterpolator(new AccelerateInterpolator());
        exit.addUpdateListener(animation -> {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) layout.getLayoutParams();
            params.height =
                    mContentFrameLayoutHeight - (int) (float) animation.getAnimatedValue();
            params.weight = 0;
            layout.setLayoutParams(params);
            layout.setTranslationY((int) (float) animation.getAnimatedValue());
        });

        AnimatorSet scrambleAnimatorSet = new AnimatorSet();
        scrambleAnimatorSet.play(exit);
        scrambleAnimatorSet.start();
    }

    @Override
    public void playToolbarEnterAnimation() {
        final LinearLayout toolbar = (LinearLayout) findViewById(R.id
                .activity_current_session_headerbar);
        final FrameLayout layout = (FrameLayout) findViewById(R.id
                .activity_current_session_framelayout);

        ObjectAnimator exit = ObjectAnimator.ofFloat(toolbar, View.TRANSLATION_Y, 0f);
        exit.setDuration(300);
        exit.setInterpolator(new DecelerateInterpolator());
        exit.addUpdateListener(animation -> {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) layout.getLayoutParams();
            params.height =
                    mContentFrameLayoutHeight - (int) (float) animation.getAnimatedValue();
            params.weight = 0;
            layout.setLayoutParams(params);
            layout.setTranslationY((int) (float) animation.getAnimatedValue());
        });

        AnimatorSet scrambleAnimatorSet = new AnimatorSet();
        scrambleAnimatorSet.play(exit);
        toolbar.setVisibility(View.VISIBLE);
        scrambleAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (toolbar.getTranslationY() == 0) {
                    LinearLayout.LayoutParams params =
                            (LinearLayout.LayoutParams) layout.getLayoutParams();
                    params.height = 0;
                    params.weight = 1;
                    layout.setLayoutParams(params);
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
        return NAVDRAWER_ITEM_CURRENT_SESSION;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_session);

        //TODO What to do on config changes?
        PuzzleType.initialize(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Object>() {
            @Override
            public void onCompleted() {
                getCurrentSessionTimerFragment().getPresenter().setInitialized();
                getSolveListFragment().setInitialized();
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(CurrentSessionActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            @Override
            public void onNext(Object o) {

            }
        });

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

        //Set up ViewPager with CurrentSessionAdapter
        mViewPager = (LockingViewPager) findViewById(R.id
                .activity_current_session_viewpager);
        mViewPager.setAdapter(new CurrentSessionPagerAdapter
                (getFragmentManager(),
                        getResources().getStringArray(R.array
                                .current_session_page_titles)));

        //Set up SlidingTabLayout
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R
                .id.activity_current_session_slidingtablayout);
        int[] attrs = {R.attr.colorAccent};
        mSlidingTabLayout.setSelectedIndicatorColors(obtainStyledAttributes
                (attrs).getColor(0, Color.BLACK));
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setCustomTabView(R.layout.sliding_tab_textview,
                android.R.id.text1);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager
                .OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mSelectedPage = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING || state ==
                        ViewPager.SCROLL_STATE_SETTLING) {
                    getCurrentSessionTimerFragment().stopHoldTimer();
                    getSolveListFragment().finishActionMode();
                }
            }
        });
        mViewPager.setCurrentItem(0);

        //noinspection ConstantConditions
        getSupportActionBar().setElevation(0);

        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: Only update Spinner, not invalidate whole action bar
        // When puzzle types are enabled/disabled, update Spinner
        queueInvalidateOptionsMenu();
        mViewPager.setPagingEnabled(!PrefUtils.isLockSwipingEnabled(this));
    }

    private CurrentSessionTimerFragment getCurrentSessionTimerFragment() {
        return (CurrentSessionTimerFragment) getFragmentManager()
                .findFragmentByTag(makeFragmentName(R.id
                        .activity_current_session_viewpager, 0));
    }

    private SolveListFragment getSolveListFragment() {
        return (SolveListFragment) getFragmentManager()
                .findFragmentByTag(makeFragmentName(R.id
                        .activity_current_session_viewpager, 1));
    }

    void queueInvalidateOptionsMenu() {
        if (!isNavDrawerOpen()) {
            invalidateOptionsMenu();
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
        outState.putBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN,
                mScrambleImageActionEnable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isNavDrawerOpen()) {
            return true;
        }
        getMenuInflater().inflate(R.menu.menu_current_session, menu);

        if (PuzzleType.getCurrentId() != null) {
            final Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat
                    .getActionView(menu.findItem(R.id.menu_activity_current_session_puzzletype_spinner));
            //noinspection ConstantConditions
            ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter = new
                    SpinnerPuzzleTypeAdapter(getLayoutInflater(),
                    getSupportActionBar().getThemedContext());
            menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
            menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(
                    PuzzleType.getCurrent()), true);
            menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    PuzzleType newPuzzleType = (PuzzleType) parent.getItemAtPosition(position);
                    if (!newPuzzleType.equals(PuzzleType.getCurrent())) {
                        PuzzleType.setCurrent(newPuzzleType.getId());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

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

    public class CurrentSessionPagerAdapter extends FragmentPagerAdapter {

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
                    return new SolveListFragment();
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
