package com.pluscubed.plustimer.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.Util;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Solve;

/**
 * Solve modify dialog
 */
public class SolveDialogFragment extends DialogFragment {

    public static final String ARG_DIALOG_INIT_PUZZLETYPE_DISPLAY_NAME
            = "com.pluscubed.plustimer.dialog.puzzleType";

    public static final String ARG_DIALOG_INIT_SESSION_INDEX
            = "com.pluscubed.plustimer.dialog.sessionIndex";

    public static final String ARG_DIALOG_INIT_SOLVE_INDEX
            = "com.pluscubed.plustimer.dialog.solveIndex";

    public static final int DIALOG_PENALTY_NONE = 0;

    public static final int DIALOG_PENALTY_PLUSTWO = 1;

    public static final int DIALOG_PENALTY_DNF = 2;

    public static final int DIALOG_RESULT_DELETE = 3;

    private String mPuzzleTypeDisplayName;

    private int mSolveIndex;

    private int mSessionIndex;

    private int mSelection;

    private boolean mAddMode;

    private OnDialogDismissedListener mListener;

    static SolveDialogFragment newInstance(String displayName, int sessionIndex, int solveIndex) {
        SolveDialogFragment d = new SolveDialogFragment();
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
        //SOLVE SETUP
        mPuzzleTypeDisplayName = getArguments().getString(ARG_DIALOG_INIT_PUZZLETYPE_DISPLAY_NAME);
        mSessionIndex = getArguments().getInt(ARG_DIALOG_INIT_SESSION_INDEX);
        mSolveIndex = getArguments().getInt(ARG_DIALOG_INIT_SOLVE_INDEX);
        final Solve solve = PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex)
                .getSolveByPosition(mSolveIndex);

        final boolean millisecondsEnabled = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SettingsActivity.PREF_MILLISECONDS_CHECKBOX, true);
        String timeString = solve.getDescriptiveTimeString(millisecondsEnabled);
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

        //VIEW INFLATION
        @SuppressLint("InflateParams")
        View v = inflater.inflate(R.layout.dialog_solve, null);

        //TIMESTAMP TEXTVIEW SETUP
        TextView timestampTextView = (TextView) v
                .findViewById(R.id.dialog_solve_timestamp_textview);
        timestampTextView.setText(
                Util.timeDateStringFromTimestamp(getActivity().getApplicationContext(), timestamp));

        //PENALTY SPINNER SETUP
        Spinner penaltySpinner = (Spinner) v.findViewById(R.id.dialog_solve_modify_penalty_spinner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getActivity(), 0,
                getResources().getStringArray(R.array.penalty_array)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater()
                            .inflate(R.layout.spinner_item, parent, false);
                }
                TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
                textView.setText(getItem(position));
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                        R.drawable.spinner_triangle_black, 0);
                return convertView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v.findViewById(android.R.id.text1)).setTextColor(
                        getResources().getColorStateList(R.color.list_dropdown_color_light));
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        penaltySpinner.setAdapter(adapter);
        penaltySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int selectedPosition,
                                       long id) {
                mSelection = selectedPosition;
                switch (mSelection) {
                    case SolveDialogFragment.DIALOG_PENALTY_NONE:
                        solve.setPenalty(Solve.Penalty.NONE);
                        break;
                    case SolveDialogFragment.DIALOG_PENALTY_PLUSTWO:
                        solve.setPenalty(Solve.Penalty.PLUSTWO);
                        break;
                    case SolveDialogFragment.DIALOG_PENALTY_DNF:
                        solve.setPenalty(Solve.Penalty.DNF);
                        break;
                }
                getDialog().setTitle(solve.getDescriptiveTimeString(millisecondsEnabled));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        penaltySpinner.setSelection(penalty);

        //TIME EDITTEXT SETUP
        EditText timeEdit = (EditText) v.findViewById(R.id.dialog_solve_time_edittext);
        timeEdit.setText(timeString);
        timeEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Toast.makeText(getActivity(), getString(R.string.dialog_wip), Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //SCRAMBLE EDITTEXT SETUP
        EditText scrambleEdit = (EditText) v.findViewById(R.id.dialog_solve_scramble_edittext);
        scrambleEdit.setText(scramble);
        scrambleEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Toast.makeText(getActivity(), getString(R.string.dialog_wip), Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        //Return
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v)
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

        public void onDialogDismissed(String displayName, int sessionIndex, int solveIndex,
                                      int penalty);
    }
}
