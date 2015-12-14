package com.pluscubed.plustimer.utils;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;

import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.SolveDialogFragment;

/**
 * Creating solve dialogs
 */
public class SolveDialogUtils {
    public static final String DIALOG_SOLVE_TAG = "SOLVE_DIALOG";

    public static void createSolveDialog(Activity activity, boolean addMode, String puzzleTypeName,
                                         String sessionId, Solve solve) {
        FragmentManager fragmentManager = activity.getFragmentManager();
        DialogFragment dialog = (DialogFragment) fragmentManager.findFragmentByTag(DIALOG_SOLVE_TAG);
        //TODO
        /*if (ErrorUtils.isSolveNonexistent(activity, puzzleTypeName, sessionId, solveId)) {
            return;
        }*/
        if (dialog == null) {
            SolveDialogFragment d = SolveDialogFragment
                    .newInstance(addMode, puzzleTypeName, sessionId, solve);
            d.show(fragmentManager, DIALOG_SOLVE_TAG);
        }
    }
}
