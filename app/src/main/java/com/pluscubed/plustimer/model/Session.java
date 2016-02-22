package com.pluscubed.plustimer.model;

import android.content.Context;
import android.support.annotation.NonNull;

import com.couchbase.lite.CouchbaseLiteException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;
import com.pluscubed.plustimer.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

/**
 * Session data
 */
@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class Session extends CbObject {
    public static final String TYPE_SESSION = "session";

    public static final int GET_AVERAGE_INVALID_NOT_ENOUGH = -1;
    public static final long TIMESTAMP_NO_SOLVES = Long.MIN_VALUE;

    private static Map<String, Set<SolvesListener>> sListenerMap;
    @JsonProperty("solves")
    private Set<String> mSolves;

    public Session(Context context) throws CouchbaseLiteException, IOException {
        super(context);
    }

    private static Map<String, Set<SolvesListener>> getListenerMap() {
        if (sListenerMap == null) {
            sListenerMap = new HashMap<>();
        }
        return sListenerMap;
    }

    public void addListener(SolvesListener listener) {
        if (getListenerMap().containsKey(mId)) {
            Set<SolvesListener> set = sListenerMap.get(mId);
            set.add(listener);
        } else {
            Set<SolvesListener> set = new HashSet<>();
            set.add(listener);
            sListenerMap.put(mId, set);
        }
    }

    public void removeListener(SolvesListener listener) {
        Set<SolvesListener> solvesListeners = getListenerMap().get(mId);
        solvesListeners.remove(listener);
        if (solvesListeners.size() == 0) {
            getListenerMap().remove(mId);
        }
    }

    @Override
    protected String getType() {
        return TYPE_SESSION;
    }

    /**
     * ID stays the same.
     */
    /*public Session(Context context, Session s) {
        this(s.getId());
    }*/
    public Solve getSolve(Context context, String solveId) throws CouchbaseLiteException, IOException {
        return fromDocId(context, solveId, Solve.class);
    }

    //TODO

    private Observable<Solve> getSolves(Context context) {
        return Observable.from(new ArrayList<>(mSolves))
                .subscribeOn(Schedulers.io())
                .flatMap(id -> {
                    try {
                        return Observable.just(getSolve(context, id));
                    } catch (CouchbaseLiteException | IOException e) {
                        return Observable.error(e);
                    }
                });
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public Solve newSolve(Context context) throws IOException, CouchbaseLiteException {
        Solve solve = new Solve(context);

        mSolves.add(solve.getId());

        updateCb(context);
        notifyListeners(solve, RecyclerViewUpdate.INSERT);

        return solve;
    }

    /**
     * Creates the Solve Document and adds it to the session
     */
    public void addDisconnectedSolve(Context context, Solve solve)
            throws CouchbaseLiteException, IOException {
        solve.connectCb(context);

        updateCb(context);
        notifyListeners(solve, RecyclerViewUpdate.INSERT);
    }

    public Completable deleteSolveDeferred(Context context, String id) {
        return Completable.fromCallable(() -> {
            deleteSolve(context, id);
            return null;
        });
    }

    public void deleteSolve(Context context, String id) throws CouchbaseLiteException, IOException {
        getSolve(context, id)
                .getDocument(context)
                .delete();

        mSolves.remove(id);

        updateCb(context);
        notifyListeners(getSolve(context, id), RecyclerViewUpdate.REMOVE);
    }

    public void notifyListeners(Solve solve, RecyclerViewUpdate update) {
        if (sListenerMap.containsKey(mId)) {
            for (SolvesListener listener : sListenerMap.get(mId)) {
                listener.notifyChange(update, solve);
            }
        }
    }

    /**
     * @return Whether there are solves.
     */
    /*public Single<Boolean> solvesExist() {
        return FirebaseDbUtil.getSolvesRef(mId)
                .flatMap(firebase -> Single.create(new Single.OnSubscribe<Boolean>() {
                    @Override
                    public void call(SingleSubscriber<? super Boolean> singleSubscriber) {
                        firebase.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                singleSubscriber.onSuccess(dataSnapshot.exists());
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {

                            }
                        });
                    }
                }));
    }*/
    public int getNumberOfSolves() {
        return mSolves.size();
    }

    public Observable<Solve> getLastSolve(Context context) {
        return getSortedSolves(context)
                .takeLast(1);
    }

    @NonNull
    public Observable<Solve> getSortedSolves(Context context) {
        return getSolves(context)
                .toSortedList((solve, solve2) ->
                        solve.getTimestamp() < solve2.getTimestamp() ? -1
                                : (solve.getTimestamp() == solve2.getTimestamp() ? 0 : 1))
                .flatMap(Observable::from);
    }

    /**
     * IndexOutOfBoundsException if no solves
     */
    public Single<Solve> getSolveByPosition(Context context, int position) {
        return getSortedSolves(context)
                .elementAt(position)
                .toSingle();
    }

    /**
     * Returns a String of the current average of some number of solves.
     * If the number less than 3 or if the number of solves is less than the
     * number, it'll return a blank String.
     *
     * @param number              the number of solves to average
     * @param millisecondsEnabled whether to display milliseconds
     * @return the current average of some number of solves
     */
    public Single<String> getStringCurrentAverageOf(Context context, int number, boolean millisecondsEnabled) {
        if (number >= 3 && getNumberOfSolves() >= number) {
            return getSortedSolves(context)
                    .takeLast(number)
                    .toList()
                    .toSingle()
                    .map(Utils::getAverageOf)
                    .map(average -> average == Long.MAX_VALUE ?
                            "DNF" : Utils.timeStringFromNs(average, millisecondsEnabled));
        } else {
            return Single.just("");
        }
    }

    //TODO

    /**
     * Returns a String of the best average of some number of solves.
     * Returns a blank String if the number is less than 3.
     *
     * @param number              the number of solves to average
     * @param millisecondsEnabled whether to display milliseconds
     * @return the best average of some number of solves
     */
    public Single<String> getStringBestAverageOf(Context context, int number, boolean millisecondsEnabled) {
        return getBestAverageOf(context, number).map(bestAverage -> {
            if (bestAverage == GET_AVERAGE_INVALID_NOT_ENOUGH) {
                return "";
            }
            if (bestAverage == Long.MAX_VALUE) {
                return "DNF";
            }
            return Utils.timeStringFromNs(bestAverage, millisecondsEnabled);
        });
    }

    /**
     * Returns the milliseconds value of the best average of some number of
     * solves.
     * Returns {@link Long#MAX_VALUE} for DNF and {@link
     * Session#GET_AVERAGE_INVALID_NOT_ENOUGH} if the list size is less than
     * 3 or if the number of solves is less than the number.
     *
     * @param number the number of solves to average
     * @return the best average of some number of solves
     */
    public Single<Long> getBestAverageOf(Context context, int number) {
        if (number >= 3 && getNumberOfSolves() >= number) {
            return getSolves(context)
                    .takeLast(number)
                    .toList()
                    .toSingle()
                    .map(solves -> {
                        long bestAverage = 0;
                        //Iterates through the list, starting with the [number] most recent solves
                        for (int i = 0; solves.size() - (number + i) >= 0; i++) {
                            //Sublist the [number] of solves, offset by i from the most
                            // recent. Gets the average.
                            long average = Utils.getAverageOf(solves.subList(solves.size() - (number + i), solves.size() - i));
                            //If the average is less than the current best (or on the first loop),
                            // set the best average to the average
                            if (i == 0 || average < bestAverage) {
                                bestAverage = average;
                            }
                        }
                        return bestAverage;
                    });
        } else {
            return Single.just((long) GET_AVERAGE_INVALID_NOT_ENOUGH);
        }
    }

    public String getStringMean(Context context, boolean milliseconds) {
        return getSolves(context)
                .toList()
                .toSingle()
                .map(solves -> {
                    long sum = 0;
                    for (Solve i : solves) {
                        if (!(i.getPenalty() == Solve.PENALTY_DNF)) {
                            sum += i.getTimeTwo();
                        } else {
                            return "DNF";
                        }
                    }
                    return Utils.timeStringFromNs(sum / solves.size(), milliseconds);
                })
                .toBlocking()
                .value();
    }

    public String getTimestampString(Context context) {
        return Utils.timeDateStringFromTimestamp(context, getTimestamp(context));
    }

    public long getTimestamp(Context context) {
        return getLastSolve(context)
                .map(Solve::getTimestamp)
                .defaultIfEmpty(TIMESTAMP_NO_SOLVES)
                .toSingle().toBlocking().value();
    }

    public Single<String> getStatsDeferred(Context context, String puzzleTypeName,
                                           boolean current, boolean displaySolves,
                                           boolean milliseconds, boolean sign) {
        return Single.defer(() -> Single.just(getStats(context, puzzleTypeName, current, displaySolves, milliseconds, sign)));
    }

    public String getStats(Context context, String puzzleTypeName,
                           boolean current, boolean displaySolves,
                           boolean milliseconds, boolean sign) {

        StringBuilder statsBuilder = new StringBuilder();
        if (displaySolves) {
            statsBuilder.append(PuzzleType.get(puzzleTypeName).getName()).append("\n\n");
        }

        int numberOfSolves = getNumberOfSolves();

        statsBuilder.append(context.getString(R.string.number_solves)).append(numberOfSolves);

        if (numberOfSolves == 0) {
            return statsBuilder.toString();
        }

        statsBuilder.append("\n").append(context.getString(R.string.mean)).append(getStringMean(context, milliseconds));

        List<Solve> solves = getSolves(context).toList().toBlocking().first();

        Solve bestSolve = Utils.getBestSolveOfList(solves);
        Solve worstSolve = Utils.getWorstSolveOfList(solves);

        if (numberOfSolves >= 2) {
            statsBuilder.append("\n").append(context.getString(R.string.best))
                    .append(bestSolve.getTimeString(milliseconds));
            statsBuilder.append("\n").append(context.getString(R.string.worst))
                    .append(worstSolve.getTimeString(milliseconds));

            if (numberOfSolves >= 3) {
                long average = Utils.getAverageOf(solves);
                if (average != Long.MAX_VALUE) {
                    statsBuilder.append("\n").append(context.getString(R.string.average))
                            .append(Utils.timeStringFromNs(average, milliseconds));
                } else {
                    statsBuilder.append("\n").append(context.getString(R.string.average)).append("DNF");
                }

                int[] averages = {1000, 100, 50, 12, 5};
                for (int i : averages) {
                    if (numberOfSolves >= i) {
                        if (current) {
                            statsBuilder.append("\n").append(String.format(context.getString(R.string.cao), i))
                                    .append(": ").append(getStringCurrentAverageOf(context, i, milliseconds).toBlocking().value());
                        } else {
                            statsBuilder.append("\n").append(String.format(context.getString(R.string.lao), i))
                                    .append(": ").append(getStringCurrentAverageOf(context, i, milliseconds).toBlocking().value());
                        }
                        statsBuilder.append("\n").append(String.format(context.getString(R.string.bao), i))
                                .append(": ").append(getStringBestAverageOf(context, i, milliseconds).toBlocking().value());
                    }
                }
            }
        }
        if (displaySolves) {
            statsBuilder.append("\n\n");
            for (int i = 0; i < solves.size(); i++) {
                Solve solve = solves.get(i);
                statsBuilder.append(i + 1).append(". ");
                if (solve == bestSolve || solve == worstSolve) {
                    statsBuilder.append("(").append(solve.getDescriptiveTimeString(milliseconds)).append(")");
                } else {
                    statsBuilder.append(solve.getDescriptiveTimeString(milliseconds));
                }
                statsBuilder.append("\n     ").append(Utils.timeDateStringFromTimestamp(context, solve.getTimestamp()))
                        .append("\n     ").append(Utils.getUiScramble(solve.getScramble(), sign, puzzleTypeName))
                        .append("\n\n");
            }
        }

        return statsBuilder.toString();
    }

    public interface SolvesListener {
        void notifyChange(RecyclerViewUpdate update, Solve solve);
    }

}
