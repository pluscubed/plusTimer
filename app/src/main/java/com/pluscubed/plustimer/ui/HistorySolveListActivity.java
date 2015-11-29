package com.pluscubed.plustimer.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.solvelist.SolveListFragment;

/**
 * History SolveList (started onListItemClick HistorySessionListFragment)
 * activity
 */
public class HistorySolveListActivity extends ThemableActivity {

    public static final String EXTRA_HISTORY_SESSION_ID = "com" +
            ".pluscubed.plustimer.history_session_position";
    public static final String EXTRA_HISTORY_PUZZLETYPE_ID = "com" +
            ".pluscubed.plustimer.history_puzzletype_displayname";

    private static final String HISTORY_DIALOG_SOLVE_TAG =
            "HISTORY_MODIFY_DIALOG";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history_solvelist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_toolbar);

        String sessionId = getIntent().getStringExtra(EXTRA_HISTORY_SESSION_ID);
        String puzzleType = getIntent().getStringExtra(EXTRA_HISTORY_PUZZLETYPE_ID);

        //TODO
/*
        PuzzleType.get(puzzleType).initializeAllSessionData();*/

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id
                .activity_with_toolbar_content_framelayout);
        if (f == null) {
            f = new SolveListFragment();
            fm.beginTransaction()
                    .replace(R.id.activity_with_toolbar_content_framelayout, f)
                    .commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        PuzzleType.get(puzzleType).getSession(sessionId).subscribe(session -> {
            setTitle(session.getTimestampString(HistorySolveListActivity.this));
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
