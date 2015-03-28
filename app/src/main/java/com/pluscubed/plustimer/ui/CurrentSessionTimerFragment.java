package com.pluscubed.plustimer.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
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
import android.util.Property;
import android.util.TypedValue;
import android.view.Display;
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
import com.pluscubed.plustimer.model.ScrambleAndSvg;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.utils.ErrorUtils;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.SolveDialogUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TimerFragment
 */

public class CurrentSessionTimerFragment extends Fragment {

    public static final String TAG = "CURRENT_SESSION_TIMER_FRAGMENT";
    private static final long HOLD_TIME = 550000000L;
    private static final int REFRESH_RATE = 15;
    private static final String STATE_IMAGE_DISPLAYED =
            "scramble_image_displayed_boolean";
    private static final String STATE_START_TIME = "start_time_long";
    private static final String STATE_RUNNING = "running_boolean";
    private static final String STATE_INSPECTING = "inspecting_boolean";
    private static final String STATE_INSPECTION_START_TIME =
            "inspection_start_time_long";
    //Preferences
    private boolean mHoldToStartEnabled;
    private boolean mInspectionEnabled;
    private boolean mTwoRowTimeEnabled;
    private PrefUtils.TimerUpdate mUpdateTimePref;
    private boolean mMillisecondsEnabled;
    private boolean mMonospaceScrambleFontEnabled;
    private int mPrefSize;
    private boolean mKeepScreenOn;
    private boolean mSignEnabled;
    //Retained Fragment
    private CurrentSessionTimerRetainedFragment mRetainedFragment;
    //Views
    private TextView mTimerText;
    private TextView mTimerText2;
    private TextView mScrambleText;
    private View mScrambleTextShadow;
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
    //Handler
    private Handler mUiHandler;
    //Dynamic status variables
    private boolean mDynamicStatusBarVisible;
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
    private boolean mBldMode;
    private AnimatorSet mLastBarAnimationSet;
    private ValueAnimator mScrambleAnimator;
    private ObjectAnimator mScrambleElevationAnimator;

