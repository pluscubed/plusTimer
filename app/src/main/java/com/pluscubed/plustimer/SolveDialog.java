package com.pluscubed.plustimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Solve modify dialog
 */
public class SolveDialog extends DialogFragment {

    public static final String BUNDLEKEY_DIALOG_INIT_SOLVE_INDEX = "index";


    public static final int DIALOG_PENALTY_NONE = 0;
    public static final int DIALOG_PENALTY_PLUSTWO = 1;
    public static final int DIALOG_PENALTY_DNF = 2;
    public static final int DIALOG_RESULT_DELETE = 3;

    private int mPosition;
    private int mSelection;

    private SolveDialogListener mListener;

    static SolveDialog newInstance(int position) {
        SolveDialog d = new SolveDialog();
        Bundle args = new Bundle();
        args.putInt(BUNDLEKEY_DIALOG_INIT_SOLVE_INDEX, position);
        d.setArguments(args);
        return d;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SolveDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement SolveDialogListener");
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        mListener.onDialogDismissed(mPosition, mSelection);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mPosition = getArguments().getInt(BUNDLEKEY_DIALOG_INIT_SOLVE_INDEX);
        Solve solve = PuzzleType.sCurrentPuzzleType.getCurrentSession().getSolveByPosition(mPosition);
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


    public interface SolveDialogListener {
        public void onDialogDismissed(int position, int penalty);
    }
}
