package com.pluscubed.plustimer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import it.sephiroth.android.library.widget.HListView;

/**
 * TimerFragment
 */

public class CurrentSTimerFragment extends Fragment {
    public static final String TAG = "TIMER";

    public static final String EXTRA_DIALOG_FINISH_SOLVE_INDEX = "com.pluscubed.plustimer.EXTRA_DIALOG_FINISH_SOLVE_INDEX";
    public static final String EXTRA_DIALOG_FINISH_SELECTION = "com.pluscubed.plustimer.EXTRA_DIALOG_FINISH_SELECTION";

    public static final String DIALOG_FRAGMENT_TAG = "MODIFY_DIALOG";

    public static final int DIALOG_REQUEST_CODE = 0;

    public static final int DIALOG_PENALTY_NONE = 0;
    public static final int DIALOG_PENALTY_PLUSTWO = 1;
    public static final int DIALOG_PENALTY_DNF = 2;
    public static final int DIALOG_RESULT_DELETE = 3;

    private TextView mTimerText;
    private TextView mScrambleText;
    private HListView mHListView;
    private ImageView mScrambleImage;
    private TextView mQuickStatsSolves;
    private TextView mQuickStats;

    private long mStartTime;
    private long mEndTime;
    private long mFinalTime;

    private Runnable mTimerRunnable;

    private boolean mOnCreateCalled;
    private boolean mScrambling;
    private boolean mRunning;
    private boolean mScrambleImageDisplay;

    private Handler mScramblerThreadHandler;
    private Handler mUIHandler;

    private ActionBarActivity mActivity;

    private ScrambleAndSvg mCurrentScrambleAndSvg;
    private ScrambleAndSvg mNextScrambleAndSvg;


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

    public static String buildQuickStatsWithAveragesOf(Context context, Integer... currentAverages) {
        Arrays.sort(currentAverages, Collections.reverseOrder());
        String s = "";
        for (int i : currentAverages) {
            if (PuzzleType.sCurrentPuzzleType.getSession().getNumberOfSolves() >= i) {
                s += context.getString(R.string.ao) + i + ": " + PuzzleType.sCurrentPuzzleType.getSession().getStringCurrentAverageOf(i) + "\n";
            }
        }
        if (PuzzleType.sCurrentPuzzleType.getSession().getNumberOfSolves() > 0) {
            s += context.getString(R.string.mean) + PuzzleType.sCurrentPuzzleType.getSession().getStringMean();
        }
        return s;
    }

    void updateQuickStats() {
        mQuickStatsSolves.setText(getString(R.string.solves) + PuzzleType.sCurrentPuzzleType.getSession().getNumberOfSolves());
        mQuickStats.setText(buildQuickStatsWithAveragesOf(mActivity, 5, 12, 100));
    }


    void updateScrambleViewsToCurrent() {
        SVG svg = null;
        try {
            svg = SVG.getFromString(mCurrentScrambleAndSvg.svgLite.toString());
        } catch (SVGParseException e) {
            e.printStackTrace();
        }
        Drawable drawable = new PictureDrawable(svg.renderToPicture());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            mScrambleImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mScrambleImage.setImageDrawable(drawable);

        mScrambleText.setText(mCurrentScrambleAndSvg.scramble);
    }

    ScrambleAndSvg generateScramble() {
        mScrambling = true;
        String scramble = PuzzleType.sCurrentPuzzleType.getPuzzle().generateScramble();
        ScrambleAndSvg scrambleAndSvg = null;
        try {
            scrambleAndSvg = new ScrambleAndSvg(scramble, PuzzleType.sCurrentPuzzleType.getPuzzle().drawScramble(scramble, null));
        } catch (InvalidScrambleException e) {
            e.printStackTrace();
        }
        mScrambling = false;

        return scrambleAndSvg;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (ActionBarActivity) getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        HandlerThread scramblerThread = new HandlerThread("ScramblerThread");
        scramblerThread.start();
        mScramblerThreadHandler = new Handler(scramblerThread.getLooper());
        mUIHandler = new Handler(Looper.getMainLooper());

        mOnCreateCalled = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mScramblerThreadHandler.removeCallbacksAndMessages(null);
        mScramblerThreadHandler.getLooper().quit();
        mUIHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mOnCreateCalled = false;
    }

    public void updateSession() {
        updateQuickStats();
        ((SolveHListViewAdapter) mHListView.getAdapter()).updateSolvesList();
    }

