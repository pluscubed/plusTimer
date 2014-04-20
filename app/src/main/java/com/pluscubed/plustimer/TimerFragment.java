package com.pluscubed.plustimer;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import net.gnehzr.tnoodle.scrambles.InvalidScrambleException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.sephiroth.android.library.widget.HListView;

/**
 * TimerFragment
 */

public class TimerFragment extends Fragment {
    public static final String TAG = "TIMER";
    public static final String DIALOG_BUNDLEKEY_TIMESTRING="TIMESTRING";
    public static final String DIALOG_BUNDLEKEY_SOLVE_POSITION ="POSITION";
    public static final String DIALOG_BUNDLEKEY_PENALTY ="PENALTY";
    public static final String DIALOG_INTENT_EXTRA_SOLVE_POSITION ="BACKTOFRAGMENTPOSITION";
    public static final String DIALOG_INTENT_EXTRA_SELECTION ="BACKTOFRAGMENTSELECTION";
    public static final String DIALOG_TAG="MODIFY_DIALOG";
    public static final int DIALOG_REQUEST_CODE =0;
    public static final int DIALOG_RESULT_DELETE=4;
    public static final int DIALOG_RESULT_PENALTY_NONE=0;
    public static final int DIALOG_RESULT_PENALTY_PLUSTWO=1;
    public static final int DIALOG_RESULT_PENALTY_DNF=2;

    private TextView mTimerText;
    private TextView mScrambleText;
    private HListView mHListView;
    private ImageView mScrambleImage;
    private TextView mQuickStatsSolves;
    private TextView mQuickStats;

    private Spinner mMenuPuzzleSpinner;
    private MenuItem mMenuDisplayScramble;

    private long mStartTime;
    private long mEndTime;
    private long mFinalTime;

    private Runnable mTimerRunnable;
    private PuzzleType mCurrentPuzzleType;

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

    String buildQuickStats(Integer... currentAverages) {
        Arrays.sort(currentAverages, Collections.reverseOrder());
        String s = "";
        for (int i : currentAverages) {
            if (mCurrentPuzzleType.getSession().getNumberOfSolves() >= i) {
                s += getString(R.string.ao) + i + ": " + mCurrentPuzzleType.getSession().getStringCurrentAverageOf(i) + "\n";
            }
        }
        if (mCurrentPuzzleType.getSession().getNumberOfSolves() > 0) {
            s += getString(R.string.mean) + mCurrentPuzzleType.getSession().getStringMean();
        }
        return s;
    }

    void updateQuickStats() {
        mQuickStatsSolves.setText(getString(R.string.solves) + mCurrentPuzzleType.getSession().getNumberOfSolves());
        mQuickStats.setText(buildQuickStats(5, 12, 100, 1000));
        ((SolveHListViewAdapter) mHListView.getAdapter()).updateSolvesList();

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
            e.printStackTrace();
        }
        mScrambling = false;

