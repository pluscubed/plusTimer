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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;

/**
 * TimerFragment
 */

public class TimerFragment extends Fragment {
    private static final String TAG = "TIMER";

    private TextView mTimerTextView;
    private TextView mScrambleText;
    private RelativeLayout mLayout;

    private long mStartTime;
    private long mEndTime;


    private boolean mRunning;
    private Runnable mTimerRunnable;

    private ScramblingTask mScrambleTask;

    private String mPuzzleType;

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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_timer_menu, menu);

        Spinner puzzleTypeSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_item_puzzletypespinner));

        ArrayAdapter<PuzzleTypes> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleTypes>(
                        ((ActionBarActivity) getActivity()).getSupportActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleTypes.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        puzzleTypeSpinner.setAdapter(puzzleTypeSpinnerAdapter);
        puzzleTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PuzzleTypes selectedPuzzle = (PuzzleTypes) parent.getItemAtPosition(position);
                mPuzzleType = selectedPuzzle.getType();
                if (mScrambleTask == null) {
                    mScrambleTask = new ScramblingTask();
                    mScrambleTask.execute(mPuzzleType);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        puzzleTypeSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(PuzzleTypes.THREE), false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_timer, container, false);

        mTimerTextView = (TextView) v.findViewById(R.id.fragment_timer_text);
        mLayout = (RelativeLayout) v.findViewById(R.id.fragment_timer_relative);
        mRunning = false;
        mScrambleText = (TextView) v.findViewById(R.id.scramble_text);
        mPuzzleType = PuzzleTypes.THREE.getType();


        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                mTimerTextView.setText(convertNanoToTime(System.nanoTime() - mStartTime));
                mTimerTextView.postDelayed(this, 1);
            }
        };


        mLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRunning) {

                    mStartTime = System.nanoTime();
                    mRunning = true;
                    mTimerTextView.post(mTimerRunnable);
                    if (mScrambleTask == null) {
                        mScrambleTask = new ScramblingTask();
                        mScrambleTask.execute(mPuzzleType);
                    }
                }

            }
        });

        mLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mRunning) {
                    mEndTime = System.nanoTime();
                    mRunning = false;
                    mTimerTextView.removeCallbacks(mTimerRunnable);
                    mTimerTextView.setText(convertNanoToTime(mEndTime - mStartTime));
                    return true;
                } else {
                    return false;
                }

            }
        });


        mScrambleTask = new ScramblingTask();
        mScrambleTask.execute(mPuzzleType);

        return v;
    }

    /*onCreate*/

    public enum PuzzleTypes {
        SQ1("sq1", "Square 1"),
        SKEWB("skewb", "Skewb"),
        PYRAMINX("pyram", "Pyraminx"),
        MINX("minx", "Megaminx"),
        CLOCK("clock", "Clock"),
        SEVEN("777", "7x7"),
        SIX("666", "6x6"),
        FIVE("555", "5x5"),
        FOUR("444fast", "4x4"),
        THREE("333", "3x3"),
        TWO("222", "2x2");

        private String type;
        private String displayName;

        PuzzleTypes(String type, String displayName) {
            this.type = type;
            this.displayName = displayName;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /* onCreateView */

    /*ScramblingTask (AsyncTask) Inner Class*/
    private class ScramblingTask extends AsyncTask<String, Void, String> {

        private Puzzle p;

        @Override
        protected void onPreExecute() {
            mScrambleText.setText("Scrambling!");
        }

        @Override
        protected String doInBackground(String... type) {
            if (type.length == 1) {
                try {
                    p = PuzzlePlugins.getScramblers().get(type[0]).cachedInstance();
                } catch (Exception e) {
                    return null;
                }
                return p.generateScramble();
            } else {
                return null;
            }

        }

        @Override
        protected void onPostExecute(String scramble) {
            mScrambleText.setText(scramble);
            mScrambleTask = null;
        }

    }


}