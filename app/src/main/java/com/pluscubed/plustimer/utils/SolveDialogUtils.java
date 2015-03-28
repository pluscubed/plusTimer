package com.pluscubed.plustimer.utils;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;

import com.pluscubed.plustimer.ui.SolveDialogFragment;

/**
 * Creating solve dialogs
 */
public class SolveDialogUtils {
    public static final String DIALOG_SOLVE_TAG = "SOLVE_DIALOG";

    public static void createSolveDialog(Activity activity, boolean addMode, String puzzleTypeName,
                                         int sessionIndex, int solveIndex) {
        FragmentManager fragmentManager = activity.getFragmentManager();
        DialogFragment dialog = (DialogFragment) fragmentManager.findFragmentByTag
                (DIALOG_SOLVE_TAG);
        if (ErrorUtils.isSolveNonexistent(activity, puzzleTypeName, sessionIndex, solveIndex)) {
            return;
        }
        if (dialog == null) {
            SolveDialogFragment d = SolveDialogFragment
                    .newInstance(addMode, puzzleTypeName,
                            sessionIndex, solveIndex);
            d.show(fragmentManager, DIALOG_SOLVE_TAG);
        }
    }
}
