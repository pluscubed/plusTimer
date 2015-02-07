package com.pluscubed.plustimer.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.ScrambleAndSvg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Error Utility Methods
 */
public class ErrorUtils {

    public static String getUiScramble(Context c, int position, ScrambleAndSvg scrambleAndSvg, boolean signEnabled,
                                       String puzzleTypeName) {
        String uiScramble = "";
        try {
            uiScramble = scrambleAndSvg.getUiScramble(signEnabled,
                    puzzleTypeName);
        } catch (NullPointerException e) {
            String positionString = String.valueOf(position);
            if (position == -1) {
                positionString = "CurrentScrambleAndSvg";
            }
            new MaterialDialog.Builder(c)
                    .content("Error: Solve #" + positionString + " UI scramble doesn't exist")
                    .positiveText("Dismiss")
                    .show();
            Crashlytics.logException(e);
        }
        return uiScramble;
    }

    public static boolean solveNonexistent(Context c, String puzzleTypeName, int solveIndex, int sessionIndex) {
        try {
            PuzzleType.valueOf(puzzleTypeName).getSession(sessionIndex).getSolveByPosition(solveIndex);
            return false;
        } catch (IndexOutOfBoundsException e) {
            new MaterialDialog.Builder(c)
                    .content("Error: Solve #" + solveIndex + " doesn't exist")
                    .positiveText("Dismiss")
                    .show();
            Crashlytics.log(Log.ERROR,
                    "Solve #" + solveIndex + " nonexistent",
                    PuzzleType.getCurrent()
                            .getSession(sessionIndex)
                            .toString(c, PuzzleType.getCurrent().name(), true, true, true, false)
            );
            Crashlytics.logException(e);
            return true;
        }
    }

    public static void sendHistoryDataEmail(Context context) {
        BufferedReader r = null;
        StringBuilder total = new StringBuilder();
        for (PuzzleType p : PuzzleType.values()) {
            try {
                total.append("\n\n\n").append(p.name()).append("\n");
                InputStream in = context.openFileInput(p.name() + ".json");
                r = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("mailto", "plusCubed@gmail.com", null));
        intent.putExtra(Intent.EXTRA_TEXT, total.toString());
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)));
    }
}
