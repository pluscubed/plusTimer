package com.pluscubed.plustimer.ui.historysessions;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.ui.DrawerActivity;

/**
 * History session list activity
 */

public class HistorySessionsActivity extends DrawerActivity {

    @Override
    protected void onNavDrawerSlide(float offset) {
        /*if (getFragmentManager()
                .findFragmentById(R.id
                        .activity_history_sessionlist_main_framelayout)
                != null) {
            ((HistorySessionsFragment) getFragmentManager()
                    .findFragmentById(R.id
                            .activity_history_sessionlist_main_framelayout))
                    .finishActionMode();
        }*/
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return R.id.nav_history;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_sessionlist);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.activity_history_sessionlist_main_framelayout);
        if (fragment == null) {
            fragment = new HistorySessionsFragment();
            fm.beginTransaction().add(R.id.activity_history_sessionlist_main_framelayout, fragment)
                    .commit();
        }

        overridePendingTransition(0, 0);
    }
}
