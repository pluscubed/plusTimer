package com.pluscubed.plustimer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;


public class OldTimerActivity extends ActionBarActivity {

    private static final String STATE_SELECTED_TAB = "selected_tab";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, getSupportActionBar()
                .getSelectedNavigationIndex());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.addTab(
                actionBar
                        .newTab()
                        .setText(R.string.timer_tab)
                        .setTabListener(new TabListener<CurrentSTimerFragment>(this, "timer", CurrentSTimerFragment.class))
        );

        actionBar.addTab(
                actionBar
                        .newTab()
                        .setText(R.string.session_tab)
                        .setTabListener(new TabListener<CurrentSDetailsListFragment>(this, "session", CurrentSDetailsListFragment.class))
        );

        if (savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_TAB));
        }
    }


    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private final ActionBarActivity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private Fragment mFragment;

        public TabListener(ActionBarActivity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                fragmentTransaction.add(android.R.id.content, mFragment, mTag);
            } else {
                fragmentTransaction.attach(mFragment);
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            if (mFragment != null)
                fragmentTransaction.detach(mFragment);
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

        }
    }
}
