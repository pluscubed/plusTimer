package com.pluscubed.plustimer;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Session tab
 */
public class CurrentSDetailsListFragment extends CurrentSBaseFragment {

    private TextView mQuickStats;
    private ListView mListView;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_current_s_detailslist, menu);

        setUpPuzzleSpinner(menu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public void updateQuickStats() {
        mQuickStats.setText(CurrentSTimerFragment.buildStatsWithAveragesOf(getAttachedActivity(), 5, 12, 50, 100, 1000));
        if (!CurrentSTimerFragment.buildStatsWithAveragesOf(getAttachedActivity(), 5, 12, 50, 100, 1000).equals("")) {
            mQuickStats.append("\n");
        }
        mQuickStats.append(getString(R.string.solves) + PuzzleType.sCurrentPuzzleType.getCurrentSession().getNumberOfSolves());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_session_list, container, false);
        mQuickStats = (TextView) v.findViewById(R.id.fragment_session_list_stats_textview);
        Button reset = (Button) v.findViewById(R.id.fragment_current_s_details_reset_button);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PuzzleType.sCurrentPuzzleType.resetCurrentSession();
                onSessionSolvesChanged();
            }
        });
        reset.setVisibility(View.VISIBLE);
        Button submit = (Button) v.findViewById(R.id.fragment_current_s_details_submit_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PuzzleType.sCurrentPuzzleType.submitCurrentSession(getAttachedActivity());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                onSessionSolvesChanged();
            }
        });
        submit.setVisibility(View.VISIBLE);
        mListView = (ListView) v.findViewById(android.R.id.list);
        mListView.setAdapter(new SolveListAdapter(PuzzleType.sCurrentPuzzleType));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onSolveItemClick(position);
            }
        });
        onSessionSolvesChanged();
        return v;
    }

    @Override
    public void onSessionSolvesChanged() {
        ((SolveListAdapter) mListView.getAdapter()).updateSolvesList(PuzzleType.sCurrentPuzzleType);
        updateQuickStats();
    }

    @Override
    public void onSessionChanged() {
        onSessionSolvesChanged();
    }

    public class SolveListAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveListAdapter(PuzzleType currentPuzzleType) {
            super(getAttachedActivity(), 0, currentPuzzleType.getCurrentSession().getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(currentPuzzleType.getCurrentSession().getBestSolve(currentPuzzleType.getCurrentSession().getSolves()));
            mBestAndWorstSolves.add(currentPuzzleType.getCurrentSession().getWorstSolve(currentPuzzleType.getCurrentSession().getSolves()));
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

        public void updateSolvesList(PuzzleType puzzleType) {
            clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                addAll(puzzleType.getCurrentSession().getSolves());
            else {
                for (Solve i : puzzleType.getCurrentSession().getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(puzzleType.getCurrentSession().getBestSolve(puzzleType.getCurrentSession().getSolves()));
            mBestAndWorstSolves.add(puzzleType.getCurrentSession().getWorstSolve(puzzleType.getCurrentSession().getSolves()));
            notifyDataSetChanged();
        }


    }
}
