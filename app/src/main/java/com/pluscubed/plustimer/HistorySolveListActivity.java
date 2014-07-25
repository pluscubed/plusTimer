package com.pluscubed.plustimer;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;

import java.io.IOException;

/**
 * History session activity
 */
public class HistorySolveListActivity extends ActionBarActivity implements CurrentSBaseFragment.OnSolveItemClickListener, SolveDialog.SolveDialogListener {
    public static final String EXTRA_HISTORY_SESSION_POSITION = "com.pluscubed.plustimer.history_session_position";
    public static final String HISTORY_DIALOG_FRAGMENT_TAG = "HISTORY_MODIFY_DIALOG";

    @Override
    public void showCurrentSolveDialog(String displayName, int sessionIndex, int solveIndex) {
        DialogFragment dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag(HISTORY_DIALOG_FRAGMENT_TAG);
        if (dialog == null) {
            SolveDialog d = SolveDialog.newInstance(PuzzleType.get(displayName).toString(), sessionIndex, solveIndex);
            d.show(getSupportFragmentManager(), HISTORY_DIALOG_FRAGMENT_TAG);
        }
    }

    @Override
    public void onDialogDismissed(String displayName, int sessionIndex, int solveIndex, int penalty) {
        Solve solve = PuzzleType.get(displayName).getSession(sessionIndex, this).getSolveByPosition(solveIndex);
        boolean finish = false;
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
                if (PuzzleType.get(displayName).getSession(sessionIndex, this).getNumberOfSolves() == 0) {
                    PuzzleType.get(displayName).deleteHistorySession(sessionIndex);
                    finish = true;
                }
                break;
        }
        try {
            PuzzleType.get(displayName).saveHistorySessionsToFile(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (finish) {
            finish();
            return;
        }
        if (getSupportFragmentManager().findFragmentById(R.id.activity_history_solve_list_framelayout) != null) {
            ((SolveListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_history_solve_list_framelayout)).onSessionSolvesChanged();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_session);
        FragmentManager fm = getSupportFragmentManager();
        int position = getIntent().getIntExtra(EXTRA_HISTORY_SESSION_POSITION, 0);
        Fragment fragment = SolveListFragment.newInstance(false, PuzzleType.CURRENT, position);
        fm.beginTransaction().add(R.id.activity_history_solve_list_framelayout, fragment).commit();
    }
}
