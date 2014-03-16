package com.pluscubed.plustimer;

import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;

import java.util.ArrayList;

/**
 * TimerFragment
 */

public class TimerFragment extends Fragment {
    private static final String TAG = "TIMER";

    private TextView mTimerTextView;
    private TextView mScrambleText;
    private RelativeLayout mTimerRelative;
    private LinearLayout mSessionScrollLinear;

    private long mStartTime;
    private long mEndTime;

    private boolean mRunning;

    private Runnable mTimerRunnable;
    private GenerateScramblesTask mScrambleTask;
    private PuzzleType mCurrentPuzzleType;
    private Session mSession;


    private boolean mConfigChange;
    private boolean mStartup;


    public static String convertNanoToTime(long nano) {


        int minutes = (int) ((nano / (60 * 1000000000L)) % 60);
        int hours = (int) ((nano / (3600 * 1000000000L)) % 24);
        float seconds = (nano / 1000000000F) % 60;

        if (hours == 0) {
            if (minutes == 0) {
                return String.format("%.3f", seconds);
            }
            if (seconds < 10)
                return String.format("%d:0%.3f", minutes, seconds);
            else
                return String.format("%d:%.3f", minutes, seconds);
        }
        if (minutes < 10)
            return String.format("%d:0%d:%.3f", hours, minutes, seconds);
        else
            return String.format("%d:%d:%.3f", hours, minutes, seconds);
    }

    private void updateScramble() {
        if (mScrambleTask == null) {
            mScrambleTask = new GenerateScramblesTask();
            mScrambleTask.execute();
        }


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_timer_menu, menu);

        Spinner puzzleTypeSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_item_puzzletypespinner));

        ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        ((ActionBarActivity) getActivity()).getSupportActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        puzzleTypeSpinner.setAdapter(puzzleTypeSpinnerAdapter);
        puzzleTypeSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(mCurrentPuzzleType), false);
        puzzleTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentPuzzleType = (PuzzleType) parent.getItemAtPosition(position);

                if (!mConfigChange) {
                    mScrambleText.setText(R.string.scrambling);
                    updateScramble();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mConfigChange = false;


    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mRunning = false;
        mCurrentPuzzleType = PuzzleType.THREE;
        mStartup = true;
        mSession = Session.get();
    }


    private TextView createSessionTextView(LayoutInflater inflater, String time) {

        TextView temp = (TextView) inflater.inflate(R.layout.scroll_textview, null);
        temp.setText(time);
        return temp;
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_timer, container, false);
        mConfigChange = true;

        mTimerTextView = (TextView) v.findViewById(R.id.fragment_timer_text);
        mTimerRelative = (RelativeLayout) v.findViewById(R.id.fragment_timer_relative);
        mSessionScrollLinear = (LinearLayout) v.findViewById(R.id.fragment_timer_sessionscrolllinear);
        mScrambleText = (TextView) v.findViewById(R.id.scramble_text);

        if (mSession.getLastSolve() != null && !mRunning) {
            mScrambleText.setText(mSession.getLastSolve().getScramble());
            mTimerTextView.setText(mSession.getLastSolve().getTime());
        } else if (mRunning) {
            mScrambleText.setText(mSession.getLastSolve().getScramble());
        }


        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                mTimerTextView.setText(convertNanoToTime(System.nanoTime() - mStartTime));
                mTimerTextView.postDelayed(this, 1);
            }
        };

        mTimerRelative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRunning) {
                    mStartTime = System.nanoTime();
                    mRunning = true;
                    mTimerTextView.post(mTimerRunnable);
                    mSession.addSolve(new Solve(mScrambleText.getText().toString(), null, null));
                }

            }
        });

        mTimerRelative.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mRunning) {
                    mEndTime = System.nanoTime();
                    mRunning = false;
                    mTimerTextView.removeCallbacks(mTimerRunnable);
                    String finalTime = convertNanoToTime(mEndTime - mStartTime);
                    mSession.getLastSolve().setScramble(mScrambleText.getText().toString());
                    mSession.getLastSolve().setTime(finalTime);
                    updateScramble();
                    mTimerTextView.setText(mSession.getLastSolve().getTime());
                    mSessionScrollLinear.addView(createSessionTextView(getActivity().getLayoutInflater(), mSession.getLastSolve().getTime()));
                    return true;
                } else {
                    return false;
                }

            }
        });

        if (mStartup)
            updateScramble();

        mStartup = false;

        return v;
    }


    public enum PuzzleType {
        //TODO: find faster square-1 scrambler
        //SQ1("sq1", "Square 1"),
        SKEWB("skewb", "Skewb"),
        PYRAMINX("pyram", "Pyraminx"),
        MINX("minx", "Megaminx"),
        CLOCK("clock", "Clock"),
        SEVEN("777", "7x7"),
        SIX("666", "6x6"),
        FIVE("555", "5x5"),
        FOURFAST("444fast", "4x4-fast"),
        THREE("333", "3x3"),
        TWO("222", "2x2");

        private String type;
        private String displayName;
        private ArrayList<String> queuedScrambles;

        PuzzleType(String type, String displayName) {
            this.type = type;
            this.displayName = displayName;
            queuedScrambles = new ArrayList<String>();
        }

        public String getScrambleType() {
            return type;
        }

        @Override
        public String toString() {
            return displayName;
        }

        public void addScramble(String scramble) {
            queuedScrambles.add(scramble);
        }


        public String getOldestScramble() {

            String s = queuedScrambles.get(0);
            queuedScrambles.remove(0);
            return s;
        }

        public int sizeOfQueuedScrambles() {
            return queuedScrambles.size();
        }
    }


    /* (AsyncTask) Inner Class*/
    private class GenerateScramblesTask extends AsyncTask<Void, Void, Void> {

        private Puzzle p;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... voids) {

            if (mCurrentPuzzleType.sizeOfQueuedScrambles() == 0) {
                try {
                    p = PuzzlePlugins.getScramblers().get(mCurrentPuzzleType.getScrambleType()).cachedInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mCurrentPuzzleType.addScramble(p.generateScramble());
            }

            publishProgress();
            if (p == null) {
                try {
                    p = PuzzlePlugins.getScramblers().get(mCurrentPuzzleType.getScrambleType()).cachedInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mCurrentPuzzleType.addScramble(p.generateScramble());

            return null;

        }

        @Override
        protected void onProgressUpdate(Void... NOTHING) {
            mScrambleText.setText(mCurrentPuzzleType.getOldestScramble());

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mScrambleTask = null;
        }
    }

}