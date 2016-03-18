package com.pluscubed.plustimer.ui.solvedialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.couchbase.lite.CouchbaseLiteException;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.RecyclerViewUpdate;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.ThemeUtils;
import com.pluscubed.plustimer.utils.Utils;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

import java.io.IOException;
import java.math.BigDecimal;

import rx.android.schedulers.AndroidSchedulers;

/**
 * Solve modify dialog
 */
public class SolveDialogFragment extends DialogFragment {

    private static final String ARG_DIALOG_INIT_PUZZLETYPE_ID = "com.pluscubed.plustimer.dialog.puzzleType";
    private static final String ARG_DIALOG_INIT_SESSION_ID = "com.pluscubed.plustimer.dialog.sessionIndex";
    private static final String ARG_DIALOG_INIT_SOLVE = "com.pluscubed.plustimer.dialog.solveIndex";
    private static final String ARG_DIALOG_INIT_ADD_MODE = "com.pluscubed.plustimer.dialog.addMode";

    private String mPuzzleTypeId;
    private String mSessionId;

    private boolean mAddMode;

    private EditText mScrambleEdit;
    private Solve mSolveCopy;
    private boolean mMillisecondsEnabled;
    private EditText mTimeEdit;
    private Solve mSolve;

    public static SolveDialogFragment newInstance(boolean addMode, String puzzleTypeId,
                                                  String sessionId, Solve solve) {
        SolveDialogFragment d = new SolveDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DIALOG_INIT_SESSION_ID, sessionId);
        args.putParcelable(ARG_DIALOG_INIT_SOLVE, solve);
        args.putString(ARG_DIALOG_INIT_PUZZLETYPE_ID, puzzleTypeId);
        args.putBoolean(ARG_DIALOG_INIT_ADD_MODE, addMode);
        d.setArguments(args);
        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //SOLVE SETUP
        mAddMode = getArguments().getBoolean(ARG_DIALOG_INIT_ADD_MODE);
        mPuzzleTypeId = getArguments().getString(ARG_DIALOG_INIT_PUZZLETYPE_ID);
        mSessionId = getArguments().getString(ARG_DIALOG_INIT_SESSION_ID);

        if (!mAddMode) {
            //Original solve contains the ID - new info will be copied back from mSolveCopy
            mSolve = getArguments().getParcelable(ARG_DIALOG_INIT_SOLVE);
            mSolveCopy = new Solve(mSolve);
        } else {
            mSolveCopy = new Solve();
        }

        mMillisecondsEnabled = PrefUtils.isDisplayMillisecondsEnabled(getActivity());
        boolean signEnabled = PrefUtils.isSignEnabled(getActivity());

        String timeString = "0";
        String scramble = "";
        long timestamp = mSolveCopy.getTimestamp();
        int penalty = Solve.PENALTY_NONE;

