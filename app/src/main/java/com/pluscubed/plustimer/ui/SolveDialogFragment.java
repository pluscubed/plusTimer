package com.pluscubed.plustimer.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.Util;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Solve;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

import java.math.BigDecimal;

/**
 * Solve modify dialog
 */
public class SolveDialogFragment extends DialogFragment {

    public static final String ARG_DIALOG_INIT_PUZZLETYPE_DISPLAY_NAME = "com" +
            ".pluscubed.plustimer.dialog.puzzleType";

    public static final String ARG_DIALOG_INIT_SESSION_INDEX = "com.pluscubed" +
            ".plustimer.dialog.sessionIndex";

    public static final String ARG_DIALOG_INIT_SOLVE_INDEX = "com.pluscubed" +
            ".plustimer.dialog.solveIndex";

    public static final int DIALOG_PENALTY_NONE = 0;
    public static final int DIALOG_PENALTY_PLUSTWO = 1;
    public static final int DIALOG_PENALTY_DNF = 2;

    private String mPuzzleTypeName;
    private int mSessionIndex;
    private int mSolveIndex;

    private boolean mAddMode;

    private OnDialogDismissedListener mListener;
    private EditText mScrambleEdit;
    private Solve mSolve;
    private boolean mMillisecondsEnabled;

    static SolveDialogFragment newInstance(String puzzleTypeName,
                                           int sessionIndex, int solveIndex) {
        SolveDialogFragment d = new SolveDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_INIT_SESSION_INDEX, sessionIndex);
        args.putInt(ARG_DIALOG_INIT_SOLVE_INDEX, solveIndex);
        args.putString(ARG_DIALOG_INIT_PUZZLETYPE_DISPLAY_NAME, puzzleTypeName);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PuzzleType.initialize(getActivity());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //SOLVE SETUP
        mPuzzleTypeName = getArguments().getString
                (ARG_DIALOG_INIT_PUZZLETYPE_DISPLAY_NAME);
        mSessionIndex = getArguments().getInt(ARG_DIALOG_INIT_SESSION_INDEX);
        mSolveIndex = getArguments().getInt(ARG_DIALOG_INIT_SOLVE_INDEX);
        mSolve = PuzzleType.valueOf(mPuzzleTypeName).getSession
                (mSessionIndex).getSolveByPosition(mSolveIndex);

        SharedPreferences defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mMillisecondsEnabled = defaultSharedPreferences.getBoolean
                (SettingsActivity.PREF_MILLISECONDS_CHECKBOX, true);
        boolean signEnabled = defaultSharedPreferences.getBoolean
                (SettingsActivity.PREF_SIGN_CHECKBOX, true);
        String timeString = mSolve.getDescriptiveTimeString
                (mMillisecondsEnabled);
        String scramble = mSolve.getScrambleAndSvg().getUiScramble
                (signEnabled, mPuzzleTypeName);
        long timestamp = mSolve.getTimestamp();
        int penalty;
        switch (mSolve.getPenalty()) {
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
        View v = inflater.inflate(R.layout.fragment_dialog_solve, null);

        //TIMESTAMP TEXTVIEW SETUP
        TextView timestampTextView = (TextView) v.findViewById(R.id
                .dialog_solve_timestamp_textview);
        timestampTextView.setText(Util.timeDateStringFromTimestamp
                (getActivity().getApplicationContext(), timestamp));

        //PENALTY SPINNER SETUP
        Spinner penaltySpinner = (Spinner) v.findViewById(R.id
                .dialog_solve_modify_penalty_spinner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>
                (getActivity(), 0, getResources().getStringArray(R.array
                        .penalty_array)) {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater()
                            .inflate(R.layout.spinner_item, parent, false);
                }
                TextView textView = (TextView) convertView.findViewById
                        (android.R.id.text1);
                textView.setText(getItem(position));
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                        R.drawable.spinner_triangle_black, 0);
                return convertView;
            }

            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                ((TextView) v.findViewById(android.R.id.text1)).setTextColor
                        (getResources().getColorStateList(R.color
                                .list_dropdown_color_light));
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        penaltySpinner.setAdapter(adapter);
        penaltySpinner.setOnItemSelectedListener(new AdapterView
                .OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int selectedPosition,
                                       long id) {
                switch (selectedPosition) {
                    case SolveDialogFragment.DIALOG_PENALTY_NONE:
                        mSolve.setPenalty(Solve.Penalty.NONE);
                        break;
                    case SolveDialogFragment.DIALOG_PENALTY_PLUSTWO:
                        mSolve.setPenalty(Solve.Penalty.PLUSTWO);
                        break;
                    case SolveDialogFragment.DIALOG_PENALTY_DNF:
                        mSolve.setPenalty(Solve.Penalty.DNF);
                        break;
                }
                updateTitle();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        penaltySpinner.setSelection(penalty);

        //TIME EDITTEXT SETUP
        EditText timeEdit = (EditText) v.findViewById(R.id
                .dialog_solve_time_edittext);
        timeEdit.setText(Util.timeStringSecondsFromNs(mSolve.getRawTime(),
                mMillisecondsEnabled));
        timeEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                while (true) {
                    try {
                        if (s.length() != 0) {
                            BigDecimal timeEditTextDecimal = BigDecimal
                                    .valueOf(Double.parseDouble(s.toString()));
                            mSolve.setRawTime(((timeEditTextDecimal.multiply
                                    (BigDecimal.valueOf(1000000000)))
                                    .longValueExact()));
                            updateTitle();
                            break;
                        } else {
                            getDialog().setTitle(Util.timeStringFromNs(0,
                                    mMillisecondsEnabled));
                            mSolve.setRawTime(0);
                            break;
                        }
                    } catch (NumberFormatException | ArithmeticException e) {
                        s.delete(s.length() - 1, s.length());
                    }
                }
            }
        });

        //SCRAMBLE EDITTEXT SETUP
        mScrambleEdit = (EditText) v.findViewById(R.id
                .dialog_solve_scramble_edittext);
        mScrambleEdit.setText(scramble);

        //Return
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(v)
                .setTitle(timeString)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }
                        })
                .setNegativeButton(R.string.delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                PuzzleType.valueOf(mPuzzleTypeName).getSession
                                        (mSessionIndex).deleteSolve
                                        (mSolveIndex);
                                mListener.onDialogDismissed();
                            }
                        });
        return builder.create();
    }

    public void updateTitle() {
        getDialog().setTitle(mSolve.getDescriptiveTimeString
                (mMillisecondsEnabled));
    }

    @Override
    public void onStart() {
        super.onStart();

        //Use workaround to prevent dialog closing without check:
        //http://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog
        // -from-closing-when-a-button-is-clicked/15619098#15619098
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String scrambleText = Util.signToWcaNotation
                                (mScrambleEdit.getText().toString(),
                                        mPuzzleTypeName);
                        if (!scrambleText.equals(mSolve.getScrambleAndSvg()
                                .getScramble())) {
                            PuzzleType.valueOf(mPuzzleTypeName).getPuzzle()
                                    .getSolvedState().applyAlgorithm
                                    (scrambleText);
                        }
                        mSolve.getScrambleAndSvg().setScramble(scrambleText,
                                mPuzzleTypeName);
                        mListener.onDialogDismissed();
                        dismiss();
                    } catch (InvalidScrambleException e) {
                        Toast.makeText(getActivity(),
                                getString(R.string.invalid_scramble),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public interface OnDialogDismissedListener {
        public void onDialogDismissed();
    }
}
