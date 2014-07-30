package com.pluscubed.plustimer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * Session tab
 */
public class SolveListFragment extends CurrentSBaseFragment implements MainActivity.ActionModeNavDrawerCallback {

    private static final String ARG_PUZZLETYPE_DISPLAYNAME = "com.pluscubed.plustimer.solvelist.display_name";
    private static final String ARG_SESSION_POSITION = "com.pluscubed.plustimer.solvelist.session_position";
    private static final String ARG_CURRENT_BOOLEAN = "com.pluscubed.plustimer.solvelist.current_boolean";

    private static final String STATE_CAB_BOOLEAN = "cab_displayed";

    private Session mSession;

    private TextView mQuickStats;
    private ListView mListView;

    private int mSessionIndex;
    private String mPuzzleTypeDisplayName;
    private boolean mCurrentToggle;

    private Button mReset;
    private Button mSubmit;

    private ShareActionProvider mShareActionProvider;

    private ActionMode mActionMode;

    public static SolveListFragment newInstance(boolean current, String displayName, int sessionIndex) {
        SolveListFragment f = new SolveListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SESSION_POSITION, sessionIndex);
        args.putString(ARG_PUZZLETYPE_DISPLAYNAME, displayName);
        args.putBoolean(ARG_CURRENT_BOOLEAN, current);
        f.setArguments(args);
        return f;
    }

    /* public String buildAdvancedStatsWithAveragesOf(Context context, Integer... currentAverages) {
         Arrays.sort(currentAverages, Collections.reverseOrder());
         String s = "";
         for (int i : currentAverages) {
             if (PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getNumberOfSolves() >= i) {
                 if (mCurrentToggle) {
                     s += context.getString(R.string.cao) + i + ": " + PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getStringCurrentAverageOf(i) + "\n";
                 } else {
                     s += context.getString(R.string.lao) + i + ": " + PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getStringCurrentAverageOf(i) + "\n";
                 }
                 s += context.getString(R.string.bao) + i + ": " + PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getStringBestAverageOf(i) + "\n";
             }
         }
         if (PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getNumberOfSolves() > 0) {
             s += context.getString(R.string.mean) + PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getStringMean();
         }
         return s;
     }
 */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_CAB_BOOLEAN, mActionMode != null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mCurrentToggle) {
            inflater.inflate(R.menu.menu_current_s_detailslist, menu);
            setUpPuzzleSpinner(menu);
        } else {
            inflater.inflate(R.menu.menu_history_solvelist, menu);
        }
        MenuItem shareItem = menu.findItem(R.id.menu_solvelist_share);
        mShareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);
        setShareIntent();
    }

    @Override
    public ActionMode getActionMode() {
        return mActionMode;
    }

    private void setShareIntent() {
        if (mShareActionProvider != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(
                    Intent.EXTRA_TEXT,
                    mSession.toString(getAttachedActivity(), PuzzleType.get(mPuzzleTypeDisplayName).toString(), mCurrentToggle, true)
            );
            mShareActionProvider.setShareIntent(intent);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentToggle = getArguments().getBoolean(ARG_CURRENT_BOOLEAN);
        mPuzzleTypeDisplayName = getArguments().getString(ARG_PUZZLETYPE_DISPLAYNAME);
        mSessionIndex = getArguments().getInt(ARG_SESSION_POSITION);
        mSession = PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity());
        setHasOptionsMenu(true);
    }

    public void updateQuickStats() {
        mQuickStats.setText(mSession.toString(getAttachedActivity(), PuzzleType.get(mPuzzleTypeDisplayName).toString(), mCurrentToggle, false));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_solvelist, container, false);

        mQuickStats = (TextView) v.findViewById(R.id.fragment_solvelist_stats_textview);

        if (mCurrentToggle) {
            mReset = (Button) v.findViewById(R.id.fragment_current_s_details_reset_button);
            mReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PuzzleType.get(mPuzzleTypeDisplayName).resetCurrentSession();
                    onSessionSolvesChanged();
                }
            });
            mSubmit = (Button) v.findViewById(R.id.fragment_current_s_details_submit_button);
            mSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSession.getNumberOfSolves() == 0) {
                        Toast.makeText(getAttachedActivity().getApplicationContext(), getResources().getText(R.string.session_no_solves), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    PuzzleType.get(mPuzzleTypeDisplayName).submitCurrentSession();

                    onSessionSolvesChanged();
                }
            });

        }

        mListView = (ListView) v.findViewById(android.R.id.list);
        mListView.setAdapter(new SolveListAdapter());
        mListView.setEmptyView(v.findViewById(android.R.id.empty));

        //Getting CAB to work API9+: Doctoror Drive's answer - http://stackoverflow.com/questions/14737519/how-can-you-implement-multi-selection-and-contextual-actionmode-in-actionbarsher

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null)
                    onSolveItemClick(mPuzzleTypeDisplayName, mSessionIndex, position);
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    return false;
                }

                mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                mListView.setItemChecked(position, true);
                ((ActionBarActivity) getAttachedActivity()).startSupportActionMode(new SolveListActionModeCallback());
                return true;
            }
        });

        if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_CAB_BOOLEAN)) {
            mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            ((ActionBarActivity) getAttachedActivity()).startSupportActionMode(new SolveListActionModeCallback());
        }

        onSessionSolvesChanged();
        return v;
    }

    public void enableResetSubmitButtons(boolean enable) {
        if (enable) {
            mReset.setVisibility(View.VISIBLE);
            mSubmit.setVisibility(View.VISIBLE);
        } else {
            mReset.setVisibility(View.GONE);
            mSubmit.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        PuzzleType.get(mPuzzleTypeDisplayName).saveHistorySessionsToFile(getAttachedActivity());
    }

    @Override
    public void onSessionSolvesChanged() {
        if (!mCurrentToggle && mSession.getNumberOfSolves() <= 0) {
            PuzzleType.get(mPuzzleTypeDisplayName).deleteHistorySession(mSessionIndex, getAttachedActivity());
            getAttachedActivity().finish();
            return;
        }
        ((SolveListAdapter) mListView.getAdapter()).updateSolvesList();
        updateQuickStats();
        if (mCurrentToggle)
            enableResetSubmitButtons(PuzzleType.get(mPuzzleTypeDisplayName).getCurrentSession().getNumberOfSolves() > 0);
        setShareIntent();
    }

    @Override
    public void onSessionChanged() {
        onSessionSolvesChanged();
    }

    private class SolveListActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            getAttachedActivity().getMenuInflater().inflate(R.menu.context_solve_or_session_list, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            SparseBooleanArray checked;
            switch (item.getItemId()) {
                case R.id.context_solvelist_delete:
                    checked = mListView.getCheckedItemPositions();
                    ArrayList<Solve> toDelete = new ArrayList<Solve>();
                    for (int i = 0; i < checked.size(); i++) {
                        final int index = checked.keyAt(i);
                        if (checked.get(index)) {
                            toDelete.add(mSession.getSolveByPosition(index));
                        }
                    }
                    for (Solve i : toDelete) {
                        mSession.deleteSolve(i);
                    }
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mListView.clearChoices();
            mListView.setChoiceMode(AbsListView.CHOICE_MODE_NONE);
            mActionMode = null;
            onSessionSolvesChanged();
        }
    }

    public class SolveListAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveListAdapter() {
            super(getAttachedActivity(), 0, mSession.getSolves());
            updateSolvesList();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getAttachedActivity().getLayoutInflater().inflate(R.layout.list_item_solvelist, parent, false);
            }
            Solve s = getItem(position);
            TextView time = (TextView) convertView.findViewById(R.id.list_item_solvelist_title_textview);
            TextView desc = (TextView) convertView.findViewById(R.id.list_item_solvelist_desc_textview);


            time.setText("");

            for (Solve a : mBestAndWorstSolves) {
                if (a == s) {
                    time.setText("(" + s.getDescriptiveTimeString() + ")");
                }
            }

            if (time.getText() == "") {
                time.setText(s.getDescriptiveTimeString());
            }

            desc.setText(s.getScrambleAndSvg().scramble);

            return convertView;
        }

        public void updateSolvesList() {
            clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                addAll(mSession.getSolves());
            else {
                for (Solve i : mSession.getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Session.getBestSolve(mSession.getSolves()));
            mBestAndWorstSolves.add(Session.getWorstSolve(mSession.getSolves()));
            notifyDataSetChanged();
        }


    }
}
