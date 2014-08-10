package com.pluscubed.plustimer.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Solve;

/**
 * Solve modify dialog
 */
public class SolveDialog extends DialogFragment {
    public static final String ARG_DIALOG_INIT_PUZZLETYPE_DISPLAY_NAME = "com.pluscubed.plustimer.dialog.puzzleType";
    public static final String ARG_DIALOG_INIT_SESSION_INDEX = "com.pluscubed.plustimer.dialog.sessionIndex";
    public static final String ARG_DIALOG_INIT_SOLVE_INDEX = "com.pluscubed.plustimer.dialog.solveIndex";


    public static final int DIALOG_PENALTY_NONE = 0;
    public static final int DIALOG_PENALTY_PLUSTWO = 1;
    public static final int DIALOG_PENALTY_DNF = 2;
    public static final int DIALOG_RESULT_DELETE = 3;

    private String mPuzzleTypeDisplayName;
    private int mSolveIndex;
    private int mSessionIndex;
    private int mSelection;

    private OnDialogDismissedListener mListener;

    static SolveDialog newInstance(String displayName, int sessionIndex, int solveIndex) {
        SolveDialog d = new SolveDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_INIT_SESSION_INDEX, sessionIndex);
        args.putInt(ARG_DIALOG_INIT_SOLVE_INDEX, solveIndex);
        args.putString(ARG_DIALOG_INIT_PUZZLETYPE_DISPLAY_NAME, displayName);
        d.setArguments(args);
        return d;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnDialogDismissedListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement OnDialogDismissedListener");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        mListener.onDialogDismissed(mPuzzleTypeDisplayName, mSessionIndex, mSolveIndex, mSelection);

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mPuzzleTypeDisplayName = getArguments().getString(ARG_DIALOG_INIT_PUZZLETYPE_DISPLAY_NAME);
        mSessionIndex = getArguments().getInt(ARG_DIALOG_INIT_SESSION_INDEX);
        mSolveIndex = getArguments().getInt(ARG_DIALOG_INIT_SOLVE_INDEX);
        Solve solve = PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getActivity()).getSolveByPosition(mSolveIndex);

        String timeString = solve.getDescriptiveTimeString();
        String scramble = solve.getScrambleAndSvg().scramble;
        long timestamp = solve.getTimestamp();
        int penalty;
        switch (solve.getPenalty()) {
            case DNF:
                penalty = DIALOG_PENALTY_DNF;
                break;
            case PLUSTWO:
                penalty = DIALOG_PENALTY_PLUSTWO;
                break;
            case NONE:
            default:
                penalty = DIALOG_PENALTY_NONE;
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();

        @SuppressLint("InflateParams")
        View v = inflater.inflate(R.layout.dialog_solve, null);

        Spinner penaltySpinner = (Spinner) v.findViewById(R.id.dialog_solve_modify_penalty_spinner);
        final TextView scrambleTextView = (TextView) v.findViewById(R.id.dialog_solve_scramble_textview);
        TextView timestampTextView = (TextView) v.findViewById(R.id.dialog_solve_timestamp_textview);

        scrambleTextView.setText(scramble);

        timestampTextView.setText(Solve.timeDateStringFromTimestamp(getActivity().getApplicationContext(), timestamp));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.penalty_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        penaltySpinner.setAdapter(adapter);

        penaltySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int selectedPosition, long id) {
                mSelection = selectedPosition;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        penaltySpinner.setSelection(penalty);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(timeString)
                .setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSelection = DIALOG_RESULT_DELETE;
                    }
                });
        return builder.create();
    }


    public interface OnDialogDismissedListener {
        public void onDialogDismissed(String displayName, int sessionIndex, int solveIndex, int penalty);
    }
}
