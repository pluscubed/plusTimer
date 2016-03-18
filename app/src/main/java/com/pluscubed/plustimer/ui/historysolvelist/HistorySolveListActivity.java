package com.pluscubed.plustimer.ui.historysolvelist;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.ThemableActivity;
import com.pluscubed.plustimer.ui.solvelist.SolveListPresenter;

/**
 * History SolveList (started onListItemClick HistorySessionListFragment)
 * activity
 */
public class HistorySolveListActivity extends ThemableActivity {

    public static final String EXTRA_HISTORY_SESSION_ID
            = "com.pluscubed.plustimer.history_session";
    public static final String EXTRA_HISTORY_PUZZLETYPE_ID
            = "com.pluscubed.plustimer.history_puzzletype";

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id
                .activity_with_toolbar_content_framelayout);
        if (f == null) {
            f = SolveListPresenter.newInstance(false, puzzleType, sessionId);
            fm.beginTransaction()
                    .replace(R.id.activity_with_toolbar_content_framelayout, f)
                    .commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        PuzzleType.get(puzzleType).getSessionDeferred(this, sessionId)
                .flatMap(session -> session.getTimestampString(this))
                .subscribe(this::setTitle);
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
