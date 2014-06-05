package com.pluscubed.plustimer;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.app.Fragment;
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
        mQuickStats.setText(buildQuickStatsWithAveragesOf(getMainActivity(), 5, 12, 100));
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
        String scramble = PuzzleType.sCurrentPuzzleType.getPuzzle().generateScramble();
        ScrambleAndSvg scrambleAndSvg = null;
        try {
            scrambleAndSvg = new ScrambleAndSvg(scramble, PuzzleType.sCurrentPuzzleType.getPuzzle().drawScramble(scramble, null));
        } catch (InvalidScrambleException e) {
            e.printStackTrace();
        }

        return scrambleAndSvg;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public MainActivity getMainActivity() {
        return ((MainActivity) getParentFragment().getActivity());
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
        getCurrentSFragment().updateFragments();
        mScrambling = true;
        mScrambleText.setText(R.string.scrambling);
        mTimerText.setText(R.string.ready);
        updateOptionsMenu();
        mScrambleImage.setVisibility(View.GONE);
        mScrambleImageDisplay = false;

        mScramblerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mCurrentScrambleAndSvg = generateScramble();
                mScrambling = false;
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        updateScrambleViewsToCurrent();
                        updateOptionsMenu();
                    }
                });

            }
        });
    }

    public void updateOptionsMenu() {
        if (mOnCreateCalled || mScrambling || mRunning) {
            getCurrentSFragment().menuItemsEnable(false);
        } else {
            getCurrentSFragment().menuItemsEnable(true);
        }
    }

    private CurrentSFragment getCurrentSFragment() {
        return ((CurrentSFragment) getParentFragment());
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
                getMainActivity().showCurrentSolveDialog(position);
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
                    updateOptionsMenu();
                    mScrambleImage.setVisibility(View.GONE);
                    mScrambleImageDisplay = false;
                    getMainActivity().lockOrientation(true);
                    mScramblerThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mScrambling = true;
                            mNextScrambleAndSvg = generateScramble();
                            mScrambling = false;
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
                    getMainActivity().lockOrientation(false);
                    getCurrentSFragment().updateFragments();

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
                                        updateOptionsMenu();
                                    }
                                });
                            }
                        });
                    } else {
                        mCurrentScrambleAndSvg = mNextScrambleAndSvg;
                        mNextScrambleAndSvg = null;
                        updateScrambleViewsToCurrent();
                        updateOptionsMenu();
                    }
                    return true;
                } else {
                    return false;
                }

            }
        });

        if (mOnCreateCalled) {
            mScrambling = true;
            updateOptionsMenu();
            mScrambleText.setText(R.string.scrambling);
            mScramblerThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCurrentScrambleAndSvg = generateScramble();
                    mScrambling = false;
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateScrambleViewsToCurrent();
                            updateOptionsMenu();
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

    public class SolveHListViewAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveHListViewAdapter() {
            super(getMainActivity(), 0, PuzzleType.sCurrentPuzzleType.getSession().getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getSession().getBestSolve(PuzzleType.sCurrentPuzzleType.getSession().getSolves()));
            mBestAndWorstSolves.add(PuzzleType.sCurrentPuzzleType.getSession().getWorstSolve(PuzzleType.sCurrentPuzzleType.getSession().getSolves()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getMainActivity().getLayoutInflater().inflate(R.layout.hlist_item_solve, parent, false);
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