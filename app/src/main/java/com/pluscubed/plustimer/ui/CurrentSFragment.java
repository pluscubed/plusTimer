package com.pluscubed.plustimer.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.widget.SlidingTabLayout;

import java.util.ArrayList;

/**
 * Current Session Fragment
 */
public class CurrentSFragment extends Fragment implements CurrentSBaseFragment.GetChildFragmentsListener, CurrentSTimerFragment.MenuItemsEnableListener {
    private static final String STATE_MENU_ITEMS_ENABLE_BOOLEAN = "menu_items_enable_boolean";
    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private boolean mMenuItemsEnable;

    public static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN, mMenuItemsEnable);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuDisplayScramble = menu.findItem(R.id.menu_current_s_toggle_scramble_image_action);

        if (menuDisplayScramble != null) {
            menuDisplayScramble.setEnabled(mMenuItemsEnable);
            menuDisplayScramble.getIcon().setAlpha(mMenuItemsEnable ? 255 : 96);
        }
    }


    @Override
    public ArrayList<CurrentSBaseFragment> getChildFragments() {
        ArrayList<CurrentSBaseFragment> fragments = new ArrayList<CurrentSBaseFragment>();
        for (int i = 0; ; i++) {
            CurrentSBaseFragment fragment = (CurrentSBaseFragment) getChildFragmentManager().findFragmentByTag(makeFragmentName(R.id.fragment_current_s_viewpager, i));
            if (fragment != null) {
                fragments.add(fragment);
                continue;
            }
            break;
        }
        return fragments;
    }

    void updateSessionsToCurrent() {
        for (CurrentSBaseFragment i : getChildFragments()) {
            i.onSessionSolvesChanged();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mMenuItemsEnable = savedInstanceState.getBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN);
        }

    }

    @Override
    public void menuItemsEnable(boolean enable) {
        mMenuItemsEnable = enable;
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_s, container, false);
        mViewPager = (ViewPager) v.findViewById(R.id.fragment_current_s_viewpager);
        mViewPager.setAdapter(new CurrentSessionPagerAdapter(getChildFragmentManager(), getResources().getStringArray(R.array.current_s_page_titles)));
        mSlidingTabLayout = (SlidingTabLayout) v.findViewById(R.id.fragment_current_s_slidingtablayout);
        mSlidingTabLayout.setSelectedIndicatorColors(Color.parseColor("white"));
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setCustomTabView(R.layout.sliding_tab_textview, android.R.id.text1);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                CurrentSTimerFragment currentSTimerFragment = (CurrentSTimerFragment) getChildFragmentManager().findFragmentByTag(CurrentSFragment.makeFragmentName(R.id.fragment_current_s_viewpager, 0));
                if (currentSTimerFragment != null) {
                    currentSTimerFragment.stopHoldTimer();
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0 && getChildFragments().get(1) != null && ((MainActivity.ActionModeNavDrawerCallback) getChildFragments().get(1)).getActionMode() != null) {
                    ((MainActivity.ActionModeNavDrawerCallback) getChildFragments().get(1)).getActionMode().finish();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING || state == ViewPager.SCROLL_STATE_SETTLING) {
                    updateSessionsToCurrent();
                }
            }
        });
        mViewPager.setCurrentItem(0);
        return v;
    }

    public class CurrentSessionPagerAdapter extends FragmentPagerAdapter {
        private String[] mPageTitles;

        public CurrentSessionPagerAdapter(FragmentManager fm, String[] pageTitles) {
            super(fm);
            mPageTitles = pageTitles;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new CurrentSTimerFragment();
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
