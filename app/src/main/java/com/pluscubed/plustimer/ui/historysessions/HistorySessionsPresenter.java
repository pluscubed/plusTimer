package com.pluscubed.plustimer.ui.historysessions;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.SparseArray;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.Presenter;
import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.historysolvelist.HistorySolveListActivity;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class HistorySessionsPresenter extends Presenter<HistorySessionsView> {

    private String mPuzzleTypeId;
    private boolean mMillisecondsEnabled;

    @Override
    public void onViewAttached(HistorySessionsView view) {
        super.onViewAttached(view);

        mPuzzleTypeId = PuzzleType.getCurrentId();

        updateAdapter();
    }

    @SuppressWarnings("ConstantConditions")
    public void onSessionClicked(Session session) {
        if (!isViewAttached()) {
            return;
        }

        Intent i = new Intent(getView().getContextCompat(), HistorySolveListActivity.class);
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_SESSION_ID, session.getId());
        i.putExtra(HistorySolveListActivity.EXTRA_HISTORY_PUZZLETYPE_ID, mPuzzleTypeId);
        getView().getContextCompat().startActivity(i);
    }

    /*private void updateAdapter(RecyclerViewUpdate change, Solve solve) {
        if (!isViewAttached()) {
            return;
        }

        Activity context = getView().getContextCompat();



        PuzzleType.get(mPuzzleTypeId).getHistorySessionsSorted(context)
                .flatMap(session -> session.getStatsDeferred(context,
                        mPuzzleTypeId,
                        mIsCurrent,
                        false,
                        PrefUtils.isDisplayMillisecondsEnabled(context),
                        PrefUtils.isSignEnabled(context)
                ))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<String>() {
                    @Override
                    public void onSuccess(String stats) {
                        getView().getSolveListAdapter().notifyChange(change, solve, stats);
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });
    }*/

    @SuppressWarnings("ConstantConditions")
    public void updateAdapter() {
        if (!isViewAttached()) {
            return;
        }

        getView().getHistorySessionsAdapter().setMillisecondsEnabled(mMillisecondsEnabled);

        PuzzleType.get(mPuzzleTypeId).getHistorySessionsSorted(getView().getContextCompat())
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(sessions -> {
                    getView().getHistorySessionsAdapter().setSessions(sessions);
                    getView().getHistorySessionsAdapter().notifyDataSetChanged();
                    getView().showList(true);
                })
                .flatMap(sessions ->
                        updateAdapter(getView().getContextCompat(), sessions)
                                .mergeWith(updateAdapterStatsText(getView().getContextCompat(), sessions))
                                .andThen(Observable.just(sessions))
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(historySessions -> {
                    getView().getHistorySessionsAdapter().notifyHeaderChanged();
                });
    }

    public Completable updateAdapter(Context context, List<Session> historySessions) {

        return Single.<LineData>create(singleSubscriber -> {
            mMillisecondsEnabled = PrefUtils.isDisplayMillisecondsEnabled(context);

            if (historySessions.isEmpty()) {
                singleSubscriber.onSuccess(null);
                return;
            }

            List<Integer> sessionSecondsTimestamps = new ArrayList<>();
            for (Session session : historySessions) {
                long timestamp = session.getTimestamp(context).toBlocking().value();
                sessionSecondsTimestamps.add((int) (timestamp / 1000));
            }

            long firstTimestamp = sessionSecondsTimestamps.get(0);
            long lastTimestamp = sessionSecondsTimestamps.get(sessionSecondsTimestamps.size() - 1);

            double firstViewportTimestamp = firstTimestamp - (lastTimestamp - firstTimestamp) * 0.1;
            double lastViewportTimestamp = lastTimestamp + (lastTimestamp - firstTimestamp) * 0.1;

            int firstViewportSecondsTimestamp = (int) (firstViewportTimestamp);
            int lastViewportSecondsTimestamp = (int) (lastViewportTimestamp);


            SparseArray<SparseArray<Long>> bestAveragePerSessionPerNumber = getBestAveragePerSessionPerNumber(context, historySessions);


            List<ILineDataSet> graphData = new ArrayList<>();

            for (int numberIndex = 0; numberIndex < bestAveragePerSessionPerNumber.size(); numberIndex++) {
                addAverageDataSet(context, sessionSecondsTimestamps, firstViewportSecondsTimestamp, bestAveragePerSessionPerNumber, graphData, numberIndex);
            }


            SparseArray<Long> bestSolvesTimes = addBestTimesDataSet(context, historySessions, sessionSecondsTimestamps, firstViewportSecondsTimestamp, graphData);


            boolean averageMoreThanOne = false;
            for (int i = 0; i < bestAveragePerSessionPerNumber.size(); i++) {
                if (bestAveragePerSessionPerNumber.valueAt(i).size() > 1) {
                    averageMoreThanOne = true;
                }
            }
            if (averageMoreThanOne || bestSolvesTimes.size() > 1) {

                List<String> xVals = new ArrayList<>();

                //ADD FILLER TIMESTAMPS
                xVals.add(Utils.dateTimeStringFromTimestamp(context, firstViewportSecondsTimestamp * 1000L));
                for (int i = firstViewportSecondsTimestamp + 1; i < lastViewportSecondsTimestamp; i++) {
                    xVals.add("");
                }
                xVals.add(Utils.dateTimeStringFromTimestamp(context, lastViewportSecondsTimestamp * 1000L));

                /*int lastAdded = 0;
                for (int i = 0; i < sessionSecondsTimestamps.size(); i++) {
                    int timestamp = sessionSecondsTimestamps.get(i);
                    int previousTimestamp;
                    if (i == 0) {
                        previousTimestamp = firstViewportSecondsTimestamp;
                    } else {
                        previousTimestamp = sessionSecondsTimestamps.get(i - 1);
                    }

                    for(int j=previousTimestamp;j<timestamp;j++){
                        xVals.add("");
                    }
                    String timestampString = Utils.dateTimeSecondsStringFromTimestamp(context, timestamp * 1000L);
                    xVals.add(timestampString);

                    lastAdded = timestamp;
                }

                for(int i=lastAdded;i<lastViewportSecondsTimestamp; i++){
                    xVals.add("");
                }*/

                LineData data = new LineData(xVals, graphData);

                singleSubscriber.onSuccess(data);

            } else {
                singleSubscriber.onSuccess(null);
            }


        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(lineData -> {
                    if (isViewAttached()) {
                        if (lineData != null)
                            //noinspection ConstantConditions
                            getView().getHistorySessionsAdapter().setLineData(lineData);
                    }
                }).toObservable().toCompletable();


    }

    @NonNull
    private SparseArray<Long> addBestTimesDataSet(Context context, List<Session> historySessions, List<Integer> sessionSecondsTimestamps, int firstViewportSecondsTimestamp, List<ILineDataSet> graphData) {
        //Get best times of each session excluding DNF,
        // and create GraphViewData array bestTimes
        SparseArray<Long> bestSolvesTimes = new SparseArray<>();
        for (int i = 0; i < historySessions.size(); i++) {
            Session session = historySessions.get(i);
            List<Solve> solveList = session.getSolves(context).toList().toBlocking().first();
            Solve bestSolveOfList = Utils.getBestSolveOfList(solveList);
            if (bestSolveOfList.getPenalty() != Solve.PENALTY_DNF) {
                //noinspection ConstantConditions
                bestSolvesTimes
                        .put(i, bestSolveOfList.getTimeTwo());
            }
        }


        List<Entry> dataSetEntries = new ArrayList<>();
        for (int i = 0; i < bestSolvesTimes.size(); i++) {
            int secondsTimestamp = sessionSecondsTimestamps.get(bestSolvesTimes.keyAt(i));

            Entry entry = new Entry(
                    bestSolvesTimes.valueAt(i),
                    secondsTimestamp - firstViewportSecondsTimestamp
            );
            dataSetEntries.add(entry);
        }

        LineDataSet bestSolvesDataSet = new LineDataSet(
                dataSetEntries,
                context.getString(R.string.best_times)
        );
        bestSolvesDataSet.setDrawCircles(true);
        bestSolvesDataSet.setCircleColor(Color.BLUE);
        bestSolvesDataSet.setColor(Color.BLUE);
        bestSolvesDataSet.setLineWidth(2f);
        bestSolvesDataSet.setDrawValues(false);
        graphData.add(bestSolvesDataSet);
        return bestSolvesTimes;
    }

    private void addAverageDataSet(Context context, List<Integer> sessionSecondsTimestamps, int firstViewportSecondsTimestamp, SparseArray<SparseArray<Long>> bestAveragePerSessionPerNumber, List<ILineDataSet> graphData, int numberIndex) {
        SparseArray<Long> bestAveragePerSession = bestAveragePerSessionPerNumber.valueAt(numberIndex);

        if (bestAveragePerSession.size() == 0) {
            return;
        }

        List<Entry> dataSetEntries = new ArrayList<>();

        for (int sessionIndex = 0; sessionIndex < bestAveragePerSession.size(); sessionIndex++) {
            int secondsTimestamp = sessionSecondsTimestamps.get(bestAveragePerSession.keyAt(sessionIndex));
            Entry entry = new Entry(bestAveragePerSession.valueAt(sessionIndex), secondsTimestamp - firstViewportSecondsTimestamp);
            dataSetEntries.add(entry);
        }
        int lineColor = Color.RED;
        switch (bestAveragePerSessionPerNumber.keyAt(numberIndex)) {
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

        LineDataSet averageDataSet = new LineDataSet(
                dataSetEntries,
                String.format(context.getString(R.string.bao), bestAveragePerSessionPerNumber.keyAt(numberIndex))
        );
        averageDataSet.setDrawCircles(true);
        averageDataSet.setCircleColor(lineColor);
        averageDataSet.setLineWidth(2f);
        averageDataSet.setColor(lineColor);
        averageDataSet.setDrawValues(false);

        graphData.add(averageDataSet);
    }

    @NonNull
    private SparseArray<SparseArray<Long>> getBestAveragePerSessionPerNumber(Context context, List<Session> historySessions) {
        //This SparseArray contains any number of SparseArray<Long>,
        // one for each average (5,12,etc)
        // e.g. 5:{0 : 23.402, 1 : 21.206, 2:...}, 12:{...}...
        SparseArray<SparseArray<Long>> bestAveragePerSessionPerNumber = new SparseArray<>();
        for (int averageNumber : new int[]{5, 12, 50, 100, 1000}) {
            SparseArray<Long> timesSparseArray = new SparseArray<>();
            for (int i = 0; i < historySessions.size(); i++) {
                Session session = historySessions.get(i);
                if (session.getNumberOfSolves() >= averageNumber) {
                    List<Solve> list = session.getSolves(context).toList().toBlocking().first();
                    long bestAverage = session.getBestAverageOf(list, averageNumber).toBlocking().value();
                    if (bestAverage != Long.MAX_VALUE
                            && bestAverage != Session.GET_AVERAGE_INVALID_NOT_ENOUGH) {
                        timesSparseArray.put(i, bestAverage);
                    }
                }
            }
            if (timesSparseArray.size() > 0) {
                bestAveragePerSessionPerNumber.put(averageNumber, timesSparseArray);
            }
        }
        return bestAveragePerSessionPerNumber;
    }

    public Completable updateAdapterStatsText(Context context, List<Session> historySessions) {
        return Single.<String>create(singleSubscriber -> {
            if (historySessions.isEmpty()) {
                singleSubscriber.onSuccess(null);
            }

            StringBuilder s = new StringBuilder();

            //Get best solves of each history session and add to list
            ArrayList<Solve> bestSolvesOfSessionsArray = new ArrayList<>();
            for (Session session : historySessions) {
                List<Solve> sessionSolveList = session.getSortedSolves(context).toList().toBlocking().first();
                Solve bestSolveOfList = Utils.getBestSolveOfList(sessionSolveList);
                bestSolvesOfSessionsArray.add(bestSolveOfList);
            }

            //Add PB of all historySessions
            //noinspection ConstantConditions
            String bestTimeString = Utils.getBestSolveOfList(bestSolvesOfSessionsArray)
                    .getTimeString(mMillisecondsEnabled);
            s.append(String.format(context.getString(R.string.pb), bestTimeString));

            //Add PB of Ao5,12,50,100,1000
            s.append(getBestAverageOfNumberOfSessions(context, new int[]{1000, 100, 50, 12, 5}, historySessions));

            singleSubscriber.onSuccess(s.toString());
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(s -> {
                    if (isViewAttached())
                        //noinspection ConstantConditions
                        getView().getHistorySessionsAdapter().setStats(s);
                }).toObservable().toCompletable();
    }

    /**
     * Returns string with best averages of [numbers].
     *
     * @param numbers  the numbers for the averages
     * @param sessions list of sessions
     * @return String with the best averages of [numbers]
     */
    private String getBestAverageOfNumberOfSessions(Context context, int[] numbers,
                                                    List<Session> sessions) {
        StringBuilder builder = new StringBuilder();
        for (int number : numbers) {
            ArrayList<Long> bestAverages = new ArrayList<>();
            if (sessions.size() > 0) {
                for (Session session : sessions) {
                    List<Solve> list = session.getSolves(context).toList().toBlocking().first();
                    long bestAverage = session.getBestAverageOf(list, number).toBlocking().value();
                    if (bestAverage != Session.GET_AVERAGE_INVALID_NOT_ENOUGH) {
                        //If the average is possible for the number
                        bestAverages.add(bestAverage);
                    }
                }
                if (bestAverages.size() > 0) {
                    Long bestAverage = Collections.min(bestAverages);
                    String finalPbAo = bestAverage == Long.MAX_VALUE ? "DNF"
                            : Utils.timeStringFromNs(bestAverage, mMillisecondsEnabled);

                    String pbAo = String.format(context.getString(R.string.pb_ao), number, finalPbAo);

                    builder.append("\n")
                            .append(pbAo);
                }
            }
        }
        return builder.toString();
    }

    public static class Factory implements PresenterFactory<HistorySessionsPresenter> {

        public Factory() {
        }

        @Override
        public HistorySessionsPresenter create() {
            return new HistorySessionsPresenter();
        }
    }
}
