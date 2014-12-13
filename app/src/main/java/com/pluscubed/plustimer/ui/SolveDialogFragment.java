package com.pluscubed.plustimer.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.Util;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Solve;
import com.rengwuxian.materialedittext.MaterialEditText;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

import java.math.BigDecimal;

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

    private String mPuzzleTypeName;
    private int mSessionIndex;
    private int mSolveIndex;

    private boolean mAddMode;

    private OnDialogDismissedListener mListener;
    private MaterialEditText mScrambleEdit;
    private Solve mSolve;
    private Solve mSolveCopy;
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
        mSolveCopy = new Solve(mSolve);

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
                ImageView triangle = (ImageView) convertView.findViewById(R.id.spinner_item_imageview);
                triangle.setColorFilter(Color.BLACK);
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
        penaltySpinner.setSelection(penalty);
        penaltySpinner.setOnItemSelectedListener(new AdapterView
                .OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int selectedPosition,
                                       long id) {
                switch (selectedPosition) {
                    case SolveDialogFragment.DIALOG_PENALTY_NONE:
                        mSolveCopy.setPenalty(Solve.Penalty.NONE);
                        break;
                    case SolveDialogFragment.DIALOG_PENALTY_PLUSTWO:
                        mSolveCopy.setPenalty(Solve.Penalty.PLUSTWO);
                        break;
                    case SolveDialogFragment.DIALOG_PENALTY_DNF:
                        mSolveCopy.setPenalty(Solve.Penalty.DNF);
                        break;
                }
                updateTitle();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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
                            mSolveCopy.setRawTime(((timeEditTextDecimal.multiply
                                    (BigDecimal.valueOf(1000000000)))
                                    .longValueExact()));
                            updateTitle();
                            break;
                        } else {
                            getDialog().setTitle(Util.timeStringFromNs(0,
                                    mMillisecondsEnabled));
                            mSolveCopy.setRawTime(0);
                            break;
                        }
                    } catch (NumberFormatException | ArithmeticException e) {
                        s.delete(s.length() - 1, s.length());
                    }
                }
            }
        });

        //SCRAMBLE EDITTEXT SETUP
        mScrambleEdit = (MaterialEditText) v.findViewById(R.id
                .dialog_solve_scramble_edittext);
        mScrambleEdit.setText(scramble);
        mScrambleEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mScrambleEdit.setError(null);
            }
        });

        //Return
        MaterialDialog.Builder builder = new MaterialDialog.Builder
                (getActivity());
        builder.customView(v)
                .title(timeString)
                .autoDismiss(false)
                .positiveText(android.R.string.ok)
                .neutralText(R.string.delete)
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.FullCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        try {
                            String scrambleText = Util.signToWcaNotation
                                    (mScrambleEdit.getText().toString(),
                                            mPuzzleTypeName);
                            if (!scrambleText.equals(mSolveCopy.getScrambleAndSvg()
                                    .getScramble())) {
                                PuzzleType.valueOf(mPuzzleTypeName).getPuzzle()
                                        .getSolvedState().applyAlgorithm
                                        (scrambleText);
                            }
                            mSolveCopy.getScrambleAndSvg().setScramble(scrambleText,
                                    mPuzzleTypeName);
                            if (mListener != null)
                                mListener.onDialogDismissed();

                            mSolve.copy(mSolveCopy);
                            dismiss();
                        } catch (InvalidScrambleException e) {
                            mScrambleEdit.setError("Invalid scramble.");
                        }
                    }

                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        if (mListener != null) mListener.onDialogDismissed();
                        dismiss();
                    }

                    @Override
                    public void onNeutral(MaterialDialog materialDialog) {
                        PuzzleType.valueOf(mPuzzleTypeName).getSession
                                (mSessionIndex).deleteSolve
                                (mSolveIndex);
                        if (mListener != null) mListener.onDialogDismissed();
                        dismiss();
                    }
                });
        return builder.build();
    }

    public void addListener(OnDialogDismissedListener listener) {
        mListener = listener;
    }

    public void updateTitle() {
        getDialog().setTitle(mSolveCopy.getDescriptiveTimeString
                (mMillisecondsEnabled));
    }

    public interface OnDialogDismissedListener {
        public void onDialogDismissed();
    }
}
