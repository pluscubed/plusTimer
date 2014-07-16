package com.pluscubed.plustimer;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * History session fragment
 */
public class HistorySessionFragment extends ListFragment {
    public static final String ARG_HISTORY_SESSION_POSITION = "com.pluscubed.plustimer.history_session_position";
    private TextView mQuickStats;
    private int mPosition;

    public static HistorySessionFragment newInstance(int position) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_HISTORY_SESSION_POSITION, position);

        HistorySessionFragment fragment = new HistorySessionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    //Generate string with specified current averages and mean of current session
    private String buildStatsWithAveragesOf(Context context, Integer... currentAverages) throws IOException {
        Arrays.sort(currentAverages, Collections.reverseOrder());
        String s = "";
        for (int i : currentAverages) {
            if (PuzzleType.sCurrentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getNumberOfSolves() >= i) {
                s += context.getString(R.string.ao) + i + ": " + PuzzleType.sCurrentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getStringCurrentAverageOf(i) + "\n";
            }
        }
        if (PuzzleType.sCurrentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getNumberOfSolves() > 0) {
            s += context.getString(R.string.mean) + PuzzleType.sCurrentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getStringMean();
        }
        return s;
    }

    public void updateQuickStats() {
        try {
            mQuickStats.setText(buildStatsWithAveragesOf(getActivity(), 5, 12, 50, 100, 1000));
            if (!buildStatsWithAveragesOf(getActivity(), 5, 12, 50, 100, 1000).equals("")) {
                mQuickStats.append("\n");
            }
            mQuickStats.append(getString(R.string.solves) + PuzzleType.sCurrentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getNumberOfSolves());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_session_list, container, false);
        mQuickStats = (TextView) v.findViewById(R.id.fragment_session_list_stats_textview);
        try {
            setListAdapter(new SolveListAdapter(PuzzleType.sCurrentPuzzleType));
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateQuickStats();
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mPosition = getArguments().getInt(ARG_HISTORY_SESSION_POSITION);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (NavUtils.getParentActivityName(getActivity()) != null) {
                    NavUtils.navigateUpFromSameTask(getActivity());
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class SolveListAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveListAdapter(PuzzleType currentPuzzleType) throws IOException {
            super(getActivity(), 0, currentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(currentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getBestSolve(currentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getSolves()));
            mBestAndWorstSolves.add(currentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getWorstSolve(currentPuzzleType.getHistorySessions(getActivity()).get(mPosition).getSolves()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_session_list_solve, parent, false);
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

        public void updateSolvesList(PuzzleType puzzleType) throws IOException {
            clear();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                addAll(puzzleType.getHistorySessions(getActivity()).get(mPosition).getSolves());
            else {
                for (Solve i : puzzleType.getHistorySessions(getActivity()).get(mPosition).getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(puzzleType.getHistorySessions(getActivity()).get(mPosition).getBestSolve(puzzleType.getCurrentSession().getSolves()));
            mBestAndWorstSolves.add(puzzleType.getHistorySessions(getActivity()).get(mPosition).getWorstSolve(puzzleType.getCurrentSession().getSolves()));
            notifyDataSetChanged();
        }


    }
}
