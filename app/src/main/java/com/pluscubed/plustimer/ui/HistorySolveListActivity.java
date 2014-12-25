package com.pluscubed.plustimer.ui;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;

/**
 * History SolveList (started onListItemClick HistorySessionListFragment)
 * activity
 */
public class HistorySolveListActivity extends ThemableActivity
        implements CreateDialogCallback {

    public static final String EXTRA_HISTORY_SESSION_POSITION = "com" +
            ".pluscubed.plustimer.history_session_position";
    public static final String EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME = "com" +
            ".pluscubed.plustimer.history_puzzletype_displayname";

    public static final String HISTORY_DIALOG_SOLVE_TAG =
            "HISTORY_MODIFY_DIALOG";

    @Override
    public void createSolveDialog(String puzzleTypeName, int sessionIndex,
                                  int solveIndex) {
        DialogFragment dialog = (DialogFragment) getFragmentManager()
                .findFragmentByTag(HISTORY_DIALOG_SOLVE_TAG);
        if (dialog == null) {
            SolveDialogFragment d = SolveDialogFragment.newInstance
                    (PuzzleType.valueOf(puzzleTypeName).toString(),
                            sessionIndex, solveIndex);
            d.show(getFragmentManager(), HISTORY_DIALOG_SOLVE_TAG);
        }
    }

    private SolveListFragment getSolveListFragment() {
        return (SolveListFragment) getFragmentManager().findFragmentById
                (android.R.id.content);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history_solvelist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PuzzleType.initialize(this);
        int position = getIntent().getIntExtra
                (EXTRA_HISTORY_SESSION_POSITION, 0);
        String puzzleType = getIntent().getStringExtra
                (EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME);

        setContentView(R.layout.activity_with_toolbar);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);

        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id
                .activity_with_toolbar_content_framelayout);
        if (f == null) {
            f = SolveListFragment.newInstance(false, puzzleType,
                    position);
            fm.beginTransaction()
                    .replace(R.id.activity_with_toolbar_content_framelayout, f)
                    .commit();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTitle(PuzzleType.valueOf(puzzleType).getSession(position)
                .getTimestampString(this));
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