    public void onPuzzleTypeChanged() {
        ((CurrentSFragment) getParentFragment()).updateFragments();

        mScrambleText.setText(R.string.scrambling);
        mTimerText.setText(R.string.ready);
        ((CurrentSFragment) getParentFragment()).menuItemsEnable(false);
        mScrambleImage.setVisibility(View.GONE);
        mScrambleImageDisplay = false;

        mScramblerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mCurrentScrambleAndSvg = generateScramble();
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateScrambleViewsToCurrent();
                        ((CurrentSFragment) getParentFragment()).menuItemsEnable(true);
                    }
                });

            }
        });
    }

    public void initializeOptionsMenu() {
        if (mOnCreateCalled || mScrambling) {
            ((CurrentSFragment) getParentFragment()).menuItemsEnable(false);
        }
    }

    public void toggleScrambleImage() {
        if (mScrambleImageDisplay) {
            mScrambleImageDisplay = false;
            mScrambleImage.setVisibility(View.GONE);
        } else {
            if (!mScrambling) {
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
                int penalty;
                switch (((Solve) parent.getItemAtPosition(position)).getPenalty()) {
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
                SolveDialog d = SolveDialog.newInstance((Solve) parent.getItemAtPosition(position), position, penalty);
                d.setTargetFragment(CurrentSTimerFragment.this, DIALOG_REQUEST_CODE);
                d.show(getParentFragment().getActivity().getSupportFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        });

        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                mTimerText.setText(Solve.timeStringFromLong(System.nanoTime() - mStartTime));
                mUIHandler.postDelayed(this, 10);
            }
        };

        mTimerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRunning && !mScrambling) {
                    mStartTime = System.nanoTime();
                    mRunning = true;
                    mUIHandler.post(mTimerRunnable);
                    ((CurrentSFragment) getParentFragment()).menuItemsEnable(false);
                    mScrambleImage.setVisibility(View.GONE);
                    mScrambleImageDisplay = false;
                    ((MainActivity) mActivity).lockOrientation(true);
                    mScramblerThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mNextScrambleAndSvg = generateScramble();
                        }
                    });
                }

            }
        });

        mTimerText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mRunning) {
                    mEndTime = System.nanoTime();
                    mRunning = false;
                    mUIHandler.removeCallbacksAndMessages(null);

                    mFinalTime = mEndTime - mStartTime;
                    mTimerText.setText(Solve.timeStringFromLong(mFinalTime));

                    PuzzleType.sCurrentPuzzleType.getSession().addSolve(new Solve(mCurrentScrambleAndSvg, mFinalTime));
                    ((MainActivity) mActivity).lockOrientation(false);
                    ((CurrentSFragment) getParentFragment()).updateFragments();

                    if (mScrambling) {
                        mScrambleText.setText(R.string.scrambling);
                        mScramblerThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mUIHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCurrentScrambleAndSvg = mNextScrambleAndSvg;
                                        mNextScrambleAndSvg = null;
                                        updateScrambleViewsToCurrent();
                                        ((CurrentSFragment) getParentFragment()).menuItemsEnable(true);
                                    }
                                });
                            }
                        });
                    } else {
                        mCurrentScrambleAndSvg = mNextScrambleAndSvg;
                        mNextScrambleAndSvg = null;
                        updateScrambleViewsToCurrent();
                        ((CurrentSFragment) getParentFragment()).menuItemsEnable(true);
                    }
                    return true;
                } else {
                    return false;
                }

            }
        });

        if (mOnCreateCalled) {
            mScrambleText.setText(R.string.scrambling);
            mScramblerThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCurrentScrambleAndSvg = generateScramble();
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateScrambleViewsToCurrent();
                            ((CurrentSFragment) getParentFragment()).menuItemsEnable(true);
                        }
                    });


                }
            });

        }

        // On config change: If timer is running, update scramble views to current. If timer is not running and not scrambling, then update scramble views to current.
        if (!mOnCreateCalled && (mRunning || !mScrambling)) {
            updateScrambleViewsToCurrent();
        }

        if (PuzzleType.sCurrentPuzzleType.getSession().getNumberOfSolves() != 0) {
            mTimerText.setText(PuzzleType.sCurrentPuzzleType.getSession().getLatestSolve().getTimeString());
        }


        if (mScrambleImageDisplay) {
            if (!mScrambling) {
                mScrambleImage.setVisibility(View.VISIBLE);
                mScrambleImageDisplay = true;
                mScrambleImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mScrambleImageDisplay = false;
                        mScrambleImage.setVisibility(View.GONE);
                    }
                });
            }
        } else {
            mScrambleImage.setVisibility(View.GONE);
            mScrambleImageDisplay = false;
        }


        updateQuickStats();

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DIALOG_REQUEST_CODE) {
            Solve solve = PuzzleType.sCurrentPuzzleType.getSession().getSolveByPosition(data.getIntExtra(EXTRA_DIALOG_FINISH_SOLVE_INDEX, 0));
            switch (data.getIntExtra(EXTRA_DIALOG_FINISH_SELECTION, 0)) {
                case DIALOG_PENALTY_NONE:
                    solve.setPenalty(Solve.Penalty.NONE);
                    break;
                case DIALOG_PENALTY_PLUSTWO:
                    solve.setPenalty(Solve.Penalty.PLUSTWO);
                    break;
                case DIALOG_PENALTY_DNF:
                    solve.setPenalty(Solve.Penalty.DNF);
                    break;
                case DIALOG_RESULT_DELETE:
                    PuzzleType.sCurrentPuzzleType.getSession().deleteSolve(data.getIntExtra(EXTRA_DIALOG_FINISH_SOLVE_INDEX, 0));
                    break;
            }
        }
        updateQuickStats();
        ((SolveHListViewAdapter) mHListView.getAdapter()).updateSolvesList();
    }

    public class SolveHListViewAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveHListViewAdapter() {
            super(mActivity, 0, PuzzleType.sCurrentPuzzleType.getSession().getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getSession().getBestSolve(PuzzleType.sCurrentPuzzleType.getSession().getSolves()));
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getSession().getWorstSolve(PuzzleType.sCurrentPuzzleType.getSession().getSolves()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mActivity.getLayoutInflater().inflate(R.layout.hlist_item_solve, parent, false);
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
                addAll(PuzzleType.sCurrentPuzzleType.getSession().getSolves());
            else {
                for (Solve i : PuzzleType.sCurrentPuzzleType.getSession().getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getSession().getBestSolve(PuzzleType.sCurrentPuzzleType.getSession().getSolves()));
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getSession().getWorstSolve(PuzzleType.sCurrentPuzzleType.getSession().getSolves()));
            notifyDataSetChanged();
        }


    }


}