        return scrambleAndSvg;
    }

    void menuItemsEnable(boolean enable) {
        mMenuPuzzleSpinner.setEnabled(enable);
        mMenuDisplayScramble.setEnabled(enable);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (ActionBarActivity) getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mCurrentPuzzleType=PuzzleType.THREE;
        mCurrentPuzzleType.resetSession();

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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_timer_menu, menu);

        mMenuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_item_puzzletypespinner));
        mMenuDisplayScramble = menu.findItem(R.id.menu_item_display_scramble_image);

        final ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        mActivity.getSupportActionBar().getThemedContext(),
                        android.R.layout.simple_spinner_item,
                        PuzzleType.values()
                );

        puzzleTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMenuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);

        mMenuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mMenuPuzzleSpinner.getSelectedItemPosition() != puzzleTypeSpinnerAdapter.getPosition(mCurrentPuzzleType)) {

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
        mMenuPuzzleSpinner.post(new Runnable() {
            @Override
            public void run() {
                mMenuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(mCurrentPuzzleType), true);
            }
        });


        if (mOnCreateCalled || mRunning || mScrambling) {
            menuItemsEnable(false);
        }
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

        mTimerText = (TextView) v.findViewById(R.id.fragment_timer_text);
        mScrambleText = (TextView) v.findViewById(R.id.scramble_text);
        mScrambleImage = (ImageView) v.findViewById(R.id.fragment_scramble_image);
        mHListView = (HListView) v.findViewById(R.id.fragment_hlistview);

        mQuickStats = (TextView) v.findViewById(R.id.fragment_quickstats_text);
        mQuickStatsSolves = (TextView) v.findViewById(R.id.fragment_quickstats_solves_text);

        final SolveHListViewAdapter adapter = new SolveHListViewAdapter();
        mHListView.setAdapter(adapter);
        mHListView.setOnItemClickListener(new it.sephiroth.android.library.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id) {
                int penalty;
                if(((Solve)parent.getItemAtPosition(position)).isDnf())
                    penalty=DIALOG_RESULT_PENALTY_DNF;
                else if(((Solve)parent.getItemAtPosition(position)).isPlusTwo())
                    penalty=DIALOG_RESULT_PENALTY_PLUSTWO;
                else
                    penalty=DIALOG_RESULT_PENALTY_NONE;
                SolveQuickModifyDialog d=SolveQuickModifyDialog.newInstance((Solve) parent.getItemAtPosition(position), position,penalty);
                d.setTargetFragment(TimerFragment.this, DIALOG_REQUEST_CODE);
                d.show(mActivity.getSupportFragmentManager(), DIALOG_TAG);
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
                    mTimerText.setText(Solve.timeStringFromLong(mFinalTime));
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
                            menuItemsEnable(true);
                        }
                    });


                }
            });

        }

        // On config change: If timer is running, update scramble views to current. If timer is not running and not scrambling, then update scramble views to current.
        if (!mOnCreateCalled && (mRunning || !mScrambling)) {
            updateScrambleViewsToCurrent();
        }

        if (!mRunning && mCurrentPuzzleType.getSession().getNumberOfSolves() != 0) {
            mTimerText.setText(mCurrentPuzzleType.getSession().getLatestSolve().getTimeString());
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
        if (requestCode == DIALOG_REQUEST_CODE) {
            Solve solve=mCurrentPuzzleType.getSession().getSolveByPosition(data.getIntExtra(DIALOG_INTENT_EXTRA_SOLVE_POSITION, 0));
            switch(data.getIntExtra(DIALOG_INTENT_EXTRA_SELECTION,0)){
                case DIALOG_RESULT_PENALTY_NONE:
                    solve.setPlusTwo(false);
                    solve.setDnf(false);
                    break;
                case DIALOG_RESULT_PENALTY_PLUSTWO:
                    solve.setPlusTwo(true);
                    break;
                case DIALOG_RESULT_PENALTY_DNF:
                    solve.setDnf(true);
                    break;
                case DIALOG_RESULT_DELETE:
                    mCurrentPuzzleType.getSession().deleteSolve(data.getIntExtra(DIALOG_INTENT_EXTRA_SOLVE_POSITION, 0));
                    break;
            }
        }
        updateQuickStats();
    }

    public static class SolveQuickModifyDialog extends DialogFragment {
        private String timeString;
        private int position;
        private int selection=0;

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if(!(getTargetFragment()==null)) {
                Intent i=new Intent();
                i.putExtra(DIALOG_INTENT_EXTRA_SOLVE_POSITION, position);
                i.putExtra(DIALOG_INTENT_EXTRA_SELECTION, selection);
                getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, i);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            position = getArguments().getInt(DIALOG_BUNDLEKEY_SOLVE_POSITION);
            timeString= getArguments().getString(DIALOG_BUNDLEKEY_TIMESTRING);

            LayoutInflater inflater=getActivity().getLayoutInflater();
            View v=inflater.inflate(R.layout.dialog_solve, null);

            Spinner penaltySpinner= (Spinner) v.findViewById(R.id.dialog_modify_penalty_spinner);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.penalty_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            penaltySpinner.setAdapter(adapter);

            penaltySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int selectedPosition, long id) {
                    selection=selectedPosition;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            penaltySpinner.setSelection(getArguments().getInt(DIALOG_BUNDLEKEY_PENALTY));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(timeString)
                    .setView(v)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            })
            .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selection=DIALOG_RESULT_DELETE;
                }
            });
            return builder.create();
        }

        static SolveQuickModifyDialog newInstance(Solve i, int position, int penalty){
            SolveQuickModifyDialog d=new SolveQuickModifyDialog();
            Bundle args = new Bundle();
            args.putString(DIALOG_BUNDLEKEY_TIMESTRING, i.getTimeString());
            args.putInt(DIALOG_BUNDLEKEY_SOLVE_POSITION, position);
            args.putInt(DIALOG_BUNDLEKEY_PENALTY, penalty);
            d.setArguments(args);
            return d;
        }
    }

    private class SolveHListViewAdapter extends BaseAdapter {

        private final Context mContext;
        private final LayoutInflater mInflater;
        private List<Solve> mSolves;
        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveHListViewAdapter() {
            super();
            mContext = mActivity;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mSolves = mCurrentPuzzleType.getSession().getSolves();
        }

        @Override
        public Object getItem(int position) {
            return mSolves.get(position);
        }

        @Override
        public int getCount() {
            return mSolves.size();
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

            time.setText("");

            for(Solve a: mBestAndWorstSolves){
                if(a.equals(s)){
                    time.setText("(" + s.getTimeString() + ")");
                }
            }

            if(time.getText()==""){
                time.setText( s.getTimeString() );
            }

            return convertView;
        }

        public void updateSolvesList() {
            mSolves = mCurrentPuzzleType.getSession().getSolves();
            mBestAndWorstSolves =mCurrentPuzzleType.getSession().getBestAndWorstSolves((ArrayList<Solve>) (mSolves));
            notifyDataSetChanged();
        }


    }


}