package com.pluscubed.plustimer.ui;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.utils.ErrorUtils;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * History SessionList Fragment
 */

public class HistorySessionListFragment extends ListFragment {

    private static final String STATE_PUZZLETYPE_DISPLAYNAME =
            "puzzletype_displayname";

    private String mPuzzleTypeId;

    private TextView mStatsText;

    private LineGraphView mGraph;

    private ActionMode mActionMode;

    private boolean mMillisecondsEnabled;

    @Override
    public void onPause() {
        super.onPause();
        //PuzzleType.valueOf(mPuzzleTypeId).getHistorySessions().save
        //(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PUZZLETYPE_DISPLAYNAME, mPuzzleTypeId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mPuzzleTypeId = savedInstanceState.getString(STATE_PUZZLETYPE_DISPLAYNAME);
        } else {
            mPuzzleTypeId = PuzzleType.getCurrentId();
        }

        //TODO
        /*PuzzleType.get(mPuzzleTypeId).initializeAllSessionData();*/
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initSharedPrefs();
        getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new AbsListView
                .MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id,
                                                  boolean checked) {

            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                getActivity().getMenuInflater().inflate(R.menu
                        .context_solve_or_session_list, menu);
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
                        for (int i = getListView().getCount() - 1; i >= 0;
                             i--) {
                            if (getListView().isItemChecked(i)) {
                                PuzzleType.get(mPuzzleTypeId).deleteSession(
                                        (Session) getListView().getItemAtPosition(i));
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
        LinearLayout headerView = (LinearLayout) getActivity()
                .getLayoutInflater()
                .inflate(R.layout.history_sessionlist_header, getListView(),
                        false);
        mStatsText = (TextView) headerView
                .findViewById(R.id.history_sessionlist_header_stats_textview);
        mGraph = new LineGraphView(getActivity(), "");
        LinearLayout.LayoutParams layoutParams = new LinearLayout
                .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.convertDpToPx(getActivity(), 220));
        layoutParams.setMargins(0, 0, 0, Utils.convertDpToPx(getActivity(), 20));
        mGraph.setLayoutParams(layoutParams);
        mGraph.setShowLegend(true);
        mGraph.setLegendAlign(GraphView.LegendAlign.TOP);
        mGraph.setCustomLabelFormatter((value, isValueX) -> {
            if (isValueX) {
                return Utils.timeDateStringFromTimestamp(getActivity()
                        .getApplicationContext(), (long) value);
            } else {
                return Utils.timeStringFromNs((long) value,
                        mMillisecondsEnabled);
            }

        });
        mGraph.setDrawDataPoints(true);
        mGraph.setDataPointsRadius(Utils.convertDpToPx(getActivity(), 3));
        headerView.addView(mGraph, 1);
        getListView().addHeaderView(headerView, null, false);
        setListAdapter(new SessionListAdapter());
    }

    void onSessionListChanged() {
        updateStats();
        ((SessionListAdapter) getListAdapter()).onSessionListChanged();
    }

    void updateStats() {
        List<Session> historySessions = PuzzleType.get(mPuzzleTypeId).getSortedHistorySessions();
        if (historySessions.size() > 0) {
            StringBuilder s = new StringBuilder();

            //Get best solves of each history session and add to list
            ArrayList<Solve> bestSolvesOfSessionsArray = new ArrayList<>();
            for (Session session : historySessions) {
                bestSolvesOfSessionsArray.add(Utils.getBestSolveOfList(session
                        .getSolves()));
            }

            //Add PB of all historySessions
            //noinspection ConstantConditions
            s.append(getString(R.string.pb)).append(": ")
                    .append(Utils.getBestSolveOfList(bestSolvesOfSessionsArray)
                            .getTimeString(mMillisecondsEnabled));

            //Add PB of Ao5,12,50,100,1000
            s.append(getBestAverageOfNumberOfSessions(new int[]{1000, 100,
                            50, 12, 5},
                    historySessions));

            mStatsText.setText(s.toString());

            //Get the timestamps of each session, and put in a SparseArray
            ArrayList<Long> sessionTimestamps = new ArrayList<>();
            for (Session session : historySessions) {
                sessionTimestamps.add(session.getTimestamp());
            }

            //This SparseArray contains any number of SparseArray<Long>,
            // one for each average (5,12,etc)
            SparseArray<SparseArray<Long>> bestAverageMatrix = new
                    SparseArray<>();
            for (int averageNumber : new int[]{5, 12, 50, 100, 1000}) {
                SparseArray<Long> timesSparseArray = new SparseArray<>();
                for (int i = 0; i < historySessions.size(); i++) {
                    Session session = historySessions.get(i);
                    //TODO
                    /*if (session.getNumberOfSolves() >= averageNumber) {
                        long bestAverage = session.getBestAverageOf
                                (averageNumber);
                        if (bestAverage != Long.MAX_VALUE
                                && bestAverage != Session
                                .GET_AVERAGE_INVALID_NOT_ENOUGH) {
                            timesSparseArray.put(i, bestAverage);
                        }
                    }*/
                }
                if (timesSparseArray.size() > 0) {
                    bestAverageMatrix.put(averageNumber, timesSparseArray);
                }

            }

            ArrayList<GraphViewSeries> bestAverageGraphViewSeries
                    = new ArrayList<>();
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
                        case 50:
                            lineColor = Color.MAGENTA;
                            break;
                        case 100:
                            lineColor = Color.BLACK;
                            break;
                        case 1000:
                            lineColor = Color.YELLOW;
                    }
                    GraphViewSeries averageSeries = new GraphViewSeries(
                            String.format(getString(R.string.bao), bestAverageMatrix.keyAt(i)),
                            new GraphViewSeries.GraphViewSeriesStyle(lineColor, Utils
                                    .convertDpToPx(getActivity(), 2)),
                            bestTimesDataArray);
                    bestAverageGraphViewSeries.add(averageSeries);
                }
            }

            //Get best times of each session excluding DNF,
            // and create GraphViewData array bestTimes
            SparseArray<Long> bestSolvesTimes = new SparseArray<>();
            for (int i = 0; i < historySessions.size(); i++) {
                Session session = historySessions.get(i);
                //noinspection ConstantConditions
                if (Utils.getBestSolveOfList(session.getSolves()).getPenalty() != Solve.PENALTY_DNF) {
                    //noinspection ConstantConditions
                    bestSolvesTimes
                            .put(i, Utils.getBestSolveOfList(session.getSolves()).getTimeTwo());
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
                    new GraphViewSeries.GraphViewSeriesStyle(Color.BLUE, Utils.convertDpToPx
                            (getActivity(), 2)),
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

                //CALCULATE LONGEST SERIES NAME WIDTH TO SET IN LEGEND
                String bestTimesSeriesName = getString(R.string.best_times);
                TextView textView = new TextView(getActivity());
                textView.setText(bestTimesSeriesName);
                textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int longestSeriesNameWidth = textView.getMeasuredWidth();

                if (bestAverageMatrix.size() > 0) {
                    String longestAverageSeriesName = String.format(getString(R.string.bao),
                            bestAverageMatrix.keyAt(bestAverageMatrix.size() - 1));
                    textView = new TextView(getActivity());
                    textView.setText(longestAverageSeriesName);
                    textView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    if (textView.getMeasuredWidth() > longestSeriesNameWidth) {
                        longestSeriesNameWidth = textView.getMeasuredWidth();
                    }
                }

                mGraph.getGraphViewStyle()
                        .setLegendWidth(longestSeriesNameWidth);

                //---

                ArrayList<Long> allPointsValue = new ArrayList<>();
                for (int i = 0; i < bestAverageMatrix.size(); i++) {
                    for (int k = 0; k < bestAverageMatrix.valueAt(i).size();
                         k++) {
                        allPointsValue.add(bestAverageMatrix.valueAt(i)
                                .valueAt(k));
                    }
                }
                for (int i = 0; i < bestSolvesTimes.size(); i++) {
                    allPointsValue.add(bestSolvesTimes.valueAt(i));
                }

                //Set bounds for Y
                long lowestValue = Collections.min(allPointsValue);
                long highestValue = Collections.max(allPointsValue);
                //Check to make sure the minimum bound is more than 0 (if
                // yes, set bound to 0)
                mGraph.setManualYMinBound(
                        lowestValue - (highestValue - lowestValue) * 0.1 >= 0
                                ? lowestValue
                                - (highestValue - lowestValue) * 0.1 : 0);
                mGraph.setManualYMaxBound(highestValue + (highestValue -
                        lowestValue) * 0.1);

                long firstTimestamp = Long.MAX_VALUE;
                for (int i = 0; i < bestAverageMatrix.size(); i++) {
                    if (sessionTimestamps.get(bestAverageMatrix.valueAt(i)
                            .keyAt(0))
                            < firstTimestamp) {
                        firstTimestamp = sessionTimestamps
                                .get(bestAverageMatrix.valueAt(i).keyAt(0));
                    }
                }
                if (sessionTimestamps.get(bestSolvesTimes.keyAt(0)) <
                        firstTimestamp) {
                    firstTimestamp = sessionTimestamps.get(bestSolvesTimes
                            .keyAt(0));
                }

                //Set bounds for X

                long lastTimestamp = Long.MIN_VALUE;
                for (int i = 0; i < bestAverageMatrix.size(); i++) {
                    if (sessionTimestamps.get(bestAverageMatrix.valueAt(i)
                            .keyAt(bestAverageMatrix.valueAt(i).size() - 1))
                            > lastTimestamp) {
                        lastTimestamp = sessionTimestamps.get
                                (bestAverageMatrix.valueAt(i)
                                        .keyAt(bestAverageMatrix.valueAt(i)
                                                .size() -
                                                1));
                    }
                }
                if (sessionTimestamps.get(bestSolvesTimes.keyAt
                        (bestSolvesTimes.size() - 1))
                        > lastTimestamp) {
                    lastTimestamp = sessionTimestamps
                            .get(bestSolvesTimes.keyAt(bestSolvesTimes.size()
                                    - 1));
                }
                mGraph.setViewPort(
                        firstTimestamp - (lastTimestamp - firstTimestamp) * 0.1,
                        (lastTimestamp + (lastTimestamp - firstTimestamp) * 0.1)
                                - (firstTimestamp
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
    String getBestAverageOfNumberOfSessions(int[] numbers,
                                            List<Session> sessions) {
        StringBuilder builder = new StringBuilder();
        for (int number : numbers) {
            ArrayList<Long> bestAverages = new ArrayList<>();
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
                    builder.append("\n").append(getString(R.string.pb))
                            .append(" ")
                            .append(String.format(getString(R.string.ao),
                                    number)).append(": ")
                            .append(bestAverage == Long.MAX_VALUE ? "DNF"
                                    : Utils.timeStringFromNs(bestAverage,
                                    mMillisecondsEnabled));
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

        Spinner menuPuzzleSpinner = (Spinner) MenuItemCompat.getActionView
                (menu.findItem(R.id
                        .menu_activity_history_sessionlist_puzzletype_spinner));
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            Context themedContext;
            if (activity.getSupportActionBar() != null) {
                themedContext = activity.getSupportActionBar().getThemedContext();
            } else {
                themedContext = activity;
            }
            ArrayAdapter<PuzzleType> puzzleTypeSpinnerAdapter =
                    new SpinnerPuzzleTypeAdapter(
                            getActivity().getLayoutInflater(),
                            themedContext
                    );
            menuPuzzleSpinner.setAdapter(puzzleTypeSpinnerAdapter);
            menuPuzzleSpinner.setSelection(puzzleTypeSpinnerAdapter
                    .getPosition(PuzzleType.get(mPuzzleTypeId)), true);
            menuPuzzleSpinner.setOnItemSelectedListener(new AdapterView
                    .OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    mPuzzleTypeId = (parent.getItemAtPosition(position))
                            .toString();
                    //TODO
                    /*PuzzleType.get(mPuzzleTypeId).initializeAllSessionData();*/
                    onSessionListChanged();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            ErrorUtils.logCrashlytics(new Exception("HistorySessionListFragment onCreateOptionsMenu(): activity is null"));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history_sessionlist,
                container, false);
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
        mMillisecondsEnabled = PrefUtils.isDisplayMillisecondsEnabled(getActivity());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent i = new Intent(getActivity(), HistorySolveListActivity.class);
        int index = PuzzleType.get(mPuzzleTypeId).getSortedHistorySessions()
                .indexOf(l.getItemAtPosition(position));
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_SESSION_ID,
                index);
        i.putExtra(HistorySolveListActivity
                .EXTRA_HISTORY_PUZZLETYPE_ID, mPuzzleTypeId);
        startActivity(i);
    }

    public class SessionListAdapter extends ArrayAdapter<Session> {

        SessionListAdapter() {
            super(getActivity(), 0, new ArrayList<Session>());
            onSessionListChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R
                        .layout.list_item_single_line, parent, false);
            }
            Session session = getItem(position);
            TextView text = (TextView) convertView.findViewById(android.R.id
                    .text1);
            text.setText(session.getTimestampString(getActivity()));

            return convertView;
        }

        public void onSessionListChanged() {
            clear();
            List<Session> sessions = new ArrayList<>(PuzzleType.get(mPuzzleTypeId)
                    .getSortedHistorySessions());
            Collections.reverse(sessions);
            addAll(sessions);
            notifyDataSetChanged();
        }

    }

}
