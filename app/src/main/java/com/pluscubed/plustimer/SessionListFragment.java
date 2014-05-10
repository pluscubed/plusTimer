package com.pluscubed.plustimer;

import android.content.Intent;
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

import java.sql.Time;
import java.util.ArrayList;


/**
 * Session tab
 */
public class SessionListFragment extends ListFragment {

    private TextView mQuickStats;
    private Button mReset;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setListAdapter(new SolveListAdapter(PuzzleType.sCurrentPuzzleType));
    }

    public void updateQuickStats() {
        mQuickStats.setText(TimerFragment.buildQuickStatsWithAveragesOf(getActivity(), 5, 12, 50, 100, 1000));
        if(!TimerFragment.buildQuickStatsWithAveragesOf(getActivity(), 5, 12, 50, 100, 1000).equals("")){
            mQuickStats.append("\n");
        }
        mQuickStats.append(getString(R.string.solves) + PuzzleType.sCurrentPuzzleType.getSession().getNumberOfSolves());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TimerFragment.DIALOG_REQUEST_CODE) {
            Solve solve = PuzzleType.sCurrentPuzzleType.getSession().getSolveByPosition(data.getIntExtra(TimerFragment.EXTRA_DIALOG_FINISH_SOLVE_INDEX, 0));
            switch (data.getIntExtra(TimerFragment.EXTRA_DIALOG_FINISH_SELECTION, 0)) {
                case TimerFragment.DIALOG_PENALTY_NONE:
                    solve.setPenalty(Solve.Penalty.NONE);
                    break;
                case TimerFragment.DIALOG_PENALTY_PLUSTWO:
                    solve.setPenalty(Solve.Penalty.PLUSTWO);
                    break;
                case TimerFragment.DIALOG_PENALTY_DNF:
                    solve.setPenalty(Solve.Penalty.DNF);
                    break;
                case TimerFragment.DIALOG_RESULT_DELETE:
                    PuzzleType.sCurrentPuzzleType.getSession().deleteSolve(data.getIntExtra(TimerFragment.EXTRA_DIALOG_FINISH_SOLVE_INDEX, 0));
                    break;
            }
        }
        updateQuickStats();
        ((SolveListAdapter)getListAdapter()).updateSolvesList(PuzzleType.sCurrentPuzzleType);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        int penalty;
        switch (((Solve) getListAdapter().getItem(position)).getPenalty()) {
            case DNF:
                penalty = TimerFragment.DIALOG_PENALTY_DNF;
                break;
            case PLUSTWO:
                penalty = TimerFragment.DIALOG_PENALTY_PLUSTWO;
                break;
            case NONE:
            default:
                penalty = TimerFragment.DIALOG_PENALTY_NONE;
        }
        SolveQuickModifyDialog d = SolveQuickModifyDialog.newInstance((Solve) getListAdapter().getItem(position), position, penalty);
        d.setTargetFragment(this, TimerFragment.DIALOG_REQUEST_CODE);
        d.show(getActivity().getSupportFragmentManager(), TimerFragment.DIALOG_FRAGMENT_TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_session, container, false);
        mQuickStats = (TextView) v.findViewById(R.id.session_quickstats);
        updateQuickStats();
        mReset = (Button) v.findViewById(R.id.session_reset);
        mReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PuzzleType.sCurrentPuzzleType.resetSession();
                ((SolveListAdapter) getListAdapter()).updateSolvesList(PuzzleType.sCurrentPuzzleType);
                updateQuickStats();
            }
        });
        ((SolveListAdapter) getListAdapter()).updateSolvesList(PuzzleType.sCurrentPuzzleType);
        updateQuickStats();
        return v;
    }

    public class SolveListAdapter extends ArrayAdapter<Solve> {

        private ArrayList<Solve> mBestAndWorstSolves;

        public SolveListAdapter(PuzzleType currentPuzzleType) {
            super(getActivity(), 0, currentPuzzleType.getSession().getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(currentPuzzleType.getSession().getBestSolve(currentPuzzleType.getSession().getSolves()));
            mBestAndWorstSolves.add(currentPuzzleType.getSession().getWorstSolve(currentPuzzleType.getSession().getSolves()));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_sessionlist_solve, parent, false);
            }
            Solve s = getItem(position);
            TextView time = (TextView) convertView.findViewById(R.id.session_list_item_title);
            TextView desc = (TextView) convertView.findViewById(R.id.session_list_item_desc);


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