    //Runnables
    private Runnable holdTimerRunnable = new Runnable() {
        @Override
        public void run() {
            setTextColor(Color.GREEN);
            setTimerTextToPrefSize();
            if (!mInspecting) {
                playExitAnimations();
                getActivityCallback().lockDrawerAndViewPager(true);
            }
        }
    };
    private Session.Observer sessionObserver = new Session.Observer() {
        @Override
        public void onSolveAdded() {
            updateStatsAndTimer();
            SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
            adapter.updateSolvesList(-1, Update.INSERT);
        }

        @Override
        public void onSolveChanged(int index) {
            updateStatsAndTimer();
            SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
            adapter.updateSolvesList(index, Update.SINGLE_CHANGE);
        }

        @Override
        public void onSolveRemoved(int index) {
            updateStatsAndTimer();
            SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
            adapter.updateSolvesList(index, Update.REMOVE);
        }

        @Override
        public void onReset() {
            updateStatsAndTimer();
            SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
            adapter.updateSolvesList(-1, Update.REMOVE_ALL);
        }
    };
    private Runnable inspectionRunnable = new Runnable() {
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

                    playEnterAnimations();

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
                        setScrambleText(getString(R.string.scrambling));
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
    private PuzzleType.Observer puzzleTypeObserver =
            new PuzzleType.Observer() {
                @Override
                public void onPuzzleTypeChanged() {
                    //Update quick stats and hlistview
                    onSessionSolvesChanged();

                    //Set timer text to ready, scramble text to scrambling
                    setScrambleText(getString(R.string.scrambling));

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
    private Runnable timerRunnable = new Runnable() {
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

    //Taken from http://stackoverflow.com/questions/19908003
    public static int getTextViewHeight(TextView textView) {
        WindowManager wm =
                (WindowManager) textView.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int deviceWidth;

        Point size = new Point();
        display.getSize(size);
        deviceWidth = size.x;

        int widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(deviceWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        return textView.getMeasuredHeight();
    }

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
    void setTimerText(String[] array) {
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
            if (!array[1].equals("") && !(mTiming && (mUpdateTimePref != PrefUtils.TimerUpdate
                    .ON))) {
                mTimerText.append("." + array[1]);
            }
        }
    }

    void setScrambleText(String text) {
        mScrambleText.setText(text);
        invalidateScrambleShadow(false);
    }

    private void invalidateScrambleShadow(final boolean overrideShowShadow) {
        Runnable animate = new Runnable() {
            @Override
            public void run() {
                if (mScrambleElevationAnimator != null) {
                    mScrambleElevationAnimator.cancel();
                }
                Property<View, Float> property;
                View view;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    property = View.TRANSLATION_Z;
                    view = mScrambleText;
                } else {
                    property = View.ALPHA;
                    view = mScrambleTextShadow;
                }
                mScrambleElevationAnimator = ObjectAnimator.ofFloat(view,
                        property, getScrambleTextElevationOrShadowAlpha(overrideShowShadow));
                mScrambleElevationAnimator.setDuration(150);
                mScrambleElevationAnimator.start();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mScrambleText.postOnAnimation(animate);
        } else {
            mScrambleText.post(animate);
        }
    }

    //Taken from http://stackoverflow.com/questions/3619693
    private int getRelativeTop(View view) {
        if (view.getParent() == view.getRootView())
            return view.getTop();
        else
            return view.getTop() + getRelativeTop((View) view.getParent());
    }

    private float getScrambleTextElevationOrShadowAlpha(boolean override) {
        boolean overlap = getRelativeTop(mScrambleText) + getTextViewHeight(mScrambleText)
                > getRelativeTop(mTimerText);
        float rootFrameTranslationY = getActivityCallback() != null ?
                getActivityCallback().getContentFrameLayout().getTranslationY() : 0f;
        boolean shadowShown = overlap || rootFrameTranslationY != 0 || override;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return shadowShown ? Utils.convertDpToPx(getActivity(), 2) : 0f;
        } else {
            return shadowShown ? 1f : 0f;
        }
    }

    //Set scramble text and scramble image to current ones
    public void setScrambleTextAndImageToCurrent() {
        ScrambleAndSvg currentScrambleAndSvg = mRetainedFragment.getCurrentScrambleAndSvg();
        if (currentScrambleAndSvg != null) {
            SVG svg = null;
            try {
                svg = SVG.getFromString(currentScrambleAndSvg.getSvg());
            } catch (SVGParseException e) {
                e.printStackTrace();
            }
            Drawable drawable = null;
            if (svg != null) {
                drawable = new PictureDrawable(svg.renderToPicture());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mScrambleImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            mScrambleImage.setImageDrawable(drawable);

            setScrambleText(ErrorUtils.getUiScramble(getActivity(), -1,
                    currentScrambleAndSvg, mSignEnabled,
                    PuzzleType.getCurrent().name()));
        } else {
            mRetainedFragment.generateNextScramble();
            mRetainedFragment.postSetScrambleViewsToCurrent();
        }
    }

    void onSessionSolvesChanged() {
        updateStatsAndTimer();

        //Update RecyclerView
        SolveRecyclerAdapter adapter = (SolveRecyclerAdapter) mTimeBarRecycler.getAdapter();
        adapter.updateSolvesList(-1, Update.DATA_RESET);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_session_timer,
                container, false);

        mTimerText = (TextView) v.findViewById(R.id.
                fragment_current_session_timer_time_textview);
        mTimerText2 = (TextView) v.findViewById(R.id.
                fragment_current_session_timer_timeSecondary_textview);
        mScrambleText = (TextView) v.findViewById(R.id.
                fragment_current_session_timer_scramble_textview);
        mScrambleTextShadow = v.findViewById(R.id.fragment_current_session_timer_scramble_shadow);
        mScrambleImage = (ImageView) v.findViewById(R.id.
                fragment_current_session_timer_scramble_imageview);
        mTimeBarRecycler = (RecyclerView) v.findViewById(R.id.
                fragment_current_session_timer_timebar_recycler);

        mStatsText = (TextView) v.findViewById(R.id.fragment_current_session_timer_stats_textview);
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
                Session currentSession = PuzzleType.getCurrent().getSession(PuzzleType
                        .CURRENT_SESSION);
                if (ErrorUtils.isSolveNonexistent(getActivity(), PuzzleType.getCurrent().name(),
                        PuzzleType.CURRENT_SESSION, currentSession.getNumberOfSolves() - 1)) {
                    return;
                }
                currentSession.getLastSolve().setPenalty(Solve
                        .Penalty.DNF);
                playLastBarExitAnimation();
            }
        });

        mLastPlusTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Session currentSession = PuzzleType.getCurrent().getSession(PuzzleType
                        .CURRENT_SESSION);
                if (ErrorUtils.isSolveNonexistent(getActivity(), PuzzleType.getCurrent().name(),
                        PuzzleType.CURRENT_SESSION, currentSession.getNumberOfSolves() - 1)) {
                    return;
                }
                currentSession.getLastSolve().setPenalty(Solve
                        .Penalty.PLUSTWO);
                playLastBarExitAnimation();
            }
        });

        mLastDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Session currentSession = PuzzleType.getCurrent().getSession(PuzzleType
                        .CURRENT_SESSION);
                if (ErrorUtils.isSolveNonexistent(getActivity(), PuzzleType.getCurrent().name(),
                        PuzzleType.CURRENT_SESSION, currentSession.getNumberOfSolves() - 1)) {
                    return;
                }
                currentSession.deleteSolve(currentSession.getLastSolve());
                playLastBarExitAnimation();
            }
        });

        LinearLayoutManager timeBarLayoutManager = new LinearLayoutManager(getActivity(),
                LinearLayoutManager.HORIZONTAL, false);
        mTimeBarRecycler.setLayoutManager(timeBarLayoutManager);
        mTimeBarRecycler.setHasFixedSize(true);
        mTimeBarRecycler.setAdapter(new SolveRecyclerAdapter());

        mDynamicStatusBarFrame = (FrameLayout) v.findViewById(R.id
                .fragment_current_session_timer_dynamic_status_frame);
        mDynamicStatusBarText = (TextView) v.findViewById(R.id
                .fragment_current_session_timer_dynamic_status_text);

        mRetainedFragment = getActivityCallback().getTimerRetainedFragment();
        mRetainedFragment.setTargetFragment(this, 0);

        //When the root view is touched...
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        return onTimerTouchDown();
                    }
                    case MotionEvent.ACTION_UP: {
                        onTimerTouchUp();
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
            setScrambleText(getString(R.string.scrambling));
            mRetainedFragment.generateNextScramble();
            mRetainedFragment.postSetScrambleViewsToCurrent();
        } else {
            if (mInspecting) {
                mUiHandler.post(inspectionRunnable);
            }
            if (mTiming) {
                mUiHandler.post(timerRunnable);
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
                setScrambleText(getString(R.string.scrambling));
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

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams
                .FLAG_KEEP_SCREEN_ON);
        PuzzleType.getCurrent().saveCurrentSession(getActivity());
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

    void initSharedPrefs() {
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

    void setTimerTextToPrefSize() {
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

    void showScrambleImage(boolean enable) {
        if (enable) {
            mScrambleImage.setVisibility(View.VISIBLE);
        } else {
            mScrambleImage.setVisibility(View.GONE);
        }
        mScrambleImageDisplay = enable;
    }

    private void setTextColorPrimary() {
        int[] textColorAttr = new int[]{android.R.attr.textColor};
        TypedArray a = getActivity().obtainStyledAttributes(new TypedValue().data, textColorAttr);
        int color = a.getColor(0, -1);
        a.recycle();
        setTextColor(color);
    }

    void setTextColor(int color) {
        mTimerText.setTextColor(color);
        mTimerText2.setTextColor(color);
    }

    /**
     * Sets the timer text to last solve's time; if there are no solves,
     * set to ready. Updates the timer text's size.
     */
    void setTimerTextToLastSolveTime() {
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

    void toggleScrambleImage() {
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

    /**
     * @return whether
     * {@link com.pluscubed.plustimer.ui.CurrentSessionTimerFragment#onTimerTouchUp()}
     * will be triggered when touch is released
     */
    private synchronized boolean onTimerTouchDown() {
        boolean scrambling = mRetainedFragment.isScrambling();

        //Currently Timing: User stopping timer
        if (mTiming) {
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
            playEnterAnimations();
            getActivityCallback().lockDrawerAndViewPager(false);

            resetTimer();

            setTimerTextToLastSolveTime();


            if (scrambling) {
                setScrambleText(getString(R.string.scrambling));
            }

            mRetainedFragment.postSetScrambleViewsToCurrent();
            return false;
        }

        if (mBldMode) {
            return onTimerBldTouchDown(scrambling);
        }

        if (mHoldToStartEnabled &&
                ((!mInspectionEnabled && !scrambling) || mInspecting)) {
            //If hold to start is on, start the hold timer
            //If inspection is enabled, only start hold timer when inspecting
            //Go to section 2
            startHoldTimer();
            return true;
        } else if (mInspecting) {
            //If inspecting and hold to start is off, start regular timer
            //Go to section 3
            setTextColor(Color.GREEN);
            return true;
        }

        //If inspection is on and haven't started yet: section 1
        //If hold to start and inspection are both off: section 3
        if (!scrambling) {
            setTextColor(Color.GREEN);
            return true;
        }
        return false;
    }

    private synchronized boolean onTimerBldTouchDown(boolean scrambling) {
        //If inspecting: section 3
        //If not inspecting yet and not scrambling: section 1
        setTextColor(Color.GREEN);
        return mInspecting || !scrambling;
    }


    private synchronized void onTimerTouchUp() {
        if ((mInspectionEnabled || mBldMode) && !mInspecting) {
            //Section 1
            //If inspection is on (or BLD) and not inspecting
            startInspection();
            playExitAnimations();
        } else if (!mBldMode && mHoldToStartEnabled) {
            //Section 2
            //Hold to start is on (may be inspecting)
            if (mHoldTiming &&
                    (System.nanoTime() - mHoldTimerStartTimestamp >=
                            HOLD_TIME)) {
                //User held long enough for timer to turn
                // green and lifted: start timing
                stopInspection();
                stopHoldTimer();
                startTiming();
                if (!mBldMode && !mInspectionEnabled) {
                    //If hold timer was started but not in
                    // inspection, generate next scramble
                    mRetainedFragment.generateNextScramble();
                }
            } else {
                //User started hold timer but lifted before
                // the timer is green: stop hold timer
                stopHoldTimer();
            }
        } else {
            //Section 3
            //Hold to start is off, start timing
            if (mInspecting) {
                stopInspection();
            } else {
                playExitAnimations();
            }
            startTiming();
            if (!mBldMode) {
                mRetainedFragment.generateNextScramble();
            }
        }
    }

    private void playExitAnimations() {
        Utils.lockOrientation(getActivity());
        playScrambleExitAnimation();
        playStatsExitAnimation();
        getActivityCallback().playToolbarExitAnimation();
    }

    public void playEnterAnimations() {
        Utils.unlockOrientation(getActivity());
        playScrambleEnterAnimation();
        playStatsEnterAnimation();
        getActivityCallback().playToolbarEnterAnimation();
    }

    private void playDynamicStatusBarEnterAnimation() {
        mDynamicStatusBarVisible = true;
        ObjectAnimator enter = ObjectAnimator.ofFloat(mDynamicStatusBarFrame, View.TRANSLATION_Y,
                0f);
        enter.setDuration(125);
        enter.setInterpolator(new DecelerateInterpolator());
        AnimatorSet dynamicStatusBarAnimatorSet = new AnimatorSet();
        dynamicStatusBarAnimatorSet.play(enter);
        dynamicStatusBarAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mDynamicStatusBarVisible) {
                    mTimeBarRecycler.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        dynamicStatusBarAnimatorSet.start();
    }

    private void playDynamicStatusBarExitAnimation() {
        mDynamicStatusBarVisible = false;
        ObjectAnimator exit = ObjectAnimator.ofFloat(mDynamicStatusBarFrame, View.TRANSLATION_Y,
                mDynamicStatusBarFrame.getHeight());
        exit.setDuration(125);
        exit.setInterpolator(new AccelerateInterpolator());
        AnimatorSet dynamicStatusBarAnimatorSet = new AnimatorSet();
        dynamicStatusBarAnimatorSet.play(exit);
        dynamicStatusBarAnimatorSet.start();
        mTimeBarRecycler.setVisibility(View.VISIBLE);
    }

    private void playLastBarEnterAnimation() {
        if (mLastBarAnimationSet != null) {
            mLastBarAnimationSet.cancel();
        }
        mLastDeleteButton.setEnabled(true);
        mLastDnfButton.setEnabled(true);
        mLastPlusTwoButton.setEnabled(true);
        ObjectAnimator enter = ObjectAnimator.ofFloat(mLastBarLinearLayout, View.TRANSLATION_Y,
                -mLastBarLinearLayout.getHeight());
        ObjectAnimator exit = ObjectAnimator.ofFloat(mLastBarLinearLayout, View.TRANSLATION_Y, 0f);
        enter.setDuration(125);
        exit.setDuration(125);
        exit.setStartDelay(1500);
        enter.setInterpolator(new DecelerateInterpolator());
        exit.setInterpolator(new AccelerateInterpolator());
        mLastBarAnimationSet = new AnimatorSet();
        mLastBarAnimationSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                mLastBarLinearLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mLastBarLinearLayout.getTranslationY() == 0f) {
                    mLastBarLinearLayout.setVisibility(View.GONE);
                }
            }
        });
        mLastBarAnimationSet.playSequentially(enter, exit);
        mLastBarAnimationSet.start();
    }

    void playLastBarExitAnimation() {
        mLastDeleteButton.setEnabled(false);
        mLastDnfButton.setEnabled(false);
        mLastPlusTwoButton.setEnabled(false);
        ObjectAnimator exit = ObjectAnimator.ofFloat(mLastBarLinearLayout, View.TRANSLATION_Y, 0f);
        exit.setDuration(125);
        exit.setInterpolator(new AccelerateInterpolator());
        AnimatorSet lastBarAnimationSet = new AnimatorSet();
        lastBarAnimationSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (mLastBarLinearLayout.getTranslationY() == 0f) {
                    mLastBarLinearLayout.setVisibility(View.GONE);
                }
            }
        });
        lastBarAnimationSet.play(exit);
        lastBarAnimationSet.start();
    }

    void playScrambleExitAnimation() {
        if (mScrambleAnimator != null) {
            mScrambleAnimator.cancel();
        }
        mScrambleAnimator = ObjectAnimator.ofFloat(mScrambleText, View.TRANSLATION_Y,
                -mScrambleText.getHeight());
        mScrambleAnimator.setDuration(300);
        mScrambleAnimator.setInterpolator(new AccelerateInterpolator());
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            mScrambleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mScrambleTextShadow.setTranslationY((int) (float) animation.getAnimatedValue());
                }
            });
        }
        mScrambleAnimator.start();
        invalidateScrambleShadow(true);
    }

    void playScrambleEnterAnimation() {
        if (mScrambleAnimator != null) {
            mScrambleAnimator.cancel();
        }
        mScrambleAnimator = ObjectAnimator.ofFloat(mScrambleText, View.TRANSLATION_Y, 0f);
        mScrambleAnimator.setDuration(300);
        mScrambleAnimator.setInterpolator(new DecelerateInterpolator());
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            mScrambleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mScrambleTextShadow.setTranslationY((int) (float) animation.getAnimatedValue());
                }
            });
        }
        mScrambleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                invalidateScrambleShadow(false);
            }
        });
        mScrambleAnimator.start();
    }

    void playStatsExitAnimation() {
        ObjectAnimator exit = ObjectAnimator.ofFloat(mStatsText, View.ALPHA, 0f);
        exit.setDuration(300);
        ObjectAnimator exit3 = ObjectAnimator.ofFloat(mStatsSolvesText, View.ALPHA, 0f);
        exit3.setDuration(300);
        AnimatorSet scrambleAnimatorSet = new AnimatorSet();
        scrambleAnimatorSet.play(exit).with(exit3);
        scrambleAnimatorSet.start();
    }

    void playStatsEnterAnimation() {
        ObjectAnimator enter = ObjectAnimator.ofFloat(mStatsText, View.ALPHA, 1f);
        enter.setDuration(300);
        ObjectAnimator enter3 = ObjectAnimator.ofFloat(mStatsSolvesText, View.ALPHA, 1f);
        enter3.setDuration(300);
        AnimatorSet scrambleAnimatorSet = new AnimatorSet();
        scrambleAnimatorSet.play(enter).with(enter3);
        scrambleAnimatorSet.start();
    }

    void startHoldTimer() {
        playLastBarExitAnimation();
        mHoldTiming = true;
        mHoldTimerStartTimestamp = System.nanoTime();
        setTextColor(Color.RED);
        mUiHandler.postDelayed(holdTimerRunnable, 550);
    }

    public void stopHoldTimer() {
        mHoldTiming = false;
        mHoldTimerStartTimestamp = 0;
        mUiHandler.removeCallbacks(holdTimerRunnable);
        setTextColorPrimary();
    }

    /**
     * Start inspection; Start Generating Next Scramble
     */
    void startInspection() {
        playLastBarExitAnimation();
        playDynamicStatusBarEnterAnimation();
        mDynamicStatusBarText.setText(R.string.inspecting);
        mInspectionStartTimestamp = System.nanoTime();
        mInspecting = true;
        if (mBldMode) {
            mUiHandler.post(timerRunnable);
        } else {
            mUiHandler.post(inspectionRunnable);
        }
        mRetainedFragment.generateNextScramble();
        enableMenuItems(false);
        showScrambleImage(false);
        getActivityCallback().lockDrawerAndViewPager(true);
        setTextColorPrimary();
    }

    void stopInspection() {
        mInspectionStopTimestamp = System.nanoTime();
        mInspecting = false;
        mUiHandler.removeCallbacks(inspectionRunnable);
    }

    /**
     * Start timing; does not start generating next scramble
     */
    void startTiming() {
        playLastBarExitAnimation();
        playDynamicStatusBarEnterAnimation();
        mTimingStartTimestamp = System.nanoTime();
        mInspecting = false;
        mTiming = true;
        if (!mBldMode) mUiHandler.post(timerRunnable);
        enableMenuItems(false);
        showScrambleImage(false);
        mDynamicStatusBarText.setText(R.string.timing);
        getActivityCallback().lockDrawerAndViewPager(true);
        setTextColorPrimary();
    }

    void resetGenerateScramble() {
        mRetainedFragment.resetScramblerThread();
        mRetainedFragment.generateNextScramble();
        mRetainedFragment.postSetScrambleViewsToCurrent();
    }

    void resetTimer() {
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

    public Handler getUiHandler() {
        return mUiHandler;
    }

    public enum Update {
        INSERT, REMOVE, REMOVE_ALL, SINGLE_CHANGE, DATA_RESET
    }

    public interface ActivityCallback {
        void lockDrawerAndViewPager(boolean lock);

        void playToolbarEnterAnimation();

        void playToolbarExitAnimation();

        CurrentSessionTimerRetainedFragment getTimerRetainedFragment();

        void enableMenuItems(boolean enable);

        FrameLayout getContentFrameLayout();
    }

    public class SolveRecyclerAdapter
            extends RecyclerView.Adapter<SolveRecyclerAdapter.ViewHolder> {

        private List<Solve> mSolves;
        private Solve[] mBestAndWorstSolves;

        public SolveRecyclerAdapter() {
            mBestAndWorstSolves = new Solve[2];
            updateSolvesList(-1, Update.DATA_RESET);
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

        private void updateSolvesList(int position, Update mode) {
            int oldSize = 0;
            if (mSolves != null) oldSize = mSolves.size();
            mSolves = PuzzleType.getCurrent().getSession(PuzzleType
                    .CURRENT_SESSION).getSolves();
            for (Solve s : mBestAndWorstSolves) {
                notifyItemChanged(mSolves.indexOf(s));
            }
            mBestAndWorstSolves[0] = Utils.getBestSolveOfList(mSolves);
            mBestAndWorstSolves[1] = Utils.getWorstSolveOfList(mSolves);
            for (Solve s : mBestAndWorstSolves) {
                notifyItemChanged(mSolves.indexOf(s));
            }

            switch (mode) {
                case DATA_RESET:
                    notifyDataSetChanged();
                    if (mSolves.size() >= 1)
                        mTimeBarRecycler.smoothScrollToPosition(mSolves.size() - 1);
                    break;
                case INSERT:
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
                case REMOVE_ALL:
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
                        SolveDialogUtils.createSolveDialog(getActivity(),
                                false,
                                PuzzleType.getCurrent().name(),
                                PuzzleType.CURRENT_SESSION,
                                getLayoutPosition()
                        );
                    }
                });
            }
        }


    }


}