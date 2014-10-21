package com.pluscubed.plustimer.ui;

import android.animation.Animator;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.Util;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Solve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import it.sephiroth.android.library.widget.HListView;

/**
 * TimerFragment
 */

public class CurrentSessionTimerFragment extends Fragment {

    public static final String TAG = "CURRENT_SESSION_TIMER_FRAGMENT";
    public static final long HOLD_TIME = 550000000L;
    public static final int REFRESH_RATE = 15;
    private static final String CURRENT_SESSION_TIMER_RETAINED_TAG
            = "CURRENT_SESSION_TIMER_RETAINED";
    private static final String STATE_IMAGE_DISPLAYED = "scramble_image_displayed_boolean";
    private static final String STATE_START_TIME = "start_time_long";
    private static final String STATE_RUNNING = "running_boolean";
    private static final String STATE_INSPECTING = "inspecting_boolean";
    private static final String STATE_INSPECTION_START_TIME = "inspection_start_time_long";

    private boolean mHoldToStartEnabled;
    private boolean mInspectionEnabled;
    private boolean mTwoRowTimeEnabled;
    private int mUpdateTimePref;
    private boolean mMillisecondsEnabled;
    private int mPrefSize;
    private boolean mKeepScreenOn;
    private boolean mSignEnabled;

    private CurrentSessionTimerRetainedFragment mRetainedFragment;

    private TextView mTimerText;
    private TextView mTimerText2;
    private TextView mScrambleText;
    private HListView mHListView;
    private ImageView mScrambleImage;
    private TextView mStatsSolvesText;
    private TextView mStatsText;
    private LinearLayout mPenaltyBarLinearLayout;
    private Button mPenaltyDnfButton;
    private Button mPenaltyPlusTwoButton;

    private Handler mUiHandler;

