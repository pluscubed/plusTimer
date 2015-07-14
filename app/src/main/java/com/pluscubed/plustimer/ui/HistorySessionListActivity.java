package com.pluscubed.plustimer.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;

/**
 * History session list activity
 */

public class HistorySessionListActivity extends DrawerActivity {

    @Override
    protected void onNavDrawerSlide(float offset) {
        if (getFragmentManager()
                .findFragmentById(R.id
                        .activity_history_sessionlist_main_framelayout)
                != null) {
            ((HistorySessionListFragment) getFragmentManager()
                    .findFragmentById(R.id
                            .activity_history_sessionlist_main_framelayout))
                    .finishActionMode();
        }
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_HISTORY;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_sessionlist);
        PuzzleType.initialize(this);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id
                .activity_history_sessionlist_main_framelayout);
        if (fragment == null) {
            fragment = new HistorySessionListFragment();
            fm.beginTransaction().add(R.id
                    .activity_history_sessionlist_main_framelayout, fragment)
                    .commit();
        }

        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PuzzleType.deinitialize();
    }
}
