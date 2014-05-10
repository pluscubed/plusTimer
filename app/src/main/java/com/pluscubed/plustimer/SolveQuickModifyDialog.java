package com.pluscubed.plustimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
public class SolveQuickModifyDialog extends DialogFragment {

    public static final String BUNDLEKEY_DIALOG_INIT_TIMESTRING = "timestring";
    public static final String BUNDLEKEY_DIALOG_INIT_SOLVE_INDEX = "index";
    public static final String BUNDLEKEY_DIALOG_INIT_PENALTY = "penalty";
    public static final String BUNDLEKEY_DIALOG_INIT_SCRAMBLE = "scramble";
    public static final String BUNDLEKEY_DIALOG_INIT_TIMESTAMP = "timestamp";

    private int mPosition;
    private int mSelection;

    static SolveQuickModifyDialog newInstance(Solve i, int position, int penalty) {
        SolveQuickModifyDialog d = new SolveQuickModifyDialog();
        Bundle args = new Bundle();
        args.putString(BUNDLEKEY_DIALOG_INIT_TIMESTRING, i.getDescriptiveTimeString());
        args.putInt(BUNDLEKEY_DIALOG_INIT_SOLVE_INDEX, position);
        args.putInt(BUNDLEKEY_DIALOG_INIT_PENALTY, penalty);
        args.putString(BUNDLEKEY_DIALOG_INIT_SCRAMBLE, i.getScrambleAndSvg().scramble);
        args.putLong(BUNDLEKEY_DIALOG_INIT_TIMESTAMP, i.getTimestamp());
        d.setArguments(args);
        return d;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!(getTargetFragment() == null)) {
            Intent i = new Intent();
            i.putExtra(TimerFragment.EXTRA_DIALOG_FINISH_SOLVE_INDEX, mPosition);
            i.putExtra(TimerFragment.EXTRA_DIALOG_FINISH_SELECTION, mSelection);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mPosition = getArguments().getInt(BUNDLEKEY_DIALOG_INIT_SOLVE_INDEX);
        String timeString = getArguments().getString(BUNDLEKEY_DIALOG_INIT_TIMESTRING);
        String scramble = getArguments().getString(BUNDLEKEY_DIALOG_INIT_SCRAMBLE);
        long timestamp = getArguments().getLong(BUNDLEKEY_DIALOG_INIT_TIMESTAMP);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_solve, null);

        Spinner penaltySpinner = (Spinner) v.findViewById(R.id.dialog_modify_penalty_spinner);
        final TextView scrambleTextView = (TextView) v.findViewById(R.id.dialog_scramble);
        TextView timestampTextView = (TextView) v.findViewById(R.id.dialog_timestamp);

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
        penaltySpinner.setSelection(getArguments().getInt(BUNDLEKEY_DIALOG_INIT_PENALTY));

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
                        mSelection = TimerFragment.DIALOG_RESULT_DELETE;
                    }
                });
        return builder.create();
    }
}
