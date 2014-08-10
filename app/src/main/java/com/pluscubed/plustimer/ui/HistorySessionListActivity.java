/*
package com.pluscubed.plustimer.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Solve;

*/
/**
 * History session activity
 *//*

public class HistorySessionListActivity extends Activity implements SolveDialog.OnDialogDismissedListener {
    public static final String EXTRA_HISTORY_SESSION_POSITION = "com.pluscubed.plustimer.history_session_position";
    public static final String EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME = "com.pluscubed.plustimer.history_puzzletype_displayname";
    public static final String HISTORY_DIALOG_FRAGMENT_TAG = "HISTORY_MODIFY_DIALOG";


    public void showCurrentSolveDialog(String displayName, int sessionIndex, int solveIndex) {
        DialogFragment dialog = (DialogFragment) getFragmentManager().findFragmentByTag(HISTORY_DIALOG_FRAGMENT_TAG);
        if (dialog == null) {
            SolveDialog d = SolveDialog.newInstance(PuzzleType.get(displayName).toString(), sessionIndex, solveIndex);
            d.show(getFragmentManager(), HISTORY_DIALOG_FRAGMENT_TAG);
        }
    }

    @Override
    public void onDialogDismissed(String displayName, int sessionIndex, int solveIndex, int penalty) {
        Solve solve = PuzzleType.get(displayName).getSession(sessionIndex, this).getSolveByPosition(solveIndex);
        switch (penalty) {
            case SolveDialog.DIALOG_PENALTY_NONE:
                solve.setPenalty(Solve.Penalty.NONE);
                break;
            case SolveDialog.DIALOG_PENALTY_PLUSTWO:
                solve.setPenalty(Solve.Penalty.PLUSTWO);
                break;
            case SolveDialog.DIALOG_PENALTY_DNF:
                solve.setPenalty(Solve.Penalty.DNF);
                break;
            case SolveDialog.DIALOG_RESULT_DELETE:
                PuzzleType.get(displayName).getSession(sessionIndex, this).deleteSolve(solveIndex);
                break;
        }

        if (getFragmentManager().findFragmentById(R.id.activity_history_solvelist_framelayout) != null) {
            ((SolveListFragment) getFragmentManager().findFragmentById(R.id.activity_history_solvelist_framelayout)).onSessionSolvesChanged();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_session);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.activity_history_solvelist_framelayout);
        if (fragment == null) {
            int position = getIntent().getIntExtra(EXTRA_HISTORY_SESSION_POSITION, 0);
            String puzzleType = getIntent().getStringExtra(EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME);
            fragment = SolveListFragment.newInstance(false, puzzleType, position);
            fm.beginTransaction().add(R.id.activity_history_solvelist_framelayout, fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
*/
