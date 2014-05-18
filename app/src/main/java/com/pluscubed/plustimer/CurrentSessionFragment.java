package com.pluscubed.plustimer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Current Session Fragment
 */
public class CurrentSessionFragment extends Fragment {
    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_current_session, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mViewPager = (ViewPager) view.findViewById(R.id.current_session_viewpager);
        mViewPager.setAdapter(new CurrentSessionFragmentStatePagerAdapter(getActivity().getSupportFragmentManager()));
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.current_session_sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
    }

    public static class CurrentSessionFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

        private FragmentManager fm;

        public CurrentSessionFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
            this.fm = fm;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return super.instantiateItem(container, position);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = fm.findFragmentByTag(makeFragmentName(R.id.current_session_viewpager, position));
            if (fragment == null) {
                switch (position) {
                    case 0:
                        return new TimerFragment();
                    case 1:
                        return new SessionDetailsListFragment();
                }
            }
            return fragment;
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
