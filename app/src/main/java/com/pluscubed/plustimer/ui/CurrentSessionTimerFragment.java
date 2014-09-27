package com.pluscubed.plustimer.ui;

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
import android.widget.ImageView;
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

public class CurrentSessionTimerFragment extends Fragment implements CurrentSessionTimerRetainedFragment.Callback {
    public static final String TAG = "CURRENT_SESSION_TIMER_FRAGMENT";

    private static final String STATE_IMAGE_DISPLAYED = "scramble_image_displayed_boolean";
    private static final String STATE_START_TIME = "start_time_long";
    private static final String STATE_RUNNING = "running_boolean";
    private static final String STATE_INSPECTING = "inspecting_boolean";
    private static final String STATE_INSPECTION_START_TIME = "inspection_start_time_long";

    private boolean mHoldToStartOn;
    private boolean mInspectionOn;
    private boolean mTwoRowTimeOn;
    private int mUpdateTime;

    private CurrentSessionTimerRetainedFragment mRetainedFragment;

    private TextView mTimerText;
    private TextView mTimerText2;
    private TextView mScrambleText;
    private HListView mHListView;
    private ImageView mScrambleImage;
    private TextView mStatsSolvesText;
    private TextView mStatsText;

    private Handler mUiHandler;

    private boolean mHoldTimerStarted;
    private long mHoldTimerStartTimestamp;
    private final Runnable mHoldTimerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerTextSize();
            if (550000000L <= System.nanoTime() - mHoldTimerStartTimestamp) {
                mTimerText.setTextColor(Color.GREEN);
            } else {
                mUiHandler.postDelayed(this, 10);
            }
        }
    };

    private boolean mInspecting;
    private long mInspectionStartTimestamp;
    private long mStartTimestamp;
    private final Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mUpdateTime != 2) {
                updateTimerTextSize();
                setTimerText(Util.timeStringsSplitByDecimal(System.nanoTime() - mStartTimestamp));
                mUiHandler.postDelayed(this, 10);
            } else {
                setTimerText(new String[]{getString(R.string.timing), ""});
                mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100);
            }
        }
    };


    private long mEndTimestamp;
    private long mFinalTime;
    private boolean mFromSavedInstanceState;
    private boolean mRunning;
    private boolean mScrambleImageDisplay;
    private boolean mLateStartPenalty;
    private final Runnable mInspectionRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerTextSize();
            String[] array = Util.timeStringsSplitByDecimal(16000000000L - (System.nanoTime() - mInspectionStartTimestamp));
            array[1] = "";
            setTimerText(array);
            if (15000000000L - (System.nanoTime() - mInspectionStartTimestamp) <= 0) {
                mLateStartPenalty = true;
                setTimerText(new String[]{"+2", ""});
                if (17000000000L - (System.nanoTime() - mInspectionStartTimestamp) <= 0) {
                    mLateStartPenalty = false;
                    mInspecting = false;
                    mHoldTimerStarted = false;
                    //stop the runnables
                    mUiHandler.removeCallbacksAndMessages(null);

                    Solve s = new Solve(mRetainedFragment.getCurrentScrambleAndSvg(), 0);
                    s.setPenalty(Solve.Penalty.DNF);

                    //Add the solve to the current session with the current scramble/scramble image and DNF
                    PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).addSolve(s);
                    updateTimerTextToLastSolveTime();

                    //Update stats and HListView
                    onSessionSolvesChanged();

                    if (mRetainedFragment.isScrambling()) {
                        mScrambleText.setText(R.string.scrambling);
                    }
                    mTimerText.setTextColor(Color.BLACK);
                    mRetainedFragment.updateViews();
                    return;
                }
            }
            mUiHandler.postDelayed(this, 10);
        }
    };
    private SharedPreferences mDefaultSharedPreferences;

    //Generate string with specified current averages and mean of current session
    private String buildStatsWithAveragesOf(Context context, Integer... currentAverages) {
        Arrays.sort(currentAverages, Collections.reverseOrder());
        String s = "";
        for (int i : currentAverages) {
            if (PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getNumberOfSolves() >= i) {
                s += String.format(context.getString(R.string.cao), i) + ": " + PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getStringCurrentAverageOf(i) + "\n";
            }
        }
        if (PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getNumberOfSolves() > 0) {
            s += context.getString(R.string.mean) + PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getStringMean();
        }
        return s;
    }


    /* Might be useful later
     public static double convertPxToDp(double px, DisplayMetrics metrics){
     double dp = px / (metrics.xdpi / (double)DisplayMetrics.DENSITY_DEFAULT);
     return dp;
     }

     public static double convertDpToPx(double dp, DisplayMetrics metrics){

     double px = dp * (metrics.xdpi /(double)DisplayMetrics.DENSITY_DEFAULT);
     return px;

     }
     */


    public void setTimerText(String[] array) {
        if (mTwoRowTimeOn) {
            mTimerText.setText(array[0]);
            mTimerText2.setText(array[1]);
            if (array[1].equals("") || (mRunning && (mUpdateTime != 0))) {
                mTimerText2.setVisibility(View.GONE);
            } else {
                mTimerText2.setVisibility(View.VISIBLE);
            }
        } else {
            mTimerText2.setVisibility(View.GONE);
            mTimerText.setText(array[0]);
            if (!array[1].equals("") && !(mRunning && (mUpdateTime != 0))) {
                mTimerText.append("." + array[1]);
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IMAGE_DISPLAYED, mScrambleImageDisplay);
        outState.putLong(STATE_START_TIME, mStartTimestamp);
        outState.putBoolean(STATE_RUNNING, mRunning);
        outState.putBoolean(STATE_INSPECTING, mInspecting);
        outState.putLong(STATE_INSPECTION_START_TIME, mInspectionStartTimestamp);
    }

    //Set scramble text and scramble image to current ones
    @Override
    public void updateScrambleTextAndImageToCurrent() {
        SVG svg = null;
        try {
            svg = SVG.getFromString(mRetainedFragment.getCurrentScrambleAndSvg().svg);
        } catch (SVGParseException e) {
            e.printStackTrace();
        }
        Drawable drawable = new PictureDrawable(svg.renderToPicture());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mScrambleImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mScrambleImage.setImageDrawable(drawable);

        mScrambleText.setText(mRetainedFragment.getCurrentScrambleAndSvg().scramble);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mRetainedFragment = getRetainedFragment();
        mRetainedFragment.setTimerFragmentCallback(this);

        //Set up UIHandler
        mUiHandler = new Handler(Looper.getMainLooper());

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        initSharedPrefs();

        if (savedInstanceState != null) {
            mScrambleImageDisplay = savedInstanceState.getBoolean(STATE_IMAGE_DISPLAYED);
            mStartTimestamp = savedInstanceState.getLong(STATE_START_TIME);
            mRunning = savedInstanceState.getBoolean(STATE_RUNNING);
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
        mRetainedFragment.setTimerFragmentCallback(null);
    }


    public void onSessionSolvesChanged() {
        //Update stats
        mStatsSolvesText.setText(getString(R.string.solves) + PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getNumberOfSolves());
        mStatsText.setText(buildStatsWithAveragesOf(getActivity(), 5, 12, 100));
        //Update HListView
        ((SolveHListViewAdapter) mHListView.getAdapter()).updateSolvesList();
        updateTimerTextToLastSolveTime();
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
        SharedPreferences sharedPreferences = initSharedPrefs();
        if (sharedPreferences.getBoolean(SettingsActivity.PREF_KEEPSCREENON_CHECKBOX, true)) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public SharedPreferences initSharedPrefs() {
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mInspectionOn = mDefaultSharedPreferences.getBoolean(SettingsActivity.PREF_INSPECTION_CHECKBOX, true);
        mHoldToStartOn = mDefaultSharedPreferences.getBoolean(SettingsActivity.PREF_HOLDTOSTART_CHECKBOX, true);
        mTwoRowTimeOn = getResources().getConfiguration().orientation == 1 && mDefaultSharedPreferences.getBoolean(SettingsActivity.PREF_TWO_ROW_TIME_CHECKBOX, true);
        mUpdateTime = Integer.parseInt(mDefaultSharedPreferences.getString(SettingsActivity.PREF_UPDATE_TIME_LIST, "0"));
        return mDefaultSharedPreferences;
    }

    public void updateTimerTextSize() {
        if (mTimerText.getText() != getString(R.string.ready)) {
            if (mTimerText != null && mTimerText2 != null) {
                int size = Integer.parseInt(mDefaultSharedPreferences.getString(SettingsActivity.PREF_TIME_TEXT_SIZE_EDITTEXT, "100"));
                if (mTwoRowTimeOn) {
                    mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
                } else {
                    mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * 0.7F);
                }
                mTimerText2.setTextSize(TypedValue.COMPLEX_UNIT_SP, size / 2);
            }
        }
    }

    public void stopHoldTimer() {
        mUiHandler.removeCallbacks(mHoldTimerRunnable);
        mTimerText.setTextColor(Color.BLACK);

    }

    private CurrentSessionTimerRetainedFragment getRetainedFragment() {
        GetRetainedFragmentCallback callback;
        try {
            callback = (GetRetainedFragmentCallback) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString()
                    + " must implement GetRetainedFragmentCallback");
        }
        return callback.getCurrentSessionTimerRetainedFragment();
    }

    //Called when the session is changed to another one (action bar spinner)
    public void onSessionChanged() {
        //Update quick stats and hlistview
        onSessionSolvesChanged();

        //Set timer text to ready, scramble text to scrambling
        mScrambleText.setText(R.string.scrambling);

        //Update options menu (disable)
        enableMenuItems(false);

        //Hide scramble image
        mScrambleImage.setVisibility(View.GONE);
        mScrambleImageDisplay = false;

        mRetainedFragment.resetScramblerThread();
        mRetainedFragment.generateNextScramble();
        mRetainedFragment.updateViews();

        resetTimer();
    }

    public void resetTimer() {
        mUiHandler.removeCallbacksAndMessages(null);
        mHoldTimerStarted = false;
        mRunning = false;
        mLateStartPenalty = false;
        mHoldTimerStartTimestamp = 0;
        mInspectionStartTimestamp = 0;
        mStartTimestamp = 0;
        mEndTimestamp = 0;
        mFinalTime = 0;
        mInspecting = false;

        mTimerText.setTextColor(Color.BLACK);
    }

    @Override
    public void enableMenuItems(boolean enable) {
        MenuItemsEnableCallback callback;
        try {
            callback = (MenuItemsEnableCallback) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement MenuItemsCallback");
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_session_timer, container, false);

        mTimerText = (TextView) v.findViewById(R.id.fragment_current_session_timer_time_textview);
        mTimerText2 = (TextView) v.findViewById(R.id.fragment_current_session_timer_timeSecondary_textview);
        mScrambleText = (TextView) v.findViewById(R.id.fragment_current_session_timer_scramble_textview);
        mScrambleImage = (ImageView) v.findViewById(R.id.fragment_current_session_timer_scramble_imageview);
        mHListView = (HListView) v.findViewById(R.id.fragment_current_session_timer_bottom_hlistview);

        mStatsText = (TextView) v.findViewById(R.id.fragment_current_session_timer_stats_textview);
        mStatsSolvesText = (TextView) v.findViewById(R.id.fragment_current_session_timer_stats_solves_number_textview);

        final SolveHListViewAdapter adapter = new SolveHListViewAdapter();
        mHListView.setAdapter(adapter);
        mHListView.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id) {
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
                        if (mRunning) {
                            //Record the ending time, set flag to false, and stop the timer runnable
                            mEndTimestamp = System.nanoTime();
                            mRunning = false;
                            mUiHandler.removeCallbacksAndMessages(null);

                            //Set the timer text to total time
                            mFinalTime = mEndTimestamp - mStartTimestamp;

                            Solve s = new Solve(mRetainedFragment.getCurrentScrambleAndSvg(), mFinalTime);
                            if (mInspectionOn && mLateStartPenalty) {
                                s.setPenalty(Solve.Penalty.PLUSTWO);
                                mLateStartPenalty = false;
                            }
                            //Add the solve to the current session with the current scramble/scramble image and time
                            PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).addSolve(s);

                            updateTimerTextToLastSolveTime();

                            //Update stats and HListView
                            onSessionSolvesChanged();

                            if (mRetainedFragment.isScrambling()) {
                                mScrambleText.setText(R.string.scrambling);
                            }

                            mRetainedFragment.updateViews();
                            return false;
                        } else if (mInspectionOn && !mInspecting && !mRetainedFragment.isScrambling()) {
                            return true;
                        } else if (mInspecting || (mHoldToStartOn && !mRetainedFragment.isScrambling())) {
                            mHoldTimerStarted = true;
                            mHoldTimerStartTimestamp = System.nanoTime();
                            mTimerText.setTextColor(Color.RED);
                            mUiHandler.postDelayed(mHoldTimerRunnable, 450);
                            return true;
                        } else if (!mInspectionOn && !mHoldToStartOn && !mRetainedFragment.isScrambling()) {
                            return true;
                        }
                        return false;
                    }

                    case MotionEvent.ACTION_UP: {
                        if (mInspectionOn && !mInspecting && !mRunning) {
                            mInspectionStartTimestamp = System.nanoTime();
                            mInspecting = true;
                            mUiHandler.post(mInspectionRunnable);
                            enableMenuItems(false);
                            //Set the scramble image to gone
                            mScrambleImage.setVisibility(View.GONE);
                            mScrambleImageDisplay = false;
                            //Post to scrambler thread: generate the next scramble
                            mRetainedFragment.generateNextScramble();
                        } else if (((mInspectionOn && mInspecting) || mHoldToStartOn) && !mRunning) {
                            if (mHoldTimerStarted && 500000000L <= System.nanoTime() - mHoldTimerStartTimestamp) {
                                mStartTimestamp = System.nanoTime();
                                mInspecting = false;
                                mRunning = true;
                                mUiHandler.removeCallbacksAndMessages(null);
                                mUiHandler.post(mTimerRunnable);
                                if (mHoldToStartOn) {
                                    enableMenuItems(false);
                                    //Set the scramble image to gone
                                    mScrambleImage.setVisibility(View.GONE);
                                    mScrambleImageDisplay = false;
                                    //Post to scrambler thread: generate the next scramble
                                    mRetainedFragment.generateNextScramble();
                                }
                            } else {
                                mHoldTimerStarted = false;
                                mUiHandler.removeCallbacks(mHoldTimerRunnable);
                            }
                            mTimerText.setTextColor(Color.BLACK);
                        } else if (!mInspectionOn && !mHoldToStartOn && !mRunning) {
                            enableMenuItems(false);
                            //Set the scramble image to gone
                            mScrambleImage.setVisibility(View.GONE);
                            mScrambleImageDisplay = false;
                            //Post to scrambler thread: generate the next scramble
                            mRetainedFragment.generateNextScramble();
                            mStartTimestamp = System.nanoTime();
                            mRunning = true;
                            mUiHandler.removeCallbacksAndMessages(null);
                            mUiHandler.post(mTimerRunnable);
                        }
                        return false;
                    }

                    default:
                        return false;
                }
            }
        });

        //When the application is initializing, disable action bar and generate a scramble.

        if (!mFromSavedInstanceState) {
            mRetainedFragment.resetScramblerThread();
            enableMenuItems(false);
            mScrambleText.setText(R.string.scrambling);
            mRetainedFragment.generateNextScramble();
            mRetainedFragment.updateViews();
        } else {
            if (mInspecting) {
                mUiHandler.post(mInspectionRunnable);
            }
            if (mRunning) {
                mUiHandler.post(mTimerRunnable);
            }
            if (mRunning || mInspecting) {
                enableMenuItems(false);
            } else if (!mRetainedFragment.isScrambling()) {
                enableMenuItems(true);
            }
            if (mInspecting || mRunning || !mRetainedFragment.isScrambling()) {
                // If timer is running, then update text/image to current. If timer is not running and not scrambling, then update scramble views to current.
                updateScrambleTextAndImageToCurrent();
            } else {
                mScrambleText.setText(R.string.scrambling);
            }
        }

        //If the scramble image is currently displayed and it is not scrambling, then make sure it is set to visible and that the OnClickListener is set to toggling it; otherwise, set to gone.
        if (mScrambleImageDisplay) {
            if (!mRetainedFragment.isScrambling()) {
                mScrambleImage.setVisibility(View.VISIBLE);
                mScrambleImageDisplay = true;
                mScrambleImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleScrambleImage();
                    }
                });
            }
        } else {
            mScrambleImage.setVisibility(View.GONE);
            mScrambleImageDisplay = false;
        }

        onSessionSolvesChanged();

        return v;
    }

    /**
     * Updates the timer text to last solve's time; if there are no solves, set to ready. Updates the timer text's size.
     */
    public void updateTimerTextToLastSolveTime() {
        if (PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getNumberOfSolves() != 0) {
            setTimerText(PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getLastSolve().getTimeStringArray());
        } else {
            setTimerText(new String[]{getString(R.string.ready), ""});
            mTimerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100);
        }
        updateTimerTextSize();
    }

    @Override
    public Handler getUiHandler() {
        return mUiHandler;
    }


    public interface GetRetainedFragmentCallback {
        CurrentSessionTimerRetainedFragment getCurrentSessionTimerRetainedFragment();
    }

    public interface MenuItemsEnableCallback {
        void enableMenuItems(boolean enable);
    }

    public class SolveHListViewAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveHListViewAdapter() {
            super(getActivity(), 0, PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Util.getBestSolveOfList(PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getSolves()));
            mBestAndWorstSolves.add(Util.getWorstSolveOfList(PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getSolves()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.hlist_item_solve, parent, false);
            }
            Solve s = getItem(position);
            TextView time = (TextView) convertView.findViewById(R.id.hlist_item_solve_textview);

            time.setText("");

            for (Solve a : mBestAndWorstSolves) {
                if (a == s) {
                    time.setText("(" + s.getTimeString() + ")");
                }
            }

            if (time.getText() == "") {
                time.setText(s.getTimeString());
            }

            return convertView;
        }

        public void updateSolvesList() {
            clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                addAll(PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getSolves());
            else {
                for (Solve i : PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Util.getBestSolveOfList(PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getSolves()));
            mBestAndWorstSolves.add(Util.getWorstSolveOfList(PuzzleType.get(PuzzleType.CURRENT).getSession(PuzzleType.CURRENT_SESSION).getSolves()));
            notifyDataSetChanged();
        }


    }


}