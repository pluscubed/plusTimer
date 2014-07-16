package com.pluscubed.plustimer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;

/**
 * History session activity
 */
public class HistorySessionActivity extends ActionBarActivity {
    public static final String EXTRA_HISTORY_SESSION_POSITION = "com.pluscubed.plustimer.history_session_position";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_session);
        FragmentManager fm = getSupportFragmentManager();
        int position = getIntent().getIntExtra(EXTRA_HISTORY_SESSION_POSITION, 0);
        Fragment fragment = HistorySessionFragment.newInstance(position);
        fm.beginTransaction().add(R.id.activity_history_session_framelayout, fragment).commit();
    }
}
