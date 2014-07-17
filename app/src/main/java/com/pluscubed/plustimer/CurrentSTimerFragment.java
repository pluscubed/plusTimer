package com.pluscubed.plustimer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import it.sephiroth.android.library.widget.HListView;

/**
 * TimerFragment
 */

public class CurrentSTimerFragment extends CurrentSBaseFragment implements CurrentSTimerFragmentCallback {
    public static final String TAG = "TIMER";

    private static final String STATE_IMAGE_DISPLAYED = "scramble_image_displayed_boolean";
    private static final String STATE_START_TIME = "start_time_long";
    private static final String STATE_RUNNING = "running_boolean";

    private RetainedFragmentCallback mRetainedFragment;

    private TextView mTimerText;
    private TextView mScrambleText;
    private HListView mHListView;
    private ImageView mScrambleImage;
    private TextView mQuickStatsSolves;
    private TextView mQuickStats;
    private long mStartTime;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            mTimerText.setText(Solve.timeStringFromLong(System.nanoTime() - mStartTime));
            mUiHandler.postDelayed(this, 10);
        }
    };
    private long mEndTime;
    private long mFinalTime;

    private boolean mFromSavedInstanceState;

    private boolean mRunning;

    private boolean mScrambleImageDisplay;

    private Handler mUiHandler;

    //Generate string with specified current averages and mean of current session
    public static String buildStatsWithAveragesOf(Context context, Integer... currentAverages) {
        Arrays.sort(currentAverages, Collections.reverseOrder());
        String s = "";
        for (int i : currentAverages) {
            if (PuzzleType.sCurrentPuzzleType.getCurrentSession().getNumberOfSolves() >= i) {
                s += context.getString(R.string.ao) + i + ": " + PuzzleType.sCurrentPuzzleType.getCurrentSession().getStringCurrentAverageOf(i) + "\n";
            }
        }
        if (PuzzleType.sCurrentPuzzleType.getCurrentSession().getNumberOfSolves() > 0) {
            s += context.getString(R.string.mean) + PuzzleType.sCurrentPuzzleType.getCurrentSession().getStringMean();
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

    public static CurrentSTimerFragment newInstance(SavedState savedState) {
        CurrentSTimerFragment fragment = new CurrentSTimerFragment();
        fragment.setInitialSavedState(savedState);
        return fragment;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IMAGE_DISPLAYED, mScrambleImageDisplay);
        outState.putLong(STATE_START_TIME, mStartTime);
        outState.putBoolean(STATE_RUNNING, mRunning);
    }

    //Set scramble text and scramble image to current ones
    @Override
    public void updateScrambleTextAndImageToCurrent() {
        SVG svg = null;
        try {
            svg = SVG.getFromString(mRetainedFragment.getCurrentScrambleAndSvg().svgLite.toString());
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

        if (savedInstanceState != null) {
            mScrambleImageDisplay = savedInstanceState.getBoolean(STATE_IMAGE_DISPLAYED);
            mStartTime = savedInstanceState.getLong(STATE_START_TIME);
            mRunning = savedInstanceState.getBoolean(STATE_RUNNING);
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

    @Override
    public void onSessionSolvesChanged() {
        //Update stats
        mQuickStatsSolves.setText(getString(R.string.solves) + PuzzleType.sCurrentPuzzleType.getCurrentSession().getNumberOfSolves());
        mQuickStats.setText(buildStatsWithAveragesOf(getAttachedActivity(), 5, 12, 100));
        //Update HListView
        ((SolveHListViewAdapter) mHListView.getAdapter()).updateSolvesList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_current_s_timer, menu);

        //setUpPuzzleSpinner (CurrentSBaseFragment)
        setUpPuzzleSpinner(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Toggle image button
            case R.id.menu_current_s_toggle_scramble_image_action:
                toggleScrambleImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private RetainedFragmentCallback getRetainedFragment() {
        GetRetainedFragmentCallback callback;
        try {
            callback = (GetRetainedFragmentCallback) getAttachedActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getAttachedActivity().toString()
                    + " must implement GetRetainedFragmentCallback");
        }
        RetainedFragmentCallback fragment;
        try {
            fragment = (RetainedFragmentCallback) callback.getCurrentSTimerRetainedFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(callback.getCurrentSTimerRetainedFragment().toString()
                    + " must implement RetainedFragmentCallback");
        }
        return fragment;
    }

    //Called when the session is changed to another one (action bar spinner)
    @Override
    public void onSessionChanged() {
        //Update quick stats and hlistview
        onSessionSolvesChanged();

        //Set timer text to ready, scramble text to scrambling
        mScrambleText.setText(R.string.scrambling);
        mTimerText.setText(R.string.ready);

        //Update options menu (disable)
        enableOptionsMenu(false);

        //Hide scramble image
        mScrambleImage.setVisibility(View.GONE);
        mScrambleImageDisplay = false;

        mRetainedFragment.generateNextScramble();
        mRetainedFragment.updateViews();

    }

    @Override
    public void enableOptionsMenu(boolean enable) {
        //Get the parent fragment and enable/disable the options menu depending if the app is initializing, scrambling, or running
        MenuItemsEnableListener listener;
        try {
            listener = (MenuItemsEnableListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getParentFragment().toString()
                    + " must implement MenuItemsEnableListener");
        }
        listener.menuItemsEnable(enable);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_s_timer, container, false);

        mTimerText = (TextView) v.findViewById(R.id.fragment_current_s_timer_time_textview);
        mScrambleText = (TextView) v.findViewById(R.id.fragment_current_s_timer_scramble_textview);
        mScrambleImage = (ImageView) v.findViewById(R.id.fragment_current_s_timer_scramble_imageview);
        mHListView = (HListView) v.findViewById(R.id.fragment_current_s_timer_bottom_hlistview);

        mQuickStats = (TextView) v.findViewById(R.id.fragment_current_s_timer_quickstats_textview);
        mQuickStatsSolves = (TextView) v.findViewById(R.id.fragment_current_s_timer_quickstats_solves_number_textview);

        final SolveHListViewAdapter adapter = new SolveHListViewAdapter();
        mHListView.setAdapter(adapter);
        mHListView.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id) {
                onSolveItemClick(position);
            }
        });

        //When the timer text is clicked (start when released)...
        mTimerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //If timer is not running and it is not scrambling...
                if (!mRunning && !mRetainedFragment.isScrambling()) {
                    //Set flag to keep screen on
                    getAttachedActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    //Set the start time, set flag to true, and start the timer runnable
                    mStartTime = System.nanoTime();
                    mRunning = true;
                    mUiHandler.post(timerRunnable);
                    //Update the options menu (disable)
                    enableOptionsMenu(false);
                    //Set the scramble image to gone
                    mScrambleImage.setVisibility(View.GONE);
                    mScrambleImageDisplay = false;
                    //Post to scrambler thread: generate the next scramble
                    mRetainedFragment.generateNextScramble();
                }

            }
        });

        //When the timer text is touched...
        mTimerText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //If the timer is running....
                if (mRunning) {
                    //Stop keeping the screen on
                    getAttachedActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    //Record the ending time, set flag to false, and stop the timer runnable
                    mEndTime = System.nanoTime();
                    mRunning = false;
                    mUiHandler.removeCallbacksAndMessages(null);

                    //Set the timer text to total time
                    mFinalTime = mEndTime - mStartTime;
                    mTimerText.setText(Solve.timeStringFromLong(mFinalTime));

                    //Add the solve to the current session with the current scramble/scramble image and time
                    PuzzleType.sCurrentPuzzleType.getCurrentSession().addSolve(new Solve(mRetainedFragment.getCurrentScrambleAndSvg(), mFinalTime));

                    //Update stats and HListView
                    onSessionSolvesChanged();

                    if (getRetainedFragment().isScrambling()) {
                        mScrambleText.setText(R.string.scrambling);
                    }

                    mRetainedFragment.updateViews();
                    return true;
                } else {
                    return false;
                }

            }
        });

        //When the application is initializing, disable action bar and generate a scramble.

        if (!mFromSavedInstanceState) {
            mRetainedFragment.resetScramblerThread();
            enableOptionsMenu(false);
            mScrambleText.setText(R.string.scrambling);
            mRetainedFragment.generateNextScramble();
            mRetainedFragment.updateViews();
        } else {
            if (mRunning) {
                mUiHandler.post(timerRunnable);
            }
            if (mRunning || !getRetainedFragment().isScrambling()) {
                // If timer is running, then update text/image to current. If timer is not running and not scrambling, then update scramble views to current.
                updateScrambleTextAndImageToCurrent();
                if (!getRetainedFragment().isScrambling()) {
                    enableOptionsMenu(true);
                }
            } else {
                mScrambleText.setText(R.string.scrambling);
            }
        }


        //If the current session has solves recorded, set the timer text to the latest solve's time.
        if (PuzzleType.sCurrentPuzzleType.getCurrentSession().getNumberOfSolves() != 0) {
            mTimerText.setText(PuzzleType.sCurrentPuzzleType.getCurrentSession().getLastSolve().getTimeString());
        }

        //If the scramble image is currently displayed and it is not scrambling, then make sure it is set to visible and that the OnClickListener is set to toggling it; otherwise, set to gone.
        if (mScrambleImageDisplay) {
            if (!getRetainedFragment().isScrambling()) {
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

    @Override
    public Handler getUiHandler() {
        return mUiHandler;
    }


    public interface GetRetainedFragmentCallback {
        Fragment getCurrentSTimerRetainedFragment();
    }


    public interface RetainedFragmentCallback {

        ScrambleAndSvg getCurrentScrambleAndSvg();

        boolean isScrambling();

        void generateNextScramble();

        void setTimerFragmentCallback(CurrentSTimerFragmentCallback fragment);

        void updateViews();

        void resetScramblerThread();

    }

    public interface MenuItemsEnableListener {
        void menuItemsEnable(boolean enable);
    }

    public class SolveHListViewAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveHListViewAdapter() {
            super(getAttachedActivity(), 0, PuzzleType.sCurrentPuzzleType.getCurrentSession().getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getCurrentSession().getBestSolve(PuzzleType.sCurrentPuzzleType.getCurrentSession().getSolves()));
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getCurrentSession().getWorstSolve(PuzzleType.sCurrentPuzzleType.getCurrentSession().getSolves()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getAttachedActivity().getLayoutInflater().inflate(R.layout.hlist_item_solve, parent, false);
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
                addAll(PuzzleType.sCurrentPuzzleType.getCurrentSession().getSolves());
            else {
                for (Solve i : PuzzleType.sCurrentPuzzleType.getCurrentSession().getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getCurrentSession().getBestSolve(PuzzleType.sCurrentPuzzleType.getCurrentSession().getSolves()));
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getCurrentSession().getWorstSolve(PuzzleType.sCurrentPuzzleType.getCurrentSession().getSolves()));
            notifyDataSetChanged();
        }


    }


}