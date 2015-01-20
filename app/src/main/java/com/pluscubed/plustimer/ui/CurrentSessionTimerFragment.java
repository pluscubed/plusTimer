package com.pluscubed.plustimer.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.BldSolve;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TimerFragment
 */

public class CurrentSessionTimerFragment extends Fragment {

    public static final String TAG = "CURRENT_SESSION_TIMER_FRAGMENT";
    public static final long HOLD_TIME = 550000000L;
    public static final int REFRESH_RATE = 15;
    private static final String STATE_IMAGE_DISPLAYED =
            "scramble_image_displayed_boolean";
    private static final String STATE_START_TIME = "start_time_long";
    private static final String STATE_RUNNING = "running_boolean";
    private static final String STATE_INSPECTING = "inspecting_boolean";
    private static final String STATE_INSPECTION_START_TIME =
            "inspection_start_time_long";
    private final Session.Observer sessionObserver = new Session.Observer() {
        @Override
        public void onSolveAdded() {
            updateStatsAndTimer();
            SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
            adapter.updateSolvesList(-1, ObservedMode.ADD);
        }

        @Override
        public void onSolveChanged(int index) {
            updateStatsAndTimer();
            SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
            adapter.updateSolvesList(index, ObservedMode.SINGLE_CHANGE);
        }

        @Override
        public void onSolveRemoved(int index) {
            updateStatsAndTimer();
            SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
            adapter.updateSolvesList(index, ObservedMode.REMOVE);
        }

        @Override
        public void onReset() {
            updateStatsAndTimer();
            SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
            adapter.updateSolvesList(-1, ObservedMode.RESET);
        }
    };
    private final Runnable mHoldTimerRunnable = new Runnable() {
        @Override
        public void run() {
            setTextColor(Color.GREEN);
            setTimerTextToPrefSize();
        }
    };
    private boolean mHoldToStartEnabled;
    private boolean mInspectionEnabled;
    private boolean mTwoRowTimeEnabled;
    private PrefUtils.TimerUpdate mUpdateTimePref;
    private boolean mMillisecondsEnabled;
    private boolean mMonospaceScrambleFontEnabled;
    private int mPrefSize;
    private boolean mKeepScreenOn;
    private boolean mSignEnabled;


    private CurrentSessionTimerRetainedFragment mRetainedFragment;


    private TextView mTimerText;
    private TextView mTimerText2;
    private TextView mScrambleText;
    private RecyclerView mTimeBarRecycler;
    private ImageView mScrambleImage;
    private TextView mStatsSolvesText;
    private TextView mStatsText;
    private LinearLayout mLastBarLinearLayout;
    private Button mLastDnfButton;
    private Button mLastPlusTwoButton;
    private Button mLastDeleteButton;
    private FrameLayout mDynamicStatusBarFrame;
    private TextView mDynamicStatusBarText;


    private Handler mUiHandler;


    private boolean mHoldTiming;
    private long mHoldTimerStartTimestamp;
    private boolean mInspecting;
    private long mInspectionStartTimestamp;
    private long mInspectionStopTimestamp;
    private long mTimingStartTimestamp;
    private boolean mFromSavedInstanceState;
    private boolean mTiming;
    private boolean mScrambleImageDisplay;
    private boolean mLateStartPenalty;
    private final Runnable mInspectionRunnable = new Runnable() {
        @Override
        public void run() {
            String[] array = Utils.timeStringsFromNsSplitByDecimal
                    (16000000000L - (System.nanoTime() -
                            mInspectionStartTimestamp), mMillisecondsEnabled);
            array[1] = "";

            if (15000000000L - (System.nanoTime() -
                    mInspectionStartTimestamp) > 0) {
                //If inspection proceeding normally
                setTimerText(array);
            } else {
                if (17000000000L - (System.nanoTime() -
                        mInspectionStartTimestamp) > 0) {
                    //If late start
                    mLateStartPenalty = true;
                    setTimerText(new String[]{"+2", ""});
                } else {
                    //If past 17 seconds which means DNF
                    stopHoldTimer();
                    stopInspection();

                    Solve s = new Solve(mRetainedFragment
                            .getCurrentScrambleAndSvg(), 0);
                    s.setPenalty(Solve.Penalty.DNF);

                    //Add the solve to the current session with the current
                    // scramble/scramble
                    // image and DNF
                    PuzzleType.getCurrent().getSession(PuzzleType
                            .CURRENT_SESSION)
                            .addSolve(s);

                    resetTimer();
                    setTimerTextToLastSolveTime();


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
    private boolean mBldMode;
    private final PuzzleType.Observer puzzleTypeObserver = new PuzzleType
            .Observer() {
        @Override
        public void onPuzzleTypeChanged() {
            //Update quick stats and hlistview
            onSessionSolvesChanged();

            //Set timer text to ready, scramble text to scrambling
            mScrambleText.setText(R.string.scrambling);

            //Update options menu (disable)
            enableMenuItems(false);
            showScrambleImage(false);

            mBldMode = PuzzleType.getCurrent().name().contains("BLD");

            resetGenerateScramble();

            resetTimer();

            PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                    .registerObserver(sessionObserver);
        }
    };
    private final Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mUpdateTimePref != PrefUtils.TimerUpdate.OFF) {
                if (!mBldMode) {
                    setTimerText(Utils.timeStringsFromNsSplitByDecimal(
                            System.nanoTime() - mTimingStartTimestamp,
                            mMillisecondsEnabled));
                } else {
                    setTimerText(Utils.timeStringsFromNsSplitByDecimal(
                            System.nanoTime() - mInspectionStartTimestamp,
                            mMillisecondsEnabled));
                }
                setTimerTextToPrefSize();
                mUiHandler.postDelayed(this, REFRESH_RATE);
            } else {
                setTimerText(new String[]{getString(R.string.timing), ""});
                mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100);
            }
        }
    };

