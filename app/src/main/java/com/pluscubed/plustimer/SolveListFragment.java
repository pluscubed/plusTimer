package com.pluscubed.plustimer;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


/**
 * Session tab
 */
public class SolveListFragment extends CurrentSBaseFragment {

    public static final String ARG_PUZZLETYPE_DISPLAYNAME = "com.pluscubed.plustimer.solvelist.display_name";
    public static final String ARG_SESSION_POSITION = "com.pluscubed.plustimer.solvelist.session_position";
    public static final String ARG_CURRENT_BOOLEAN = "com.pluscubed.plustimer.solvelist.current_boolean";

    private TextView mQuickStats;
    private ListView mListView;

    private int mSessionIndex;
    private String mPuzzleTypeDisplayName;
    private boolean mCurrentToggle;

    private Button mReset;
    private Button mSubmit;


    public static SolveListFragment newInstance(boolean current, String displayName, int sessionIndex) {
        SolveListFragment f = new SolveListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SESSION_POSITION, sessionIndex);
        args.putString(ARG_PUZZLETYPE_DISPLAYNAME, displayName);
        args.putBoolean(ARG_CURRENT_BOOLEAN, current);
        f.setArguments(args);
        return f;
    }

    public String buildAdvancedStatsWithAveragesOf(Context context, Integer... currentAverages) {
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mCurrentToggle) {
            inflater.inflate(R.menu.menu_current_s_detailslist, menu);
            setUpPuzzleSpinner(menu);
        } else {
            inflater.inflate(R.menu.menu_history_solvelist, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_solvelist_share:
                share();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, PuzzleType.get(mPuzzleTypeDisplayName).toSessionText(getAttachedActivity(), PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()), mCurrentToggle));
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_dialog_title)));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentToggle = getArguments().getBoolean(ARG_CURRENT_BOOLEAN);
        mPuzzleTypeDisplayName = getArguments().getString(ARG_PUZZLETYPE_DISPLAYNAME);
        mSessionIndex = getArguments().getInt(ARG_SESSION_POSITION);
        setHasOptionsMenu(true);
    }

    public void updateQuickStats() {
        mQuickStats.setText(buildAdvancedStatsWithAveragesOf(getAttachedActivity(), 5, 12, 50, 100, 1000));
        if (!buildAdvancedStatsWithAveragesOf(getAttachedActivity(), 5, 12, 50, 100, 1000).equals("")) {
            mQuickStats.append("\n");
        }
        mQuickStats.append(getString(R.string.solves) + PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getNumberOfSolves());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_solvelist, container, false);

        mQuickStats = (TextView) v.findViewById(R.id.fragment_session_list_stats_textview);

        if (mCurrentToggle) {
            mReset = (Button) v.findViewById(R.id.fragment_current_s_details_reset_button);
            mReset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PuzzleType.get(PuzzleType.CURRENT).resetCurrentSession();
                    onSessionSolvesChanged();
                }
            });
            mSubmit = (Button) v.findViewById(R.id.fragment_current_s_details_submit_button);
            mSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PuzzleType.get(PuzzleType.CURRENT).getCurrentSession().getNumberOfSolves() == 0) {
                        Toast.makeText(getAttachedActivity().getApplicationContext(), getResources().getText(R.string.session_no_solves), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        PuzzleType.get(PuzzleType.CURRENT).submitCurrentSession(getAttachedActivity());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    onSessionSolvesChanged();
                }
            });

        }

        mListView = (ListView) v.findViewById(android.R.id.list);
        mListView.setAdapter(new SolveListAdapter());
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onSolveItemClick(mPuzzleTypeDisplayName, mSessionIndex, position);
            }
        });
        mListView.setEmptyView(v.findViewById(android.R.id.empty));
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
    public void onSessionSolvesChanged() {
        ((SolveListAdapter) mListView.getAdapter()).updateSolvesList();
        updateQuickStats();
        if (mCurrentToggle)
            enableResetSubmitButtons(PuzzleType.get(PuzzleType.CURRENT).getCurrentSession().getNumberOfSolves() > 0);
    }

    @Override
    public void onSessionChanged() {
        onSessionSolvesChanged();
    }

    public class SolveListAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveListAdapter() {
            super(getAttachedActivity(), 0, PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getSolves());
            updateSolvesList();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getAttachedActivity().getLayoutInflater().inflate(R.layout.list_item_session_list_solve, parent, false);
            }
            Solve s = getItem(position);
            TextView time = (TextView) convertView.findViewById(R.id.list_item_session_list_solve_title_textview);
            TextView desc = (TextView) convertView.findViewById(R.id.list_item_session_list_solve_desc_textview);


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
                addAll(PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getSolves());
            else {
                for (Solve i : PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Session.getBestSolve(PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getSolves()));
            mBestAndWorstSolves.add(Session.getWorstSolve(PuzzleType.get(mPuzzleTypeDisplayName).getSession(mSessionIndex, getAttachedActivity()).getSolves()));
            notifyDataSetChanged();
        }


    }
}
