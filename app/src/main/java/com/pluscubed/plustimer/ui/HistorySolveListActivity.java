package com.pluscubed.plustimer.ui;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;

/**
 * History SolveList (started onListItemClick HistorySessionListFragment) activity
 */
public class HistorySolveListActivity extends ActionBarActivity
        implements SolveDialogFragment.OnDialogDismissedListener, CreateDialogCallback {

    public static final String EXTRA_HISTORY_SESSION_POSITION = "com.pluscubed.plustimer.history_session_position";
    public static final String EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME = "com.pluscubed.plustimer.history_puzzletype_displayname";

    public static final String HISTORY_DIALOG_SOLVE_TAG = "HISTORY_MODIFY_DIALOG";

    @Override
    public void createSolveDialog(String puzzleTypeName, int sessionIndex, int solveIndex) {
        DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(HISTORY_DIALOG_SOLVE_TAG);
        if (dialog == null) {
            SolveDialogFragment d = SolveDialogFragment.newInstance(PuzzleType.valueOf(puzzleTypeName).toString(), sessionIndex, solveIndex);
            d.show(getFragmentManager(), HISTORY_DIALOG_SOLVE_TAG);
        }
    }

    @Override
    public void onDialogDismissed() {
        if (getSolveListFragment() != null) {
            getSolveListFragment().onSessionSolvesChanged();
        }
    }

    private SolveListFragment getSolveListFragment() {
        return (SolveListFragment) getFragmentManager().findFragmentById(android.R.id.content);
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

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(android.R.id.content);
        int position = getIntent().getIntExtra(EXTRA_HISTORY_SESSION_POSITION, 0);
        String puzzleType = getIntent().getStringExtra(EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME);
        if (fragment == null) {
            fragment = SolveListFragment.newInstance(false, puzzleType, position);
            fm.beginTransaction().add(android.R.id.content, fragment).commit();
        }
        setTitle(PuzzleType.valueOf(puzzleType).getSession(position).getTimestampString(this));
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
