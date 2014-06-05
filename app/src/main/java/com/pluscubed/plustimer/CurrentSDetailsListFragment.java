package com.pluscubed.plustimer;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * Session tab
 */
public class CurrentSDetailsListFragment extends ListFragment {

    private TextView mQuickStats;
    private Button mReset;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new SolveListAdapter(PuzzleType.sCurrentPuzzleType));
    }

    public void updateQuickStats() {
        mQuickStats.setText(CurrentSTimerFragment.buildQuickStatsWithAveragesOf(getMainActivity(), 5, 12, 50, 100, 1000));
        if (!CurrentSTimerFragment.buildQuickStatsWithAveragesOf(getMainActivity(), 5, 12, 50, 100, 1000).equals("")) {
            mQuickStats.append("\n");
        }
        mQuickStats.append(getString(R.string.solves) + PuzzleType.sCurrentPuzzleType.getSession().getNumberOfSolves());
    }

    public MainActivity getMainActivity() {
        return ((MainActivity) getParentFragment().getActivity());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        getMainActivity().showCurrentSolveDialog(position);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_current_s_details, container, false);
        mQuickStats = (TextView) v.findViewById(R.id.fragment_current_s_details_stats_textview);
        mReset = (Button) v.findViewById(R.id.fragment_current_s_details_reset_button);
        mReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PuzzleType.sCurrentPuzzleType.resetSession();
                ((CurrentSFragment) getParentFragment()).updateFragments();
            }
        });
        updateSession();
        return v;
    }

    public void updateSession() {
        ((SolveListAdapter) getListAdapter()).updateSolvesList(PuzzleType.sCurrentPuzzleType);
        updateQuickStats();
    }

    public class SolveListAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveListAdapter(PuzzleType currentPuzzleType) {
            super(getMainActivity(), 0, currentPuzzleType.getSession().getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(currentPuzzleType.getSession().getBestSolve(currentPuzzleType.getSession().getSolves()));
            mBestAndWorstSolves.add(currentPuzzleType.getSession().getWorstSolve(currentPuzzleType.getSession().getSolves()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getMainActivity().getLayoutInflater().inflate(R.layout.list_item_current_s_details_solve, parent, false);
            }
            Solve s = getItem(position);
            TextView time = (TextView) convertView.findViewById(R.id.list_item_current_s_details_solve_title_textview);
            TextView desc = (TextView) convertView.findViewById(R.id.list_item_current_s_details_solve_desc_textview);


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
                addAll(puzzleType.getSession().getSolves());
            else {
                for (Solve i : puzzleType.getSession().getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(puzzleType.getSession().getBestSolve(puzzleType.getSession().getSolves()));
            mBestAndWorstSolves.add(puzzleType.getSession().getWorstSolve(puzzleType.getSession().getSolves()));
            notifyDataSetChanged();
        }


    }
}
