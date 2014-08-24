package com.pluscubed.plustimer.ui;

import android.app.ListFragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * History SessionList Fragment
 */

public class HistorySessionListFragment extends ListFragment {

    private static final String STATE_PUZZLETYPE_DISPLAYNAME = "puzzletype_displayname";

    private String mPuzzleTypeDisplayName;

    private TextView mStatsText;
    private GraphView mGraph;

    private ActionMode mActionMode;

    @Override
    public void onPause() {
        super.onPause();
        PuzzleType.get(mPuzzleTypeDisplayName).saveHistorySessionsToFile(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PUZZLETYPE_DISPLAYNAME, mPuzzleTypeDisplayName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mPuzzleTypeDisplayName = savedInstanceState.getString(STATE_PUZZLETYPE_DISPLAYNAME);
        } else {
            mPuzzleTypeDisplayName = PuzzleType.CURRENT;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {

            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu.context_solve_or_session_list, menu);
                mActionMode = mode;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.context_solvelist_delete_menuitem:
                        for (int i = getListView().getCount() - 1; i >= 0; i--) {
                            if (getListView().isItemChecked(i)) {
                                PuzzleType.get(mPuzzleTypeDisplayName).deleteHistorySession((Session) getListView().getItemAtPosition(i), getActivity());
                            }
                        }
                        mode.finish();
                        onSessionListChanged();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
            }
        });
        LinearLayout headerView = (LinearLayout) getActivity().getLayoutInflater().inflate(R.layout.history_sessionlist_header, getListView(), false);
        mStatsText = (TextView) headerView.findViewById(R.id.history_sessionlist_header_stats_textview);
        mGraph = new LineGraphView(getActivity(), "");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) convertDpToPx(220));
        layoutParams.setMargins(0, (int) convertDpToPx(8), 0, (int) convertDpToPx(8));
        mGraph.setLayoutParams(layoutParams);
        mGraph.setShowLegend(true);
        mGraph.getGraphViewStyle().setLegendWidth((int) convertDpToPx(70));
        headerView.addView(mGraph);
        getListView().addHeaderView(headerView, null, false);
        try {
            setListAdapter(new SessionListAdapter());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public float convertDpToPx(float dp) {
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public void onSessionListChanged() {
        updateStats();
        ((SessionListAdapter) getListAdapter()).onSessionListChanged();
    }

    public void updateStats() {
        List<Session> historySessions = PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity());
        if (historySessions.size() > 0) {
            StringBuilder s = new StringBuilder();

            //Get best solves of each history session and add to list
            ArrayList<Solve> bestSolvesOfSessions = new ArrayList<Solve>();
            for (Session session : historySessions) {
                bestSolvesOfSessions.add(Session.getBestSolve(session.getSolves()));
            }

            //Add PB of all historySessions
            s.append(getString(R.string.pb)).append(": ").append(Session.getBestSolve(bestSolvesOfSessions).getDescriptiveTimeString());

            //Add PB of Ao5,12,50,100,1000
            s.append(getBestAverageOfSessions(new int[]{1000, 100, 50, 12, 5}, historySessions));

            mStatsText.setText(s.toString());

            if (bestSolvesOfSessions.size() > 1) {
                ArrayList<Solve> bestSolvesOfSessionsNoDnf = new ArrayList<Solve>();
                for (Session session : historySessions) {
                    if (Session.getBestSolve(session.getSolves()).getPenalty() != Solve.Penalty.DNF) {
                        bestSolvesOfSessionsNoDnf.add(Session.getBestSolve(session.getSolves()));
                    }
                }
                mGraph.setVisibility(View.VISIBLE);

                GraphView.GraphViewData[] bestTimes = new GraphView.GraphViewData[bestSolvesOfSessionsNoDnf.size()];
                for (int i = 0; i < bestSolvesOfSessionsNoDnf.size(); i++) {
                    bestTimes[i] = new GraphView.GraphViewData(bestSolvesOfSessionsNoDnf.get(i).getTimestamp(), bestSolvesOfSessionsNoDnf.get(i).getTimeTwo());
                }
                GraphViewSeries bestTimesSeries = new GraphViewSeries("Best Times", new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, 5), bestTimes);

                mGraph.setCustomLabelFormatter(new CustomLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return Solve.timeDateStringFromTimestamp(getActivity().getApplicationContext(), (long) value);
                        } else {
                            return Solve.timeStringFromLong((long) value);
                        }

                    }
                });

                //Set bounds for Y
                Solve bestSolve = Session.getBestSolve(bestSolvesOfSessions);
                mGraph.setManualYMinBound(bestSolve.getTimeTwo() - bestSolve.getTimeTwo() * 0.1 >= 0 ? bestSolve.getTimeTwo() - bestSolve.getTimeTwo() * 0.1 : 0);
                Solve worstSolve = Session.getWorstSolve(bestSolvesOfSessions);
                mGraph.setManualYMaxBound(worstSolve.getTimeTwo() + bestSolve.getTimeTwo() * 0.1);

                //Set bounds for X
                long firstTimestamp = bestSolvesOfSessionsNoDnf.get(0).getTimestamp();
                long lastTimestamp = bestSolvesOfSessionsNoDnf.get(bestSolvesOfSessionsNoDnf.size() - 1).getTimestamp();
                mGraph.setViewPort(firstTimestamp - firstTimestamp * 0.000005, (lastTimestamp + firstTimestamp * 0.000005) - (firstTimestamp - firstTimestamp * 0.000005));

                //Remove all series since we're adding one
                mGraph.removeAllSeries();
                mGraph.addSeries(bestTimesSeries);
            } else {
                mGraph.setVisibility(View.GONE);
            }
        }
    }

    public String getBestAverageOfSessions(int[] numbers, List<Session> sessions) {
        StringBuilder builder = new StringBuilder();
        for (int number : numbers) {
            ArrayList<Long> bestAverages = new ArrayList<Long>();
            if (sessions.size() > 0) {
                for (Session session : sessions) {
                    long bestAverage = session.getBestAverageOf(number);
                    if (bestAverage != Session.GET_AVERAGE_INVALID) bestAverages.add(bestAverage);
                }
                if (bestAverages.size() > 0) {
                    Long bestAverage = Collections.min(bestAverages);
                    builder.append("\n").append(getString(R.string.pb)).append(" ").append(getString(R.string.ao)).append(number).append(": ").append(bestAverage == Long.MAX_VALUE ? "DNF" : Solve.timeStringFromLong(bestAverage));
                }
            }
        }
        return builder.toString();
    }

    public void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_history_sessionlist, menu);
        final Spinner menuPuzzleSpinner = (Spinner) menu.findItem(R.id.menu_activity_history_sessionlist_puzzletype_spinner).getActionView();

        final ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                new ArrayAdapter<PuzzleType>(
                        getActivity().getActionBar().getThemedContext(),
                        0,
                        PuzzleType.values()
                ) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null) {
                            convertView = getActivity().getLayoutInflater().inflate(R.layout.spinner_item, parent, false);
                        }
                        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
                        textView.setText(getItem(position).toString());
                        textView.setTextColor(Color.WHITE);
                        return convertView;
                    }
                };


        puzzleTypeSpinnerAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
        menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(PuzzleType.get(mPuzzleTypeDisplayName)), true);
        menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (menuPuzzleSpinner.getSelectedItemPosition() != puzzleTypeSpinnerAdapter.getPosition(PuzzleType.get(mPuzzleTypeDisplayName))) {
                    mPuzzleTypeDisplayName = (parent.getItemAtPosition(position)).toString();
                    onSessionListChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_sessionlist, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Update list when session is deleted in HistorySolveList
        onSessionListChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getActivity(), HistorySolveListActivity.class);
        int index = PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity()).indexOf(getListView().getItemAtPosition(position));
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_SESSION_POSITION, index);
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME, mPuzzleTypeDisplayName);
        startActivity(i);
    }

    public class SessionListAdapter extends ArrayAdapter<Session> {
        SessionListAdapter() throws IOException {
            super(getActivity(), 0, new ArrayList<Session>());
            onSessionListChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_history_sessionlist, parent, false);
            }
            Session session = getItem(position);
            TextView text = (TextView) convertView.findViewById(R.id.list_item_history_sessionlist_textview);
            text.setText(session.getTimestampStringOfLastSolve(getActivity()));

            return convertView;
        }

        public void onSessionListChanged() {
            clear();
            List<Session> sessions = PuzzleType.get(mPuzzleTypeDisplayName).getHistorySessions(getActivity());
            Collections.reverse(sessions);
            addAll(sessions);
            notifyDataSetChanged();
        }

    }

}