    //Generate string with specified current averages and mean of current
    // session
    private String buildStatsWithAveragesOf(Context context,
                                            Integer... currentAverages) {
        Arrays.sort(currentAverages, Collections.reverseOrder());
        String s = "";
        for (int i : currentAverages) {
            if (PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                    .getNumberOfSolves() >= i) {
                s += String.format(context.getString(R.string.cao),
                        i) + ": " + PuzzleType.getCurrent().getSession
                        (PuzzleType.CURRENT_SESSION)
                        .getStringCurrentAverageOf(i, mMillisecondsEnabled) +
                        "\n";
            }
        }
        if (PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                .getNumberOfSolves() > 0) {
            s += context.getString(R.string.mean) + PuzzleType.getCurrent()
                    .getSession(PuzzleType.CURRENT_SESSION).getStringMean
                            (mMillisecondsEnabled);
        }
        return s;
    }

    /**
     * Set timer textviews using an array. Hides/shows lower textview
     * depending on preferences
     * and whether the second array item is blank.
     *
     * @param array An array of 2 strings
     */
    public void setTimerText(String[] array) {
        if (mTwoRowTimeEnabled) {
            mTimerText.setText(array[0]);
            mTimerText2.setText(array[1]);
            if (array[1].equals("") || (mTiming && mUpdateTimePref != PrefUtils.TimerUpdate.ON)) {
                mTimerText2.setVisibility(View.GONE);
            } else {
                mTimerText2.setVisibility(View.VISIBLE);
            }
        } else {
            mTimerText2.setVisibility(View.GONE);
            mTimerText.setText(array[0]);
            if (!array[1].equals("") && !(mTiming && (mUpdateTimePref != PrefUtils.TimerUpdate.ON))) {
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
        outState.putLong(STATE_INSPECTION_START_TIME,
                mInspectionStartTimestamp);
    }

    //Set scramble text and scramble image to current ones
    public void setScrambleTextAndImageToCurrent() {
        if (mRetainedFragment.getCurrentScrambleAndSvg() != null) {
            SVG svg = null;
            try {
                svg = SVG.getFromString(mRetainedFragment
                        .getCurrentScrambleAndSvg().getSvg());
            } catch (SVGParseException e) {
                e.printStackTrace();
            }
            Drawable drawable = new PictureDrawable(svg.renderToPicture());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mScrambleImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            mScrambleImage.setImageDrawable(drawable);

            mScrambleText.setText(mRetainedFragment.getCurrentScrambleAndSvg
                    ().getUiScramble(mSignEnabled, PuzzleType.getCurrent()
                    .name()));
        } else {
            mRetainedFragment.generateNextScramble();
            mRetainedFragment.postSetScrambleViewsToCurrent();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        PuzzleType.registerObserver(puzzleTypeObserver);
        PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                .registerObserver(sessionObserver);

        //Set up UIHandler
        mUiHandler = new Handler(Looper.getMainLooper());

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences,
                false);
        initSharedPrefs();

        if (savedInstanceState != null) {
            mScrambleImageDisplay = savedInstanceState.getBoolean
                    (STATE_IMAGE_DISPLAYED);
            mTimingStartTimestamp = savedInstanceState.getLong
                    (STATE_START_TIME);
            mTiming = savedInstanceState.getBoolean(STATE_RUNNING);
            mInspecting = savedInstanceState.getBoolean(STATE_INSPECTING);
            mInspectionStartTimestamp = savedInstanceState.getLong
                    (STATE_INSPECTION_START_TIME);
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

        PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                .unregisterObserver(sessionObserver);
        PuzzleType.unregisterObserver(puzzleTypeObserver);
    }

    public void onSessionSolvesChanged() {
        updateStatsAndTimer();

        //Update RecyclerView
        SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
        adapter.updateSolvesList(-1, ObservedMode.UPDATE_ALL);
    }

    private void updateStatsAndTimer() {
        //Update stats
        mStatsSolvesText.setText(getString(R.string.solves) + PuzzleType
                .getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                .getNumberOfSolves());
        mStatsText.setText(buildStatsWithAveragesOf(getActivity(), 5, 12, 100));

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
            getActivity().getWindow().addFlags(WindowManager.LayoutParams
                    .FLAG_KEEP_SCREEN_ON);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams
                    .FLAG_KEEP_SCREEN_ON);
        }

        if (mMonospaceScrambleFontEnabled) {
            mScrambleText.setTypeface(Typeface.MONOSPACE);
        } else {
            mScrambleText.setTypeface(Typeface.DEFAULT);
        }

        //When Settings change
        onSessionSolvesChanged();
    }

    public void initSharedPrefs() {
        mInspectionEnabled = PrefUtils.isInspectionEnabled(getActivity());
        mHoldToStartEnabled = PrefUtils.isHoldToStartEnabled(getActivity());
        mTwoRowTimeEnabled =
                getResources().getConfiguration().orientation == 1
                        && PrefUtils.isTwoRowTimeEnabled(getActivity());
        mUpdateTimePref = PrefUtils.getTimerUpdateMode(getActivity());
        mMillisecondsEnabled = PrefUtils.isDisplayMillisecondsEnabled(getActivity());
        mPrefSize = PrefUtils.getTimerTextSize(getActivity());
        mKeepScreenOn = PrefUtils.isKeepScreenOnEnabled(getActivity());
        mSignEnabled = PrefUtils.isSignEnabled(getActivity());
        mMonospaceScrambleFontEnabled = PrefUtils.isMonospaceScrambleFontEnabled(getActivity());
    }

    public void setTimerTextToPrefSize() {
        if (mTimerText.getText() != getString(R.string.ready)) {
            if (mTimerText != null && mTimerText2 != null) {
                if (mTwoRowTimeEnabled) {
                    mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                            mPrefSize);
                } else {
                    mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                            mPrefSize * 0.7F);
                }
                mTimerText2.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                        mPrefSize / 2);
            }
        }
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
        setTextColorPrimary();
        playDynamicStatusBarExitAnimation();
    }

    private void setTextColorPrimary() {
        int[] textColorAttr = new int[]{android.R.attr.textColor};
        TypedArray a = getActivity().obtainStyledAttributes(new TypedValue().data, textColorAttr);
        int color = a.getColor(0, -1);
        a.recycle();
        setTextColor(color);
    }

    public void setTextColor(int color) {
        mTimerText.setTextColor(color);
        mTimerText2.setTextColor(color);
    }

    public void enableMenuItems(boolean enable) {
        ActivityCallback callback = getActivityCallback();
        callback.enableMenuItems(enable);
    }

    private ActivityCallback getActivityCallback() {
        ActivityCallback callback;
        try {
            callback = (ActivityCallback) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    getActivity().toString() + " must implement " +
                            "ActivityCallback");
        }
        return callback;
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
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams
                .FLAG_KEEP_SCREEN_ON);
        PuzzleType.getCurrent().saveCurrentSession(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_session_timer,
                container, false);

        mTimerText = (TextView) v.findViewById(R.id
                .fragment_current_session_timer_time_textview);
        mTimerText2 = (TextView) v.findViewById(R.id
                .fragment_current_session_timer_timeSecondary_textview);
        mScrambleText = (TextView) v.findViewById(R.id
                .fragment_current_session_timer_scramble_textview);
        mScrambleImage = (ImageView) v.findViewById(R.id
                .fragment_current_session_timer_scramble_imageview);
        mTimeBarRecycler = (RecyclerView) v.findViewById(R.id
                .fragment_current_session_timer_timebar_recycler);

        mStatsText = (TextView) v.findViewById(R.id
                .fragment_current_session_timer_stats_textview);
        mStatsSolvesText = (TextView) v.findViewById(R.id
                .fragment_current_session_timer_stats_solves_number_textview);

        mLastBarLinearLayout = (LinearLayout) v.findViewById(R.id
                .fragment_current_session_timer_last_linearlayout);
        mLastDnfButton = (Button) v.findViewById(R.id
                .fragment_current_session_timer_last_dnf_button);
        mLastPlusTwoButton = (Button) v.findViewById(R.id
                .fragment_current_session_timer_last_plustwo_button);
        mLastDeleteButton = (Button) v.findViewById(R.id
                .fragment_current_session_timer_last_delete_button);

        mLastDnfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PuzzleType.getCurrent().getSession(PuzzleType
                        .CURRENT_SESSION).getLastSolve().setPenalty(Solve
                        .Penalty.DNF);
                playLastBarExitAnimation();
            }
        });

        mLastPlusTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PuzzleType.getCurrent().getSession(PuzzleType
                        .CURRENT_SESSION).getLastSolve().setPenalty(Solve
                        .Penalty.PLUSTWO);
                playLastBarExitAnimation();
            }
        });

        mLastDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Session session = PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION);
                session.deleteSolve(session.getLastSolve());
                playLastBarExitAnimation();
            }
        });

        LinearLayoutManager timeBarLayoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.HORIZONTAL, false);
        mTimeBarRecycler.setLayoutManager(timeBarLayoutManager);
        mTimeBarRecycler.setHasFixedSize(true);
        mTimeBarRecycler.setAdapter(new SolveRecyclerAdapter());

        mDynamicStatusBarFrame = (FrameLayout) v.findViewById(R.id.fragment_current_session_timer_dynamic_status_frame);
        mDynamicStatusBarText = (TextView) v.findViewById(R.id.fragment_current_session_timer_dynamic_status_text);

        mRetainedFragment = getActivityCallback().getTimerRetainedFragment();
        mRetainedFragment.setTargetFragment(this, 0);

        //When the root view is touched...
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        boolean scrambling = mRetainedFragment.isScrambling();
                        if (mTiming) {
                            //If we're timing and user stopped

                            Solve s;
                            if (!mBldMode) {
                                s = new Solve(mRetainedFragment
                                        .getCurrentScrambleAndSvg(),
                                        System.nanoTime() - mTimingStartTimestamp);
                            } else {
                                s = new BldSolve(mRetainedFragment.getCurrentScrambleAndSvg(),
                                        System.nanoTime() - mTimingStartTimestamp,
                                        mInspectionStopTimestamp - mInspectionStartTimestamp);
                            }

                            if (mInspectionEnabled && mLateStartPenalty) {
                                s.setPenalty(Solve.Penalty.PLUSTWO);
                            }
                            //Add the solve to the current session with the
                            // current scramble/scramble image and time
                            PuzzleType.getCurrent().getSession(PuzzleType
                                    .CURRENT_SESSION).addSolve(s);
                            playLastBarEnterAnimation();
                            playDynamicStatusBarExitAnimation();

                            resetTimer();

                            setTimerTextToLastSolveTime();

                            //Update stats and HListView

                            if (scrambling) {
                                mScrambleText.setText(R.string.scrambling);
                            }

                            mRetainedFragment.postSetScrambleViewsToCurrent();
                            return false;
                        }
                        if (!mBldMode && mHoldToStartEnabled &&
                                ((!mInspectionEnabled && !scrambling) || mInspecting)) {
                            //If hold to start is on and/or inspecting, start the hold timer
                            startHoldTimer();
                            return true;
                        } else if (mInspecting) {
                            //If inspecting and hold to start is off, start regular timer
                            //BLD inspecting as well
                            return true;
                        }
                        return !scrambling;
                    }

                    case MotionEvent.ACTION_UP: {
                        if ((mInspectionEnabled || mBldMode) && !mInspecting) {
                            //If inspection is on (or BLD) and not inspecting
                            startInspection();
                        } else if (!mBldMode && mHoldToStartEnabled) {
                            //Hold to start is on (may be inspecting)
                            if (mHoldTiming &&
                                    (System.nanoTime() - mHoldTimerStartTimestamp >=
                                            HOLD_TIME)) {
                                stopInspection();
                                stopHoldTimer();
                                //User held long enough for timer to turn
                                // green and lifted: start timing
                                startTiming();
                                if (!mBldMode && !mInspectionEnabled) {
                                    //If hold timer was started but not in
                                    // inspection, generate next scramble
                                    mRetainedFragment.generateNextScramble();
                                }
                            } else {
                                //User started hold timer but lifted before
                                // the timer is green
                                stopHoldTimer();
                            }
                        } else {
                            //Hold to start is off, start timing
                            if (mInspecting) {
                                stopInspection();
                            }
                            startTiming();
                            if (!mBldMode) {
                                mRetainedFragment.generateNextScramble();
                            }
                        }
                        return false;
                    }

                    default:
                        return false;
                }
            }
        });

        if (!mFromSavedInstanceState) {
            //When the fragment is initializing, disable action bar and
            // generate a scramble.
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
                // If timer is timing/inspecting, then update text/image to
                // current. If timer is
                // not timing/inspecting and not scrambling,
                // then update scramble views to current.
                setScrambleTextAndImageToCurrent();
            } else {
                mScrambleText.setText(R.string.scrambling);
            }
        }

        //If the scramble image is currently displayed and it is not scrambling,
        // then make sure it is set to visible; otherwise, set to gone.
        showScrambleImage(mScrambleImageDisplay && !mRetainedFragment
                .isScrambling());

        mScrambleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScrambleImage();
            }
        });

        mBldMode = PuzzleType.getCurrent().name().contains("BLD");

        onSessionSolvesChanged();

        return v;
    }

    public void playDynamicStatusBarEnterAnimation() {
        ObjectAnimator enter = ObjectAnimator.ofFloat(mDynamicStatusBarFrame, View.TRANSLATION_Y, 0f);
        enter.setDuration(125);
        enter.setInterpolator(new DecelerateInterpolator());
        AnimatorSet dynamicStatusBarAnimatorSet = new AnimatorSet();
        dynamicStatusBarAnimatorSet.play(enter);
        dynamicStatusBarAnimatorSet.start();
    }

    public void playDynamicStatusBarExitAnimation() {
        ObjectAnimator exit = ObjectAnimator.ofFloat(mDynamicStatusBarFrame, View.TRANSLATION_Y, mDynamicStatusBarFrame.getHeight());
        exit.setDuration(125);
        exit.setInterpolator(new AccelerateInterpolator());
        AnimatorSet dynamicStatusBarAnimatorSet = new AnimatorSet();
        dynamicStatusBarAnimatorSet.play(exit);
        dynamicStatusBarAnimatorSet.start();

    }

    private void playLastBarEnterAnimation() {
        ObjectAnimator enter = ObjectAnimator.ofFloat(mLastBarLinearLayout, View.TRANSLATION_Y, -mLastBarLinearLayout.getHeight());
        ObjectAnimator exit = ObjectAnimator.ofFloat(mLastBarLinearLayout, View.TRANSLATION_Y, 0f);
        enter.setDuration(125);
        exit.setDuration(125);
        exit.setStartDelay(1500);
        enter.setInterpolator(new DecelerateInterpolator());
        exit.setInterpolator(new AccelerateInterpolator());
        AnimatorSet lastBarAnimationSet = new AnimatorSet();
        lastBarAnimationSet.playSequentially(enter, exit);
        lastBarAnimationSet.start();
    }

    public void playLastBarExitAnimation() {
        ObjectAnimator exit = ObjectAnimator.ofFloat(mLastBarLinearLayout, View.TRANSLATION_Y, 0f);
        exit.setDuration(125);
        exit.setInterpolator(new AccelerateInterpolator());
        AnimatorSet lastBarAnimationSet = new AnimatorSet();
        lastBarAnimationSet.play(exit);
        lastBarAnimationSet.start();
    }

    public void startHoldTimer() {
        playLastBarExitAnimation();
        mHoldTiming = true;
        mHoldTimerStartTimestamp = System.nanoTime();
        setTextColor(Color.RED);
        mUiHandler.postDelayed(mHoldTimerRunnable, 550);
    }

    public void stopHoldTimer() {
        mHoldTiming = false;
        mHoldTimerStartTimestamp = 0;
        mUiHandler.removeCallbacks(mHoldTimerRunnable);
        setTextColorPrimary();
    }

    /**
     * Start inspection; Start Generating Next Scramble
     */
    public void startInspection() {
        playLastBarExitAnimation();
        mDynamicStatusBarText.setText(R.string.inspecting);
        playDynamicStatusBarEnterAnimation();
        mInspectionStartTimestamp = System.nanoTime();
        mInspecting = true;
        if (mBldMode) {
            mUiHandler.post(mTimerRunnable);
        } else {
            mUiHandler.post(mInspectionRunnable);
        }
        mRetainedFragment.generateNextScramble();
        enableMenuItems(false);
        showScrambleImage(false);
    }

    public void stopInspection() {
        mInspectionStopTimestamp = System.nanoTime();
        mInspecting = false;
        mUiHandler.removeCallbacks(mInspectionRunnable);
    }

    /**
     * Start timing; does not start generating next scramble
     */
    public void startTiming() {
        playLastBarExitAnimation();
        playDynamicStatusBarEnterAnimation();
        mTimingStartTimestamp = System.nanoTime();
        mInspecting = false;
        mTiming = true;
        if (!mBldMode) mUiHandler.post(mTimerRunnable);
        enableMenuItems(false);
        showScrambleImage(false);
        mDynamicStatusBarText.setText(R.string.timing);
    }

    /**
     * Sets the timer text to last solve's time; if there are no solves,
     * set to ready. Updates the timer text's size.
     */
    public void setTimerTextToLastSolveTime() {
        if (PuzzleType.getCurrent().getSession(PuzzleType.CURRENT_SESSION)
                .getNumberOfSolves() != 0) {
            setTimerText(PuzzleType.getCurrent().getSession(PuzzleType
                    .CURRENT_SESSION)
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

    public enum ObservedMode {
        ADD, REMOVE, RESET, SINGLE_CHANGE, UPDATE_ALL
    }

    public interface ActivityCallback {
        Toolbar getActionBarToolbar();

        CurrentSessionTimerRetainedFragment getTimerRetainedFragment();

        void enableMenuItems(boolean enable);
    }

    public class SolveRecyclerAdapter extends RecyclerView
            .Adapter<SolveRecyclerAdapter.ViewHolder> {

        private List<Solve> mSolves;

        private Solve[] mBestAndWorstSolves;

        public SolveRecyclerAdapter() {
            mBestAndWorstSolves = new Solve[2];
            updateSolvesList(-1, ObservedMode.UPDATE_ALL);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView v = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycler_item_solve, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Solve s = mSolves.get(position);
            String timeString = s.getTimeString(mMillisecondsEnabled);
            holder.textView.setText(timeString);
            for (Solve a : mBestAndWorstSolves) {
                if (a == s) {
                    holder.textView.setText("(" + timeString + ")");
                }
            }
        }

        @Override
        public int getItemCount() {
            return mSolves.size();
        }

        private void updateSolvesList(int position, ObservedMode mode) {
            int oldSize = 0;
            if (mSolves != null) oldSize = mSolves.size();
            mSolves = PuzzleType.getCurrent().getSession(PuzzleType
                    .CURRENT_SESSION).getSolves();
            mBestAndWorstSolves[0] = Utils.getBestSolveOfList(mSolves);
            mBestAndWorstSolves[1] = Utils.getWorstSolveOfList(mSolves);

            switch (mode) {
                case UPDATE_ALL:
                    notifyDataSetChanged();
                    if (mSolves.size() >= 1)
                        mTimeBarRecycler.smoothScrollToPosition(mSolves.size() - 1);
                    break;
                case ADD:
                    //TODO: Smooth scroll so empty space for insertion opens up
                    notifyItemInserted(mSolves.size() - 1);
                    mTimeBarRecycler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Run after status bar goes down
                            if (mSolves.size() >= 1)
                                mTimeBarRecycler.smoothScrollToPosition(mSolves.size() - 1);
                        }
                    }, 200);
                    break;
                case REMOVE:
                    notifyItemRemoved(position);
                    break;
                case SINGLE_CHANGE:
                    notifyItemChanged(position);
                    break;
                case RESET:
                    notifyItemRangeRemoved(0, oldSize);
                    break;
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView textView;

            public ViewHolder(TextView v) {
                super(v);
                textView = v;
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            CreateDialogCallback callback =
                                    (CreateDialogCallback) getActivity();
                            callback.createSolveDisplayDialog(null, 0, getPosition());
                        } catch (ClassCastException e) {
                            throw new ClassCastException(getActivity()
                                    .toString() + " must implement " +
                                    "CreateDialogCallback");
                        }
                    }
                });
            }
        }


    }


}