    private boolean mHoldTiming;
    private long mHoldTimerStartTimestamp;
    private final Runnable mHoldTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (550000000L <= System.nanoTime() - mHoldTimerStartTimestamp) {
                setTextColor(Color.GREEN);
            } else {
                mUiHandler.postDelayed(this, REFRESH_RATE);
            }
            setTimerTextToPrefSize();
        }
    };

    private boolean mInspecting;
    private long mInspectionStartTimestamp;
    private long mTimingStartTimestamp;
    private final Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mUpdateTimePref != 2) {
                setTimerText(Util.timeStringsFromNsSplitByDecimal(System.nanoTime() -
                                mTimingStartTimestamp,
                        mMillisecondsEnabled));
                setTimerTextToPrefSize();
                mUiHandler.postDelayed(this, REFRESH_RATE);
            } else {
                setTimerText(new String[]{getString(R.string.timing), ""});
                mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100);
            }
        }
    };

    private boolean mFromSavedInstanceState;

    private boolean mTiming;

    private boolean mScrambleImageDisplay;

    private boolean mLateStartPenalty;
    private final Runnable mInspectionRunnable = new Runnable() {
        @Override
        public void run() {
            String[] array = Util.timeStringsFromNsSplitByDecimal(16000000000L - (System.nanoTime() -
                    mInspectionStartTimestamp), mMillisecondsEnabled);
            array[1] = "";

            if (15000000000L - (System.nanoTime() - mInspectionStartTimestamp) > 0) {
                //If inspection proceeding normally
                setTimerText(array);
            } else {
                if (17000000000L - (System.nanoTime() - mInspectionStartTimestamp) > 0) {
                    //If late start
                    mLateStartPenalty = true;
                    setTimerText(new String[]{"+2", ""});
                } else {
                    //If past 17 seconds which means DNF
                    stopHoldTimer();
                    stopInspection();

                    Solve s = new Solve(mRetainedFragment.getCurrentScrambleAndSvg(), 0);
                    s.setPenalty(Solve.Penalty.DNF);

                    //Add the solve to the current session with the current scramble/scramble
                    // image and DNF
                    PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                            .addSolve(s);

                    resetTimer();
                    setTimerTextToLastSolveTime();

                    onSessionSolvesChanged();

                    if (mRetainedFragment.isScrambling()) {
                        mScrambleText.setText(R.string.scrambling);
                    }
                    mRetainedFragment.postSetScrambleViewsToCurrent();
                    return;
                }
            }

            //If proceeding normally or +2
            setTimerTextToPrefSize();
            mUiHandler.postDelayed(this, REFRESH_RATE);
        }
    };

    //Generate string with specified current averages and mean of current session
    private String buildStatsWithAveragesOf(Context context, Integer... currentAverages) {
        Arrays.sort(currentAverages, Collections.reverseOrder());
        String s = "";
        for (int i : currentAverages) {
            if (PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                    .getNumberOfSolves() >= i) {
                s += String.format(context.getString(R.string.cao), i) + ": " + PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                        .getStringCurrentAverageOf(i, mMillisecondsEnabled) + "\n";
            }
        }
        if (PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                .getNumberOfSolves() > 0) {
            s += context.getString(R.string.mean) + PuzzleType.getCurrent()
                    .getSession(PuzzleType.CURRENT_SESSION).getStringMean(mMillisecondsEnabled);
        }
        return s;
    }

    /**
     * Set timer textviews using an array. Hides/shows lower textview depending on preferences
     * and whether the second array item is blank.
     *
     * @param array An array of 2 strings
     */
    public void setTimerText(String[] array) {
        if (mTwoRowTimeEnabled) {
            mTimerText.setText(array[0]);
            mTimerText2.setText(array[1]);
            if (array[1].equals("") || (mTiming && mUpdateTimePref != 0)) {
                mTimerText2.setVisibility(View.GONE);
            } else {
                mTimerText2.setVisibility(View.VISIBLE);
            }
        } else {
            mTimerText2.setVisibility(View.GONE);
            mTimerText.setText(array[0]);
            if (!array[1].equals("") && !(mTiming && (mUpdateTimePref != 0))) {
                mTimerText.append("." + array[1]);
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IMAGE_DISPLAYED, mScrambleImageDisplay);
        outState.putLong(STATE_START_TIME, mTimingStartTimestamp);
        outState.putBoolean(STATE_RUNNING, mTiming);
        outState.putBoolean(STATE_INSPECTING, mInspecting);
        outState.putLong(STATE_INSPECTION_START_TIME, mInspectionStartTimestamp);
    }

    //Set scramble text and scramble image to current ones
    public void setScrambleTextAndImageToCurrent() {
        if (mRetainedFragment.getCurrentScrambleAndSvg() != null) {
            SVG svg = null;
            try {
                svg = SVG.getFromString(mRetainedFragment.getCurrentScrambleAndSvg().getSvg());
            } catch (SVGParseException e) {
                e.printStackTrace();
            }
            Drawable drawable = new PictureDrawable(svg.renderToPicture());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mScrambleImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            mScrambleImage.setImageDrawable(drawable);

            mScrambleText.setText(mRetainedFragment.getCurrentScrambleAndSvg().getUiScramble(mSignEnabled, PuzzleType.getCurrent().name()));
        } else {
            mRetainedFragment.generateNextScramble();
            mRetainedFragment.postSetScrambleViewsToCurrent();
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        PuzzleType.initialize(getActivity());

        mRetainedFragment = (CurrentSessionTimerRetainedFragment) getFragmentManager().findFragmentByTag(CURRENT_SESSION_TIMER_RETAINED_TAG);
        // If the Fragment is null, create and add it
        if (mRetainedFragment == null) {
            mRetainedFragment = new CurrentSessionTimerRetainedFragment();
            getFragmentManager().beginTransaction().add(mRetainedFragment, CURRENT_SESSION_TIMER_RETAINED_TAG).commit();
        }
        mRetainedFragment.setTargetFragment(this, 0);

        //Set up UIHandler
        mUiHandler = new Handler(Looper.getMainLooper());

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        initSharedPrefs();

        if (savedInstanceState != null) {
            mScrambleImageDisplay = savedInstanceState.getBoolean(STATE_IMAGE_DISPLAYED);
            mTimingStartTimestamp = savedInstanceState.getLong(STATE_START_TIME);
            mTiming = savedInstanceState.getBoolean(STATE_RUNNING);
            mInspecting = savedInstanceState.getBoolean(STATE_INSPECTING);
            mInspectionStartTimestamp = savedInstanceState.getLong(STATE_INSPECTION_START_TIME);
            mFromSavedInstanceState = true;
        } else {
            mFromSavedInstanceState = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //When destroyed, stop timer runnable
        mUiHandler.removeCallbacksAndMessages(null);
        mRetainedFragment.setTargetFragment(null, 0);
    }

    public void onSessionSolvesChanged() {
        //Update stats
        mStatsSolvesText.setText(getString(R.string.solves) + PuzzleType.getCurrent()
                .getSession(PuzzleType.CURRENT_SESSION).getNumberOfSolves());
        mStatsText.setText(buildStatsWithAveragesOf(getActivity(), 5, 12, 100));
        //Update HListView
        ((SolveHListViewAdapter) mHListView.getAdapter()).updateSolvesList();
        if (!mTiming && !mInspecting) setTimerTextToLastSolveTime();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Toggle image button
            case R.id.menu_activity_current_session_scramble_image_menuitem:
                toggleScrambleImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initSharedPrefs();
        if (mKeepScreenOn) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        onSessionSolvesChanged();
    }

    public void initSharedPrefs() {
        SharedPreferences defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mInspectionEnabled = defaultSharedPreferences
                .getBoolean(SettingsActivity.PREF_INSPECTION_CHECKBOX, true);
        mHoldToStartEnabled = defaultSharedPreferences
                .getBoolean(SettingsActivity.PREF_HOLDTOSTART_CHECKBOX, true);
        mTwoRowTimeEnabled = getResources().getConfiguration().orientation == 1
                && defaultSharedPreferences
                .getBoolean(SettingsActivity.PREF_TWO_ROW_TIME_CHECKBOX, true);
        mUpdateTimePref = Integer.parseInt(
                defaultSharedPreferences.getString(SettingsActivity.PREF_UPDATE_TIME_LIST, "0"));
        mMillisecondsEnabled = defaultSharedPreferences
                .getBoolean(SettingsActivity.PREF_MILLISECONDS_CHECKBOX, true);
        mPrefSize = Integer.parseInt(defaultSharedPreferences
                .getString(SettingsActivity.PREF_TIME_TEXT_SIZE_EDITTEXT, "100"));
        mKeepScreenOn = defaultSharedPreferences.getBoolean(SettingsActivity
                .PREF_KEEPSCREENON_CHECKBOX, true);
        mSignEnabled = defaultSharedPreferences.getBoolean(SettingsActivity.PREF_SIGN_CHECKBOX, true);
    }

    public void setTimerTextToPrefSize() {
        if (mTimerText.getText() != getString(R.string.ready)) {
            if (mTimerText != null && mTimerText2 != null) {
                if (mTwoRowTimeEnabled) {
                    mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mPrefSize);
                } else {
                    mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, mPrefSize * 0.7F);
                }
                mTimerText2.setTextSize(TypedValue.COMPLEX_UNIT_SP, mPrefSize / 2);
            }
        }
    }

    //Called when the session is changed to another one (action bar spinner)
    public void onSessionChanged() {
        //Update quick stats and hlistview
        onSessionSolvesChanged();

        //Set timer text to ready, scramble text to scrambling
        mScrambleText.setText(R.string.scrambling);

        //Update options menu (disable)
        enableMenuItems(false);
        showScrambleImage(false);


        resetGenerateScramble();

        resetTimer();
    }

    public void showScrambleImage(boolean enable) {
        if (enable) {
            mScrambleImage.setVisibility(View.VISIBLE);
        } else {
            mScrambleImage.setVisibility(View.GONE);
        }
        mScrambleImageDisplay = enable;
    }

    public void resetGenerateScramble() {
        mRetainedFragment.resetScramblerThread();
        mRetainedFragment.generateNextScramble();
        mRetainedFragment.postSetScrambleViewsToCurrent();
    }

    public void resetTimer() {
        mUiHandler.removeCallbacksAndMessages(null);
        mHoldTiming = false;
        mTiming = false;
        mLateStartPenalty = false;
        mHoldTimerStartTimestamp = 0;
        mInspectionStartTimestamp = 0;
        mTimingStartTimestamp = 0;
        mInspecting = false;
        setTextColor(Color.BLACK);
    }

    public void setTextColor(int color) {
        mTimerText.setTextColor(color);
        mTimerText2.setTextColor(color);
    }

    public void enableMenuItems(boolean enable) {
        MenuItemsEnableCallback callback;
        try {
            callback = (MenuItemsEnableCallback) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    getActivity().toString() + " must implement MenuItemsCallback");
        }
        callback.enableMenuItems(enable);
    }

    public void toggleScrambleImage() {
        if (mScrambleImageDisplay) {
            mScrambleImageDisplay = false;
            mScrambleImage.setVisibility(View.GONE);
        } else {
            if (!mRetainedFragment.isScrambling()) {
                mScrambleImageDisplay = true;
                mScrambleImage.setVisibility(View.VISIBLE);
                mScrambleImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mScrambleImageDisplay = false;
                        mScrambleImage.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PuzzleType.getCurrent().saveCurrentSession(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_session_timer, container, false);

        mTimerText = (TextView) v.findViewById(R.id.fragment_current_session_timer_time_textview);
        mTimerText2 = (TextView) v.findViewById(R.id.fragment_current_session_timer_timeSecondary_textview);
        mScrambleText = (TextView) v.findViewById(R.id.fragment_current_session_timer_scramble_textview);
        mScrambleImage = (ImageView) v.findViewById(R.id.fragment_current_session_timer_scramble_imageview);
        mHListView = (HListView) v.findViewById(R.id.fragment_current_session_timer_bottom_hlistview);

        mStatsText = (TextView) v.findViewById(R.id.fragment_current_session_timer_stats_textview);
        mStatsSolvesText = (TextView) v.findViewById(R.id.fragment_current_session_timer_stats_solves_number_textview);

        mPenaltyBarLinearLayout = (LinearLayout) v.findViewById(R.id.fragment_current_session_timer_penalty_linearlayout);
        mPenaltyDnfButton = (Button) v.findViewById(R.id.fragment_current_session_timer_penalty_dnf_button);
        mPenaltyPlusTwoButton = (Button) v.findViewById(R.id.fragment_current_session_timer_penalty_plustwo_button);

        mPenaltyDnfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION).getLastSolve().setPenalty(Solve.Penalty.DNF);
                onSessionSolvesChanged();
            }
        });

        mPenaltyPlusTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION).getLastSolve().setPenalty(Solve.Penalty.PLUSTWO);
                onSessionSolvesChanged();
            }
        });

        final SolveHListViewAdapter adapter = new SolveHListViewAdapter();
        mHListView.setAdapter(adapter);
        mHListView.setOnItemClickListener(
                new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            it.sephiroth.android.library.widget.AdapterView<?> parent, View view,
                            int position, long id) {
                        try {
                            CreateDialogCallback callback = (CreateDialogCallback) getActivity();
                            callback.createSolveDialog(null, 0, position);
                        } catch (ClassCastException e) {
                            throw new ClassCastException(getActivity().toString()
                                    + " must implement OnDialogDismissedListener");
                        }
                    }
                });

        //When the root view is touched...
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        boolean scrambling = mRetainedFragment.isScrambling();
                        if (mTiming) {
                            //If we're timing and user stopped

                            Solve s = new Solve(mRetainedFragment.getCurrentScrambleAndSvg(),
                                    System.nanoTime() - mTimingStartTimestamp);

                            if (mInspectionEnabled && mLateStartPenalty) {
                                s.setPenalty(Solve.Penalty.PLUSTWO);
                            }
                            //Add the solve to the current session with the current
                            // scramble/scramble image and time
                            PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION).addSolve(s);

                            mPenaltyBarLinearLayout.setVisibility(View.VISIBLE);
                            mPenaltyBarLinearLayout.animate().setStartDelay(1500).alpha(0f).setDuration(150).setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mPenaltyBarLinearLayout.setVisibility(View.GONE);
                                    mPenaltyBarLinearLayout.setAlpha(1f);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                }
                            });


                            resetTimer();

                            setTimerTextToLastSolveTime();

                            //Update stats and HListView
                            onSessionSolvesChanged();

                            if (scrambling) {
                                mScrambleText.setText(R.string.scrambling);
                            }

                            mRetainedFragment.postSetScrambleViewsToCurrent();
                            return false;
                        }
                        if ((mInspecting) || (!mInspectionEnabled && !scrambling &&
                                mHoldToStartEnabled)) {
                            //If hold to start is on or if inspection was started,
                            // start the hold timer
                            startHoldTimer();
                            return true;
                        }
                        return !scrambling;
                    }

                    case MotionEvent.ACTION_UP: {
                        if (mInspectionEnabled && !mInspecting) {
                            //If inspection is on and we're not inspecting
                            startInspection();
                        } else if (mHoldToStartEnabled) {
                            //Inspecting is on or hold to start is on
                            if (mHoldTiming && (System.nanoTime() - mHoldTimerStartTimestamp >=
                                    HOLD_TIME)) {
                                stopInspection();
                                stopHoldTimer();
                                //User held long enough for timer to turn green and lifted: start
                                // timing
                                startTiming();
                                if (!mInspectionEnabled) {
                                    //If hold timer was started not in inspection,
                                    // generate next scramble
                                    mRetainedFragment.generateNextScramble();
                                }
                            } else {
                                //User started hold timer but lifted before the timer is green
                                stopHoldTimer();
                            }
                        } else {
                            //If inspection and hold to start are both off, just start timing
                            startTiming();
                            mRetainedFragment.generateNextScramble();
                        }
                        return false;
                    }

                    default:
                        return false;
                }
            }
        });

        if (!mFromSavedInstanceState) {
            //When the fragment is initializing, disable action bar and generate a scramble.
            mRetainedFragment.resetScramblerThread();
            enableMenuItems(false);
            mScrambleText.setText(R.string.scrambling);
            mRetainedFragment.generateNextScramble();
            mRetainedFragment.postSetScrambleViewsToCurrent();
        } else {
            if (mInspecting) {
                mUiHandler.post(mInspectionRunnable);
            }
            if (mTiming) {
                mUiHandler.post(mTimerRunnable);
            }
            if (mTiming || mInspecting) {
                enableMenuItems(false);
            } else if (!mRetainedFragment.isScrambling()) {
                enableMenuItems(true);
            }
            if (mInspecting || mTiming || !mRetainedFragment.isScrambling()) {
                // If timer is timing/inspecting, then update text/image to current. If timer is
                // not timing/inspecting and not scrambling, then update scramble views to current.
                setScrambleTextAndImageToCurrent();
            } else {
                mScrambleText.setText(R.string.scrambling);
            }
        }

        //If the scramble image is currently displayed and it is not scrambling,
        // then make sure it is set to visible; otherwise, set to gone.
        showScrambleImage(mScrambleImageDisplay && !mRetainedFragment.isScrambling());

        mScrambleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScrambleImage();
            }
        });

        onSessionSolvesChanged();

        return v;
    }

    public void startHoldTimer() {
        mHoldTiming = true;
        mHoldTimerStartTimestamp = System.nanoTime();
        setTextColor(Color.RED);
        mUiHandler.postDelayed(mHoldTimerRunnable, 450);
    }

    public void stopHoldTimer() {
        mHoldTiming = false;
        mHoldTimerStartTimestamp = 0;
        mUiHandler.removeCallbacks(mHoldTimerRunnable);
        setTextColor(Color.BLACK);
    }

    /**
     * Start inspection; Start Generating Next Scramble
     */
    public void startInspection() {
        mInspectionStartTimestamp = System.nanoTime();
        mInspecting = true;
        mUiHandler.post(mInspectionRunnable);
        mRetainedFragment.generateNextScramble();
        enableMenuItems(false);
        showScrambleImage(false);
    }

    public void stopInspection() {
        mInspectionStartTimestamp = 0;
        mInspecting = false;
        mUiHandler.removeCallbacks(mInspectionRunnable);
    }

    /**
     * Start timing; does not start generating next scramble
     */
    public void startTiming() {
        mTimingStartTimestamp = System.nanoTime();
        mInspecting = false;
        mTiming = true;
        mUiHandler.post(mTimerRunnable);
        enableMenuItems(false);
        showScrambleImage(false);
    }

    /**
     * Sets the timer text to last solve's time; if there are no solves, set to ready. Updates
     * the timer text's size.
     */
    public void setTimerTextToLastSolveTime() {
        if (PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                .getNumberOfSolves() != 0) {
            setTimerText(PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                    .getLastSolve().getTimeStringArray(mMillisecondsEnabled));
        } else {
            setTimerText(new String[]{getString(R.string.ready), ""});
            mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100);
        }
        setTimerTextToPrefSize();
    }

    public Handler getUiHandler() {
        return mUiHandler;
    }

    public interface MenuItemsEnableCallback {

        void enableMenuItems(boolean enable);
    }

    public class SolveHListViewAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveHListViewAdapter() {
            super(getActivity(), 0,
                    PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                            .getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Util.getBestSolveOfList(
                    PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                            .getSolves()));
            mBestAndWorstSolves.add(Util.getWorstSolveOfList(
                    PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                            .getSolves()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.hlist_item_solve, parent, false);
            }
            Solve s = getItem(position);
            TextView time = (TextView) convertView.findViewById(R.id.hlist_item_solve_textview);

            time.setText("");

            for (Solve a : mBestAndWorstSolves) {
                if (a == s) {
                    time.setText("(" + s.getTimeString(mMillisecondsEnabled) + ")");
                }
            }

            if (time.getText() == "") {
                time.setText(s.getTimeString(mMillisecondsEnabled));
            }

            return convertView;
        }

        public void updateSolvesList() {
            clear();
            addAll(PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                    .getSolves());

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Util.getBestSolveOfList(
                    PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                            .getSolves()));
            mBestAndWorstSolves.add(Util.getWorstSolveOfList(
                    PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                            .getSolves()));
            notifyDataSetChanged();
        }


    }


}