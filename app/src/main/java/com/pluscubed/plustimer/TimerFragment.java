package com.pluscubed.plustimer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import it.sephiroth.android.library.widget.HListView;

/**
 * TimerFragment
 */

public class TimerFragment extends Fragment {
    private static final String TAG = "TIMER";

    private TextView mTimerText;
    private TextView mScrambleText;
    private HListView mHListView;
    private Spinner mActionPuzzleSpinner;

    private long mStartTime;
    private long mEndTime;
    private long mFinalTime;

    private boolean mRunning;

    private Runnable mTimerRunnable;
    private PuzzleType mCurrentPuzzleType;

    private boolean mConfigChange;
    private boolean mStartup;
    private boolean mScrambling;

    private Handler mScramblerThreadHandler;
    private Handler mMainHandler;

    private String mCurrentScramble;
    private String mNextScramble;

    private TextView mQuickStatsSolves;
    private TextView mQuickStatsAo5;
    private TextView mQuickStatsAo12;


    public static String convertNanoToTime(long nano) {
        int minutes = (int) ((nano / (60 * 1000000000L)) % 60);
        int hours = (int) ((nano / (3600 * 1000000000L)) % 24);
        float seconds = (nano / 1000000000F) % 60;

        if (hours != 0) {
            return String.format("%d:%02d:%06.3f", hours, minutes, seconds);
        } else if (minutes != 0) {
            return String.format("%d:%06.3f", minutes, seconds);
        } else {
            return String.format("%.3f", seconds);
        }

    }

    public void updateQuickStats() {
        mQuickStatsSolves.setText(getString(R.string.solves) + mCurrentPuzzleType.getSession().getNumberOfSolves());
        if (mCurrentPuzzleType.getSession().getNumberOfSolves() >= 12) {
            mQuickStatsAo12.setText(getString(R.string.ao12) + convertNanoToTime(mCurrentPuzzleType.getSession().getCurrentAverageOf(12)));
        } else {
            mQuickStatsAo12.setText("");
        }
        if (mCurrentPuzzleType.getSession().getNumberOfSolves() >= 5) {
            mQuickStatsAo5.setText(getString(R.string.ao5) + convertNanoToTime(mCurrentPuzzleType.getSession().getCurrentAverageOf(5)));
        } else {
            mQuickStatsAo5.setText("");
        }


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_timer_menu, menu);

        mActionPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_item_puzzletypespinner));

        ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        ((ActionBarActivity) getActivity()).getSupportActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mActionPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
        mActionPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(mCurrentPuzzleType), false);
        mActionPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentPuzzleType = (PuzzleType) parent.getItemAtPosition(position);
                if (!mConfigChange) {
                    ((SolveAdapter) mHListView.getAdapter()).updateSolvesList();
                    updateQuickStats();

                    mScrambleText.setText(R.string.scrambling);
                    mTimerText.setText(R.string.ready);
                    mActionPuzzleSpinner.setEnabled(false);
                    mScramblerThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mScrambling = true;
                            mCurrentScramble = mCurrentPuzzleType.getPuzzle().generateScramble();
                            mScrambling = false;
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mScrambleText.setText(mCurrentScramble);
                                    mActionPuzzleSpinner.setEnabled(true);
                                }
                            });

                        }
                    });
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (mStartup || (mRunning || mScrambling)) {
            mActionPuzzleSpinner.setEnabled(false);
        }

        mConfigChange = false;
        mStartup = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mRunning = false;
        mCurrentPuzzleType = PuzzleType.THREE;
        mStartup = true;

        HandlerThread scramblerThread = new HandlerThread("ScramblerThread");
        scramblerThread.start();
        mScramblerThreadHandler = new Handler(scramblerThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_timer, container, false);
        mConfigChange = true;

        mTimerText = (TextView) v.findViewById(R.id.fragment_timer_text);
        mScrambleText = (TextView) v.findViewById(R.id.scramble_text);
        mHListView = (HListView) v.findViewById(R.id.fragment_hlistview);

        mQuickStatsAo12 = (TextView) v.findViewById(R.id.fragment_quickstats_ao12_text);
        mQuickStatsAo5 = (TextView) v.findViewById(R.id.fragment_quickstats_ao5_text);
        mQuickStatsSolves = (TextView) v.findViewById(R.id.fragment_quickstats_solves_text);

        SolveAdapter adapter = new SolveAdapter();
        mHListView.setAdapter(adapter);
        mHListView.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id) {
                Bundle args = new Bundle();
                args.putInt("position", position);
                SolveQuickModifyDialog dialog = new SolveQuickModifyDialog();
                dialog.setArguments(args);
                dialog.show(getActivity().getSupportFragmentManager(), "modify");
            }
        });

        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                mTimerText.setText(convertNanoToTime(System.nanoTime() - mStartTime));
                mMainHandler.postDelayed(this, 10);
            }
        };

        mTimerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRunning && !mScrambling) {
                    mStartTime = System.nanoTime();
                    mRunning = true;
                    mMainHandler.post(mTimerRunnable);
                    mActionPuzzleSpinner.setEnabled(false);
                    mScramblerThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mScrambling = true;
                            mNextScramble = mCurrentPuzzleType.getPuzzle().generateScramble();
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
                    mMainHandler.removeCallbacksAndMessages(null);
                    mFinalTime = mEndTime - mStartTime;
                    mTimerText.setText(convertNanoToTime(mFinalTime));
                    mCurrentPuzzleType.getSession().addSolve(new Solve(mCurrentScramble, mFinalTime));
                    ((SolveAdapter) mHListView.getAdapter()).notifyDataSetChanged();
                    updateQuickStats();
                    if (mScrambling) {
                        mScrambleText.setText(R.string.scrambling);
                        mScramblerThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCurrentScramble = mNextScramble;
                                        mNextScramble = null;
                                        mScrambleText.setText(mCurrentScramble);
                                        mActionPuzzleSpinner.setEnabled(true);
                                    }
                                });
                            }
                        });
                    } else {
                        mCurrentScramble = mNextScramble;
                        mNextScramble = null;
                        mScrambleText.setText(mCurrentScramble);
                        mActionPuzzleSpinner.setEnabled(true);
                    }
                    return true;
                } else {
                    return false;
                }

            }
        });

        if (mStartup) {
            mScrambleText.setText(R.string.scrambling);
            mScramblerThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mScrambling = true;
                    mCurrentScramble = mCurrentPuzzleType.getPuzzle().generateScramble();
                    mScrambling = false;
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mScrambleText.setText(mCurrentScramble);
                            if (mActionPuzzleSpinner != null) {
                                mActionPuzzleSpinner.setEnabled(true);
                            }
                        }
                    });

                }
            });

        }

        if (!mStartup && (mRunning || !mScrambling)) {
            mScrambleText.setText(mCurrentScramble);
        }

        if (!mRunning && mCurrentPuzzleType.getSession().getNumberOfSolves() != 0) {
            mTimerText.setText(convertNanoToTime(mCurrentPuzzleType.getSession().getLatestSolve().getTime()));
        }

        updateQuickStats();

        return v;
    }

    private class SolveAdapter extends BaseAdapter {

        private List<Solve> mObjects;
        private Context mContext;
        private LayoutInflater mInflater;

        public SolveAdapter() {
            super();
            mContext = getActivity();
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mObjects = mCurrentPuzzleType.getSession().getSolves();
        }

        @Override
        public Object getItem(int position) {
            return mObjects.get(position);
        }

        @Override
        public int getCount() {
            return mObjects.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_list_item_solve, parent, false);
            }
            Solve s = (Solve) getItem(position);
            TextView time = (TextView) convertView.findViewById(R.id.fragment_hlistview_text);
            time.setText(convertNanoToTime(s.getTime()));
            return convertView;
        }

        public void updateSolvesList() {
            mObjects = mCurrentPuzzleType.getSession().getSolves();
            notifyDataSetChanged();
        }


    }

    private class SolveQuickModifyDialog extends DialogFragment {
        private int position;


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            position = getArguments().getInt("position");

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(new String[]{getString(R.string.delete)}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mCurrentPuzzleType.getSession().deleteSolve(position);
                    updateQuickStats();
                    ((SolveAdapter) mHListView.getAdapter()).notifyDataSetChanged();
                }
            });
            return builder.create();
        }
    }



}