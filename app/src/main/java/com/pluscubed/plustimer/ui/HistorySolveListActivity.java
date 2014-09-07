package com.pluscubed.plustimer.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;

/**
 * History SolveList (started onListItemClick HistorySessionListFragment) activity
 */
public class HistorySolveListActivity extends Activity implements SolveDialog.OnDialogDismissedListener, CreateDialogCallback {
    public static final String EXTRA_HISTORY_SESSION_POSITION = "com.pluscubed.plustimer.history_session_position";
    public static final String EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME = "com.pluscubed.plustimer.history_puzzletype_displayname";
    public static final String HISTORY_DIALOG_SOLVE_TAG = "HISTORY_MODIFY_DIALOG";

    @Override
    public void createSolveDialog(String displayName, int sessionIndex, int solveIndex) {
        DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(HISTORY_DIALOG_SOLVE_TAG);
        if (dialog == null) {
            SolveDialog d = SolveDialog.newInstance(PuzzleType.get(displayName).toString(), sessionIndex, solveIndex);
            d.show(getFragmentManager(), HISTORY_DIALOG_SOLVE_TAG);
        }
    }

    @Override
    public void onDialogDismissed(String displayName, int sessionIndex, int solveIndex, int penalty) {
        switch (penalty) {
            case SolveDialog.DIALOG_RESULT_DELETE:
                PuzzleType.get(displayName).getSession(sessionIndex).deleteSolve(solveIndex);
                break;
        }

        if (getSolveListFragment() != null) {
            getSolveListFragment().onSessionSolvesChanged();
        }

        setTitle(PuzzleType.get(displayName).getSession(sessionIndex).getTimestampString(this));

    }

    private SolveListFragment getSolveListFragment() {
        return (SolveListFragment) getFragmentManager().findFragmentById(R.id.activity_history_solvelist_framelayout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history_solvelist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_history_solvelist);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.activity_history_solvelist_framelayout);
        int position = getIntent().getIntExtra(EXTRA_HISTORY_SESSION_POSITION, 0);
        String puzzleType = getIntent().getStringExtra(EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME);
        if (fragment == null) {
            fragment = SolveListFragment.newInstance(false, puzzleType, position);
            fm.beginTransaction().add(R.id.activity_history_solvelist_framelayout, fragment).commit();
        }
        setTitle(PuzzleType.get(puzzleType).getSession(position).getTimestampString(this));
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
