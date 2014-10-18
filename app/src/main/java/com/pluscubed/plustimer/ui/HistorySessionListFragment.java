package com.pluscubed.plustimer.ui;

import android.app.ListFragment;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.SparseArray;
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
import com.pluscubed.plustimer.Util;
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

    private String mPuzzleTypeName;

    private TextView mStatsText;

    private LineGraphView mGraph;

    private ActionMode mActionMode;

    private boolean mMillisecondsEnabled;

    @Override
    public void onPause() {
        super.onPause();
        PuzzleType.valueOf(mPuzzleTypeName).getHistorySessions().save(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PUZZLETYPE_DISPLAYNAME, mPuzzleTypeName);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        PuzzleType.initialize(getActivity());
        if (savedInstanceState != null) {
            mPuzzleTypeName = savedInstanceState.getString(STATE_PUZZLETYPE_DISPLAYNAME);
        } else {
            mPuzzleTypeName = PuzzleType.getCurrent().name();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSharedPrefs();
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                                  boolean checked) {

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
                                PuzzleType.valueOf(mPuzzleTypeName).getHistorySessions()
                                        .deleteSession((Session) getListView().getItemAtPosition(i),
                                                getActivity());
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
        LinearLayout headerView = (LinearLayout) getActivity().getLayoutInflater()
                .inflate(R.layout.history_sessionlist_header, getListView(), false);
        mStatsText = (TextView) headerView
                .findViewById(R.id.history_sessionlist_header_stats_textview);
        mGraph = new LineGraphView(getActivity(), "");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) convertDpToPx(220));
        layoutParams.setMargins(0, (int) convertDpToPx(8), 0, (int) convertDpToPx(8));
        mGraph.setLayoutParams(layoutParams);
        mGraph.setShowLegend(true);
        mGraph.getGraphViewStyle().setLegendWidth((int) convertDpToPx(85));
        mGraph.getGraphViewStyle().setLegendMarginBottom((int) convertDpToPx(12));
        mGraph.setLegendAlign(GraphView.LegendAlign.BOTTOM);
        mGraph.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return Util.timeDateStringFromTimestamp(getActivity().getApplicationContext(), (long) value);
                } else {
                    return Util.timeStringFromNs((long) value, mMillisecondsEnabled);
                }

            }
        });
        mGraph.setDrawDataPoints(true);
        mGraph.setDataPointsRadius(convertDpToPx(3));
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
        List<Session> historySessions = PuzzleType.valueOf(mPuzzleTypeName).getHistorySessions().getList();
        if (historySessions.size() > 0) {
            StringBuilder s = new StringBuilder();

            //Get best solves of each history session and add to list
            ArrayList<Solve> bestSolvesOfSessionsArray = new ArrayList<Solve>();
            for (Session session : historySessions) {
                bestSolvesOfSessionsArray.add(Util.getBestSolveOfList(session.getSolves()));
            }

            //Add PB of all historySessions
            s.append(getString(R.string.pb)).append(": ")
                    .append(Util.getBestSolveOfList(bestSolvesOfSessionsArray)
                            .getTimeString(mMillisecondsEnabled));

            //Add PB of Ao5,12,50,100,1000
            s.append(getBestAverageOfNumberOfSessions(new int[]{1000, 100, 50, 12, 5},
                    historySessions));

            mStatsText.setText(s.toString());

            //Get the timestamps of each session, and put in a SparseArray
            ArrayList<Long> sessionTimestamps = new ArrayList<Long>();
            for (Session session : historySessions) {
                sessionTimestamps.add(session.getTimestamp());
            }

            //This SparseArray contains any number of SparseArray<Long>, one for each average (5,12,etc)
            SparseArray<SparseArray<Long>> bestAverageMatrix = new SparseArray<SparseArray<Long>>();
            for (int averageNumber : new int[]{5, 12, 50, 100, 1000}) {
                SparseArray<Long> timesSparseArray = new SparseArray<Long>();
                for (int i = 0; i < historySessions.size(); i++) {
                    Session session = historySessions.get(i);
                    if (session.getNumberOfSolves() >= averageNumber) {
                        long bestAverage = session.getBestAverageOf(averageNumber);
                        if (bestAverage != Long.MAX_VALUE
                                && bestAverage != Session.GET_AVERAGE_INVALID_NOT_ENOUGH) {
                            timesSparseArray.put(i, bestAverage);
                        }
                    }
                }
                if (timesSparseArray.size() > 0) {
                    bestAverageMatrix.put(averageNumber, timesSparseArray);
                }

            }

            ArrayList<GraphViewSeries> bestAverageGraphViewSeries
                    = new ArrayList<GraphViewSeries>();
            for (int i = 0; i < bestAverageMatrix.size(); i++) {
                SparseArray<Long> averageArray = bestAverageMatrix.valueAt(i);
                if (averageArray.size() > 0) {
                    GraphView.GraphViewData[] bestTimesDataArray
                            = new GraphView.GraphViewData[averageArray.size()];
                    for (int k = 0; k < averageArray.size(); k++) {
                        bestTimesDataArray[k] = new GraphView.GraphViewData(
                                sessionTimestamps.get(averageArray.keyAt(k)),
                                averageArray.valueAt(k));
                    }
                    int lineColor = Color.RED;
                    switch (bestAverageMatrix.keyAt(i)) {
                        case 5:
                            lineColor = Color.RED;
                            break;
                        case 12:
                            lineColor = Color.GREEN;
                            break;
                        case 100:
                            lineColor = Color.BLACK;
                            break;
                        case 1000:
                            lineColor = Color.YELLOW;
                    }
                    bestAverageGraphViewSeries.add(new GraphViewSeries(
                            String.format(getString(R.string.best_ao), bestAverageMatrix.keyAt(i)),
                            new GraphViewSeries.GraphViewSeriesStyle(lineColor,
                                    (int) convertDpToPx(2)), bestTimesDataArray));
                }
            }

            //Get best times of each session excluding DNF, and create GraphViewData array bestTimes
            SparseArray<Long> bestSolvesTimes = new SparseArray<Long>();
            for (int i = 0; i < historySessions.size(); i++) {
                Session session = historySessions.get(i);
                if (Util.getBestSolveOfList(session.getSolves()).getPenalty()
                        != Solve.Penalty.DNF) {
                    bestSolvesTimes
                            .put(i, Util.getBestSolveOfList(session.getSolves()).getTimeTwo());
                }
            }
            GraphView.GraphViewData[] bestTimesDataArray
                    = new GraphView.GraphViewData[bestSolvesTimes.size()];
            for (int i = 0; i < bestSolvesTimes.size(); i++) {
                bestTimesDataArray[i] = new GraphView.GraphViewData(
                        sessionTimestamps.get(bestSolvesTimes.keyAt(i)),
                        bestSolvesTimes.valueAt(i));
            }
            GraphViewSeries bestTimesSeries = new GraphViewSeries(getString(R.string.best_times),
                    new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, (int) convertDpToPx(2)),
                    bestTimesDataArray);

            boolean averageMoreThanOne = false;
            for (int i = 0; i < bestAverageMatrix.size(); i++) {
                if (bestAverageMatrix.valueAt(i).size() > 1) {
                    averageMoreThanOne = true;
                }
            }
            if (averageMoreThanOne || bestSolvesTimes.size() > 1) {
                mGraph.setVisibility(View.VISIBLE);
                mGraph.removeAllSeries();
                mGraph.addSeries(bestTimesSeries);
                for (GraphViewSeries averageSeries : bestAverageGraphViewSeries) {
                    mGraph.addSeries(averageSeries);
                }

                ArrayList<Long> allPointsValue = new ArrayList<Long>();
                for (int i = 0; i < bestAverageMatrix.size(); i++) {
                    for (int k = 0; k < bestAverageMatrix.valueAt(i).size(); k++) {
                        allPointsValue.add(bestAverageMatrix.valueAt(i).valueAt(k));
                    }
                }
                for (int i = 0; i < bestSolvesTimes.size(); i++) {
                    allPointsValue.add(bestSolvesTimes.valueAt(i));
                }

                //Set bounds for Y
                long lowestValue = Collections.min(allPointsValue);
                long highestValue = Collections.max(allPointsValue);
                //Check to make sure the minimum bound is more than 0 (if yes, set bound to 0)
                mGraph.setManualYMinBound(
                        lowestValue - (highestValue - lowestValue) * 0.1 >= 0 ? lowestValue
                                - (highestValue - lowestValue) * 0.1 : 0);
                mGraph.setManualYMaxBound(highestValue + (highestValue - lowestValue) * 0.1);

                long firstTimestamp = Long.MAX_VALUE;
                for (int i = 0; i < bestAverageMatrix.size(); i++) {
                    if (sessionTimestamps.get(bestAverageMatrix.valueAt(i).keyAt(0))
                            < firstTimestamp) {
                        firstTimestamp = sessionTimestamps
                                .get(bestAverageMatrix.valueAt(i).keyAt(0));
                    }
                }
                if (sessionTimestamps.get(bestSolvesTimes.keyAt(0)) < firstTimestamp) {
                    firstTimestamp = sessionTimestamps.get(bestSolvesTimes.keyAt(0));
                }

                //Set bounds for X

                long lastTimestamp = Long.MIN_VALUE;
                for (int i = 0; i < bestAverageMatrix.size(); i++) {
                    if (sessionTimestamps.get(bestAverageMatrix.valueAt(i)
                            .keyAt(bestAverageMatrix.valueAt(i).size() - 1)) > lastTimestamp) {
                        lastTimestamp = sessionTimestamps.get(bestAverageMatrix.valueAt(i)
                                .keyAt(bestAverageMatrix.valueAt(i).size() - 1));
                    }
                }
                if (sessionTimestamps.get(bestSolvesTimes.keyAt(bestSolvesTimes.size() - 1))
                        > lastTimestamp) {
                    lastTimestamp = sessionTimestamps
                            .get(bestSolvesTimes.keyAt(bestSolvesTimes.size() - 1));
                }
                mGraph.setViewPort(firstTimestamp - (lastTimestamp - firstTimestamp) * 0.1,
                        (lastTimestamp + (lastTimestamp - firstTimestamp) * 0.1) - (firstTimestamp
                                - (lastTimestamp - firstTimestamp) * 0.1));
            } else {
                mGraph.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Returns string with best averages of [numbers].
     *
     * @param numbers  the numbers for the averages
     * @param sessions list of sessions
     * @return String with the best averages of [numbers]
     */
    public String getBestAverageOfNumberOfSessions(int[] numbers, List<Session> sessions) {
        StringBuilder builder = new StringBuilder();
        for (int number : numbers) {
            ArrayList<Long> bestAverages = new ArrayList<Long>();
            if (sessions.size() > 0) {
                for (Session session : sessions) {
                    long bestAverage = session.getBestAverageOf(number);
                    if (bestAverage != Session.GET_AVERAGE_INVALID_NOT_ENOUGH) {
                        //If the average is possible for the number
                        bestAverages.add(bestAverage);
                    }
                }
                if (bestAverages.size() > 0) {
                    Long bestAverage = Collections.min(bestAverages);
                    builder.append("\n").append(getString(R.string.pb)).append(" ")
                            .append(String.format(getString(R.string.ao), number)).append(": ")
                            .append(bestAverage == Long.MAX_VALUE ? "DNF"
                                    : Util.timeStringFromNs(bestAverage, mMillisecondsEnabled));
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

        Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.menu_activity_history_sessionlist_puzzletype_spinner));
        ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter = new SpinnerPuzzleTypeAdapter(getActivity().getLayoutInflater(), ((ActionBarActivity) getActivity()).getSupportActionBar().getThemedContext());
        menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
        menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter.getPosition(PuzzleType.valueOf(mPuzzleTypeName)), true);
        menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPuzzleTypeName = (parent.getItemAtPosition(position)).toString();
                onSessionListChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_sessionlist, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Update list when session is deleted in HistorySolveList
        onSessionListChanged();
        //update puzzle spinner in case settings were changed
        getActivity().invalidateOptionsMenu();
        initSharedPrefs();
    }

    private void initSharedPrefs() {
        mMillisecondsEnabled = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SettingsActivity.PREF_MILLISECONDS_CHECKBOX, true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getActivity(), HistorySolveListActivity.class);
        int index = PuzzleType.valueOf(mPuzzleTypeName).getHistorySessions().getList().indexOf(l.getItemAtPosition(position));
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_SESSION_POSITION, index);
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_PUZZLETYPE_DISPLAYNAME, mPuzzleTypeName);
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
            text.setText(session.getTimestampString(getActivity()));

            return convertView;
        }

        public void onSessionListChanged() {
            clear();
            List<Session> sessions = PuzzleType.valueOf(mPuzzleTypeName).getHistorySessions().getList();
            Collections.reverse(sessions);
            addAll(sessions);
            notifyDataSetChanged();
        }

    }

}
