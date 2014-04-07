package com.pluscubed.plustimer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

import java.util.ArrayList;
import java.util.Collections;
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
    private ImageView mScrambleImage;
    private TextView mQuickStatsSolves;
    private TextView mQuickStats;


    private long mStartTime;
    private long mEndTime;
    private long mFinalTime;

    private Runnable mTimerRunnable;
    private PuzzleType mCurrentPuzzleType;

    private boolean mConfigChange;
    private boolean mStartup;
    private boolean mScrambling;
    private boolean mRunning;
    private boolean mScrambleImageDisplay;
    private boolean mMenuItemsEnabled;

    private Handler mScramblerThreadHandler;
    private Handler mUIHandler;

    private ScrambleAndSvg mCurrentScrambleAndSvg;
    private ScrambleAndSvg mNextScrambleAndSvg;

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

    void updateQuickStats() {
        String quickStats = new String();
        mQuickStatsSolves.setText(getString(R.string.solves) + mCurrentPuzzleType.getSession().getNumberOfSolves());
        if (mCurrentPuzzleType.getSession().getNumberOfSolves() >= 1000) {
            quickStats += getString(R.string.ao1000) + convertNanoToTime(mCurrentPuzzleType.getSession().getCurrentAverageOf(100)) + "\n";
        }
        if (mCurrentPuzzleType.getSession().getNumberOfSolves() >= 100) {
            quickStats += getString(R.string.ao100) + convertNanoToTime(mCurrentPuzzleType.getSession().getCurrentAverageOf(100)) + "\n";
        }
        if (mCurrentPuzzleType.getSession().getNumberOfSolves() >= 12) {
            quickStats += getString(R.string.ao12) + convertNanoToTime(mCurrentPuzzleType.getSession().getCurrentAverageOf(12)) + "\n";
        }
        if (mCurrentPuzzleType.getSession().getNumberOfSolves() >= 5) {
            quickStats += getString(R.string.ao5) + convertNanoToTime(mCurrentPuzzleType.getSession().getCurrentAverageOf(5)) + "\n";
        }
        if (mCurrentPuzzleType.getSession().getNumberOfSolves() > 0) {
            quickStats += getString(R.string.mean) + convertNanoToTime(mCurrentPuzzleType.getSession().getMean());
        }
        mQuickStats.setText(quickStats);
        ((SolveAdapter) mHListView.getAdapter()).updateSolvesList();

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
        String scramble = mCurrentPuzzleType.getPuzzle().generateScramble();
        ScrambleAndSvg scrambleAndSvg = null;
        try {
            scrambleAndSvg = new ScrambleAndSvg(scramble, mCurrentPuzzleType.getPuzzle().drawScramble(scramble, null));
        } catch (InvalidScrambleException e) {
        }
        mScrambling = false;

        return scrambleAndSvg;
    }

    void menuItemsEnable(boolean enable) {
        mMenuItemsEnabled = enable;
        ActivityCompat.invalidateOptionsMenu(getActivity());
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
        mUIHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_timer_menu, menu);

        Spinner menuItemPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_item_puzzletypespinner));

        ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        ((ActionBarActivity) getActivity()).getSupportActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        menuItemPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
        menuItemPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(mCurrentPuzzleType), false);
        menuItemPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mConfigChange) {
                    mCurrentPuzzleType = (PuzzleType) parent.getItemAtPosition(position);
                    updateQuickStats();

                    mScrambleText.setText(R.string.scrambling);
                    mTimerText.setText(R.string.ready);
                    menuItemsEnable(false);
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
                                    menuItemsEnable(true);
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

        menuItemPuzzleSpinner.setEnabled(mMenuItemsEnabled);
        menu.findItem(R.id.menu_item_display_scramble_image).setEnabled(mMenuItemsEnabled);

        if (mStartup || (mRunning || mScrambling)) {
            menuItemPuzzleSpinner.setEnabled(true);
            menu.findItem(R.id.menu_item_display_scramble_image).setEnabled(true);
        }

        mConfigChange = false;
        mStartup = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_display_scramble_image:
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_timer, container, false);

        mConfigChange = true;

        mTimerText = (TextView) v.findViewById(R.id.fragment_timer_text);
        mScrambleText = (TextView) v.findViewById(R.id.scramble_text);
        mScrambleImage = (ImageView) v.findViewById(R.id.fragment_scramble_image);
        mHListView = (HListView) v.findViewById(R.id.fragment_hlistview);

        mQuickStats = (TextView) v.findViewById(R.id.fragment_quickstats_text);
        mQuickStatsSolves = (TextView) v.findViewById(R.id.fragment_quickstats_solves_text);

        SolveAdapter adapter = new SolveAdapter();
        mHListView.setAdapter(adapter);
        mHListView.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id) {
                Bundle args = new Bundle();
                args.putInt("position", position);
                SolveQuickModifyDialog dialog = new SolveQuickModifyDialog();
                dialog.setTargetFragment(TimerFragment.this, 0);
                dialog.setArguments(args);
                dialog.show(getActivity().getSupportFragmentManager(), "modify");
            }
        });

        mTimerRunnable = new Runnable() {
            @Override
            public void run() {
                mTimerText.setText(convertNanoToTime(System.nanoTime() - mStartTime));
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
                    menuItemsEnable(false);
                    mScrambleImage.setVisibility(View.GONE);
                    mScrambleImageDisplay = false;
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
                    mTimerText.setText(convertNanoToTime(mFinalTime));
                    mCurrentPuzzleType.getSession().addSolve(new Solve(mCurrentScrambleAndSvg, mFinalTime));
                    updateQuickStats();
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
                                        menuItemsEnable(true);
                                    }
                                });
                            }
                        });
                    } else {
                        mCurrentScrambleAndSvg = mNextScrambleAndSvg;
                        mNextScrambleAndSvg = null;
                        updateScrambleViewsToCurrent();
                        menuItemsEnable(true);
                    }
                    return true;
                } else {
                    return false;
                }

            }
        });

        if (mStartup) {
            mScrambleText.setText(R.string.scrambling);
            menuItemsEnable(false);
            mScramblerThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCurrentScrambleAndSvg = generateScramble();
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateScrambleViewsToCurrent();
                            menuItemsEnable(true);
                        }
                    });


                }
            });

        }

        // On config change: If timer is running, update scramble views to current. If timer is not running and not scrambling, then update scramble views to current.
        if (!mStartup && (mRunning || !mScrambling)) {
            updateScrambleViewsToCurrent();
        }

        if (!mRunning && mCurrentPuzzleType.getSession().getNumberOfSolves() != 0) {
            mTimerText.setText(convertNanoToTime(mCurrentPuzzleType.getSession().getLatestSolve().getTime()));
            updateScrambleViewsToCurrent();
        }

        if (!mRunning) {
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
        }

        updateQuickStats();

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            mCurrentPuzzleType.getSession().deleteSolve(data.getIntExtra("position", 0));
            updateQuickStats();
        }
    }

    public static class SolveQuickModifyDialog extends DialogFragment {
        private int position;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            position = getArguments().getInt("position");

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setItems(new String[]{getString(R.string.delete)}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (getTargetFragment() == null) {
                        return;
                    }
                    Intent i = new Intent();
                    i.putExtra("position", position);
                    getTargetFragment().onActivityResult(getTargetRequestCode(), 0, i);
                }
            });
            return builder.create();
        }
    }

    private class SolveAdapter extends BaseAdapter {

        private final Context mContext;
        private final LayoutInflater mInflater;
        private List<Solve> mObjects;
        private ArrayList<Long> mTimes;
        private int indexBest;
        private int indexWorst;

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


            if (position == indexBest || position == indexWorst) {
                time.setText("(" + convertNanoToTime(s.getTime()) + ")");
            } else {
                time.setText(convertNanoToTime(s.getTime()));
            }

            return convertView;
        }

        public void updateSolvesList() {
            mObjects = mCurrentPuzzleType.getSession().getSolves();
            if (mObjects.size() > 0) {
                mTimes = new ArrayList<Long>();
                for (Solve i : mObjects) {
                    mTimes.add(i.getTime());
                }
                indexBest = mTimes.indexOf(Collections.min(mTimes));
                indexWorst = mTimes.indexOf(Collections.max(mTimes));
            }
            notifyDataSetChanged();
        }


    }


}