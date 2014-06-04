package com.pluscubed.plustimer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Current Session Fragment
 */
public class CurrentSFragment extends Fragment {
    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private boolean mMenuItemsEnable;

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_current_s, menu);

        final Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_current_s_puzzletype_spinner));

        final ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        ((ActionBarActivity) getActivity()).getSupportActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);


        menuPuzzleSpinner.post(new Runnable() {
            @Override
            public void run() {
                menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(PuzzleType.sCurrentPuzzleType), true);
                menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (menuPuzzleSpinner.getSelectedItemPosition() != puzzleTypeSpinnerAdapter.getPosition(PuzzleType.sCurrentPuzzleType)) {

                            PuzzleType.sCurrentPuzzleType = (PuzzleType) parent.getItemAtPosition(position);
                            ((CurrentSTimerFragment) getViewPagerFragment(0)).onPuzzleTypeChanged();
                            ((CurrentSDetailsListFragment) getViewPagerFragment(1)).updateSession();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_current_s_puzzletype_spinner));
        MenuItem menuDisplayScramble = menu.findItem(R.id.menu_current_s_toggle_scramble_image_action);
        if (menuPuzzleSpinner != null && menuDisplayScramble != null) {
            menuPuzzleSpinner.setEnabled(mMenuItemsEnable);
            menuDisplayScramble.setEnabled(mMenuItemsEnable);
        }
    }

    public Fragment getViewPagerFragment(int position) {
        String name = makeFragmentName(R.id.fragment_current_s_viewpager, position);
        return getChildFragmentManager().findFragmentByTag(name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_current_s_toggle_scramble_image_action:
                ((CurrentSTimerFragment) getViewPagerFragment(0)).toggleScrambleImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateFragments() {
        ((CurrentSTimerFragment) getViewPagerFragment(0)).updateSession();
        ((CurrentSDetailsListFragment) getViewPagerFragment(1)).updateSession();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        PuzzleType.sCurrentPuzzleType = PuzzleType.THREE;
        PuzzleType.sCurrentPuzzleType.resetSession();
    }

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
        mViewPager.setAdapter(new CurrentSessionPagerAdapter(getChildFragmentManager()));
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.fragment_current_s_slidingtablayout);
        mSlidingTabLayout.setViewPager(mViewPager);
        mViewPager.setCurrentItem(0);
    }

    public static class CurrentSessionPagerAdapter extends FragmentPagerAdapter {

        public CurrentSessionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
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
            switch (position) {
                case 0:
                    return "Timer";
                case 1:
                    return "Session Details";
            }
            return null;
        }
    }
}
