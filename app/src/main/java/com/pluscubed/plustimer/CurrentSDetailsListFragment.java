package com.pluscubed.plustimer;

import android.content.Context;
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
import java.util.Arrays;
import java.util.Collections;


/**
 * Session tab
 */
public class CurrentSDetailsListFragment extends CurrentSBaseFragment {

    private TextView mQuickStats;
    private ListView mListView;

    public static String buildAdvancedStatsWithAveragesOf(Context context, Integer... currentAverages) {
        Arrays.sort(currentAverages, Collections.reverseOrder());
        String s = "";
        for (int i : currentAverages) {
            if (PuzzleType.sCurrentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getNumberOfSolves() >= i) {
                s += context.getString(R.string.cao) + i + ": " + PuzzleType.sCurrentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getStringCurrentAverageOf(i) + "\n";
                s += context.getString(R.string.bao) + i + ": " + PuzzleType.sCurrentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getStringBestAverageOf(i) + "\n";
            }
        }
        if (PuzzleType.sCurrentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getNumberOfSolves() > 0) {
            s += context.getString(R.string.mean) + PuzzleType.sCurrentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getStringMean();
        }
        return s;
    }

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
        mQuickStats.setText(buildAdvancedStatsWithAveragesOf(getAttachedActivity(), 5, 12, 50, 100, 1000));
        if (!buildAdvancedStatsWithAveragesOf(getAttachedActivity(), 5, 12, 50, 100, 1000).equals("")) {
            mQuickStats.append("\n");
        }
        mQuickStats.append(getString(R.string.solves) + PuzzleType.sCurrentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getNumberOfSolves());
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
            super(getAttachedActivity(), 0, currentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getSolves());
            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Session.getBestSolve(currentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getSolves()));
            mBestAndWorstSolves.add(Session.getWorstSolve(currentPuzzleType.getSession(PuzzleType.CURRENT_SESSION).getSolves()));
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
                addAll(puzzleType.getSession(PuzzleType.CURRENT_SESSION).getSolves());
            else {
                for (Solve i : puzzleType.getSession(PuzzleType.CURRENT_SESSION).getSolves()) {
                    add(i);
                }
            }

            mBestAndWorstSolves = new ArrayList<Solve>();
            mBestAndWorstSolves.add(Session.getBestSolve(puzzleType.getSession(PuzzleType.CURRENT_SESSION).getSolves()));
            mBestAndWorstSolves.add(Session.getWorstSolve(puzzleType.getSession(PuzzleType.CURRENT_SESSION).getSolves()));
            notifyDataSetChanged();
        }


    }
}
