package com.pluscubed.plustimer;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import java.util.ArrayList;

/**
 * Current Session Fragment
 */
public class CurrentSFragment extends Fragment implements CurrentSBaseFragment.GetChildFragmentsListener, CurrentSTimerFragment.MenuItemsEnableListener {
    private static final String STATE_MENU_ITEMS_ENABLE_BOOLEAN = "menu_items_enable_boolean";
    private static final String ARG_CURRENT_S_TIMER_STATE = "state";
    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private boolean mMenuItemsEnable;

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }


    public static CurrentSFragment newInstance(SavedState state) {
        CurrentSFragment fragment = new CurrentSFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CURRENT_S_TIMER_STATE, state);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MENU_ITEMS_ENABLE_BOOLEAN, mMenuItemsEnable);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuPuzzleMenuItem = menu.findItem(R.id.menu_current_s_puzzletype_spinner);
        MenuItem menuDisplayScramble = menu.findItem(R.id.menu_current_s_toggle_scramble_image_action);

        if (menuPuzzleMenuItem != null) {
            Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menuPuzzleMenuItem);
            menuPuzzleSpinner.setEnabled(mMenuItemsEnable);
        }
        if (menuDisplayScramble != null) {
            menuDisplayScramble.setEnabled(mMenuItemsEnable);
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
        return inflater.inflate(R.layout.fragment_current_s, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mViewPager = (ViewPager) view.findViewById(R.id.fragment_current_s_viewpager);
        SavedState savedState = null;
        if (getArguments() != null) {
            savedState = getArguments().getParcelable(ARG_CURRENT_S_TIMER_STATE);
        }
        mViewPager.setAdapter(new CurrentSessionPagerAdapter(savedState, getChildFragmentManager(), getResources().getStringArray(R.array.current_s_page_titles)));
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.fragment_current_s_slidingtablayout);
        mSlidingTabLayout.setViewPager(mViewPager);
        mViewPager.setCurrentItem(0);
        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING || state == ViewPager.SCROLL_STATE_SETTLING) {
                    updateSessionsToCurrent();
                }
            }
        });
        mSlidingTabLayout.setSelectedIndicatorColors(Color.parseColor("#2d2d2d"));
    }

    public class CurrentSessionPagerAdapter extends FragmentPagerAdapter {
        private String[] mPageTitles;
        private SavedState mTimerSavedState;

        public CurrentSessionPagerAdapter(SavedState savedState, FragmentManager fm, String[] pageTitles) {
            super(fm);
            mPageTitles = pageTitles;
            mTimerSavedState = savedState;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if (mTimerSavedState != null) {
                        CurrentSTimerFragment fragment = CurrentSTimerFragment.newInstance(mTimerSavedState);
                        mTimerSavedState = null;
                        return fragment;
                    }
                    return new CurrentSTimerFragment();
                case 1:
                    return new CurrentSDetailsListFragment();
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