        if (!mAddMode) {
            timeString = mSolveCopy.getDescriptiveTimeString(mMillisecondsEnabled);
            scramble = Utils.getUiScramble(mSolveCopy.getScramble(), signEnabled, mPuzzleTypeId);
            penalty = mSolveCopy.getPenalty();
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //VIEW INFLATION
        @SuppressLint("InflateParams")
        View v = inflater.inflate(R.layout.fragment_dialog_solve, null);

        //TIMESTAMP TEXTVIEW SETUP
        TextView timestampTextView = (TextView) v.findViewById(R.id.dialog_solve_timestamp_textview);
        timestampTextView.setText(Utils.dateTimeSecondsStringFromTimestamp(getActivity().getApplicationContext(), timestamp));

        //PENALTY SPINNER SETUP
        Spinner penaltySpinner = (Spinner) v.findViewById(R.id.dialog_solve_modify_penalty_spinner);
        ArrayAdapter<CharSequence> adapter =
                new ArrayAdapter<CharSequence>(getActivity(), 0, getResources().getStringArray(R.array.penalty_array)) {
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
                        if (PrefUtils.getTheme(getActivity()) != PrefUtils.Theme.DARK
                                && PrefUtils.getTheme(getActivity()) != PrefUtils.Theme.BLACK) {
                            ImageView triangle = (ImageView) convertView.findViewById(R.id
                                    .spinner_item_imageview);
                            triangle.setColorFilter(Color.BLACK);
                        }
                        return convertView;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView,
                                                ViewGroup parent) {
                        View v = super.getDropDownView(position, convertView, parent);
                        int textColor;
                        if (PrefUtils.getTheme(getActivity()) == PrefUtils.Theme.DARK
                                || PrefUtils.getTheme(getActivity()) == PrefUtils.Theme.BLACK) {
                            textColor = R.color.list_dropdown_color_dark;
                        } else {
                            textColor = R.color.list_dropdown_color_light;
                        }
                        ((TextView) v.findViewById(android.R.id.text1)).setTextColor
                                (getResources().getColorStateList(textColor));
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
                try {
                    mSolveCopy.setPenalty(null, selectedPosition);
                } catch (CouchbaseLiteException | IOException e) {
                    e.printStackTrace();
                }
                updateTitle();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //TIME EDITTEXT SETUP
        mTimeEdit = (EditText) v.findViewById(R.id.dialog_solve_time_edittext);
        if (!mAddMode) {
            mTimeEdit.setText(Utils.timeStringSecondsFromNs(mSolveCopy.getRawTime(),
                    mMillisecondsEnabled));
        }
        mTimeEdit.addTextChangedListener(new TextWatcher() {
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
                            BigDecimal timeEditTextDecimal = BigDecimal.valueOf(Double.parseDouble(s.toString()));
                            mSolveCopy.setRawTime(null,
                                    timeEditTextDecimal.multiply(BigDecimal.valueOf(1000000000)).longValueExact());
                            updateTitle();
                            break;
                        } else {
                            getDialog().setTitle(Utils.timeStringFromNs(0, mMillisecondsEnabled));
                            mSolveCopy.setRawTime(null, 0);
                            break;
                        }
                    } catch (NumberFormatException | ArithmeticException e) {
                        s.delete(s.length() - 1, s.length());
                    } catch (CouchbaseLiteException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //SCRAMBLE EDITTEXT SETUP
        mScrambleEdit = (EditText) v.findViewById(R.id.dialog_solve_scramble_edittext);
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
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.customView(v, true)
                .title(timeString)
                .theme(ThemeUtils.getDialogTheme(getActivity()))
                .autoDismiss(false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive((dialog, which) -> onOkPressed())
                .onNegative((dialog, which) -> onCancelPressed());
        if (!mAddMode) {
            builder.neutralText(R.string.delete)
                    .onNeutral((dialog, which) -> {
                        onDeletePressed();
                        dialog.dismiss();
                    });
        }

        return builder.build();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        if (mAddMode) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_STATE_VISIBLE);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void onDeletePressed() {
        //TODO
        /*if (ErrorUtils.isSolveNonexistent(getActivity(), mPuzzleTypeId, mSessionId, mSolveId)) {
            return;
        }*/
        PuzzleType.get(mPuzzleTypeId).getSessionDeferred(getActivity(), mSessionId)
                .flatMapObservable(session -> session.deleteSolveAsync(getActivity(), mSolve.getId()).toObservable())
                .toCompletable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::dismiss);
    }

    private void onCancelPressed() {
        dismiss();
    }

    private void onOkPressed() {
        try {
            if (mTimeEdit.getText().toString().equals("")) {
                mTimeEdit.setText("0");
            }
            String scrambleText = Utils.signToWcaNotation
                    (mScrambleEdit.getText().toString(),
                            mPuzzleTypeId);
            if (!scrambleText.equals(mSolveCopy.getScramble())) {
                PuzzleType.get(mPuzzleTypeId).getPuzzle()
                        .getSolvedState().applyAlgorithm(scrambleText);
            }
            mSolveCopy.setScramble(null, scrambleText);
            if (!mAddMode) {
                mSolve.copy(mSolveCopy);
                mSolve.updateCb(getActivity());
                Session.notifyListeners(mSessionId, mSolve, RecyclerViewUpdate.SINGLE_CHANGE);
                dismiss();
            } else {
                PuzzleType.get(mPuzzleTypeId).getSession(getActivity(), mSessionId)
                        .addDisconnectedSolve(getActivity(), mSolveCopy);
                dismiss();
            }

        } catch (InvalidScrambleException e) {
            mScrambleEdit.setError(getString(R.string.invalid_scramble));
        } catch (CouchbaseLiteException | IOException e) {
            e.printStackTrace();
        }
    }

    void updateTitle() {
        if (getDialog() != null) {
            getDialog().setTitle(mSolveCopy.getDescriptiveTimeString(mMillisecondsEnabled));
        }
    }

}
