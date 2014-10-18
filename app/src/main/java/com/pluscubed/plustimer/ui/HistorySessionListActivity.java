package com.pluscubed.plustimer.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.pluscubed.plustimer.R;

/**
 * History session list activity
 */

public class HistorySessionListActivity extends BaseActivity {

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_HISTORY;
    }

    @Override
    protected ActionBarWrappedDrawerToggle getWrappedDrawerToggle() {
        return new ActionBarWrappedDrawerToggle() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if (getFragmentManager()
                        .findFragmentById(R.id.activity_history_sessionlist_main_framelayout)
                        != null) {
                    ((HistorySessionListFragment) getFragmentManager()
                            .findFragmentById(R.id.activity_history_sessionlist_main_framelayout))
                            .finishActionMode();
                }
            }
        };
    }

    @Override
    protected int[] getNavDrawerHideActionBarItemIds() {
        return new int[]{R.id.menu_activity_history_sessionlist_puzzletype_spinner};
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu.findItem(R.id.menu_activity_history_sessionlist_puzzletype_spinner) != null) {
            menu.findItem(R.id.menu_activity_history_sessionlist_puzzletype_spinner)
                    .setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.activity_history_sessionlist_main_framelayout);
        if (fragment == null) {
            fragment = new HistorySessionListFragment();
            fm.beginTransaction().add(R.id.activity_history_sessionlist_main_framelayout, fragment)
                    .commit();
        }
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_history_sessionlist;
    }
}
