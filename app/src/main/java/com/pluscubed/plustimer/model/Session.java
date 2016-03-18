package com.pluscubed.plustimer.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.couchbase.lite.CouchbaseLiteException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.RecyclerViewUpdate;
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
import rx.android.schedulers.AndroidSchedulers;
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
public class Session extends CbObject implements Parcelable {
    public static final String TYPE_SESSION = "session";

    public static final int GET_AVERAGE_INVALID_NOT_ENOUGH = -1;
    public static final long TIMESTAMP_NO_SOLVES = Long.MIN_VALUE;
    public static final Parcelable.Creator<Session> CREATOR = new Parcelable.Creator<Session>() {
        public Session createFromParcel(Parcel source) {
            return new Session(source);
        }

        public Session[] newArray(int size) {
            return new Session[size];
        }
    };
    private static Map<String, Set<SolvesListener>> sListenerMap;
    @JsonProperty("solves")
    @NonNull
    private Set<String> mSolves;

    public Session() {
        mSolves = new HashSet<>();
    }

    public Session(Context context) throws CouchbaseLiteException, IOException {
        super(context);
        mSolves = new HashSet<>();

        updateCb(context);
    }

    protected Session(Parcel in) {
        List<String> list = new ArrayList<>();
        in.readStringList(list);
        mSolves = new HashSet<>();
        mSolves.addAll(list);
    }

    private static Map<String, Set<SolvesListener>> getListenerMap() {
        if (sListenerMap == null) {
            sListenerMap = new HashMap<>();
        }
        return sListenerMap;
    }

    public static void notifyListeners(String sessionId, Solve solve, RecyclerViewUpdate update) {
        Completable.fromCallable(() -> {
            if (sListenerMap.containsKey(sessionId)) {
                for (SolvesListener listener : sListenerMap.get(sessionId)) {
                    listener.notifyChange(update, solve);
                }
            }
            return null;
        }).subscribeOn(AndroidSchedulers.mainThread())
                .subscribe();
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
    public static Single<String> getStringCurrentAverageOf(List<Solve> sortedSolves, int number, boolean millisecondsEnabled) {
        if (number >= 3 && sortedSolves.size() >= number) {
            return Observable.from(sortedSolves)
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

    public static String getStringMean(List<Solve> solves, boolean milliseconds) {
        long sum = 0;
        for (Solve i : solves) {
            if (!(i.getPenalty() == Solve.PENALTY_DNF)) {
                sum += i.getTimeTwo();
            } else {
                return "DNF";
            }
        }
        return Utils.timeStringFromNs(sum / solves.size(), milliseconds);
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
    @WorkerThread
    public Solve getSolve(Context context, String solveId) throws CouchbaseLiteException, IOException {
        return fromDocId(context, solveId, Solve.class);
    }

    public Observable<Solve> getSolves(Context context) {
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

    public SolveBuilder newSolve(Context context) {
        return new SolveBuilder(context);
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

    public Completable deleteSolveAsync(Context context, String id) {
        return Completable.fromCallable(() -> {
            deleteSolve(context, id);
            return null;
        });
    }

    public void deleteSolve(Context context, String id) throws CouchbaseLiteException, IOException {
        Solve solve = getSolve(context, id);
        solve.getDocument(context).delete();

        mSolves.remove(id);

        updateCb(context);
        notifyListeners(solve, RecyclerViewUpdate.REMOVE);
    }

    private void notifyListeners(Solve solve, RecyclerViewUpdate update) {
        notifyListeners(mId, solve, update);
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

    //TODO

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
     * Returns a String of the best average of some number of solves.
     * Returns a blank String if the number is less than 3.
     *
     * @param number              the number of solves to average
     * @param millisecondsEnabled whether to display milliseconds
     * @return the best average of some number of solves
     */
    public Single<String> getStringBestAverageOf(List<Solve> solves, int number, boolean millisecondsEnabled) {
        return getBestAverageOf(solves, number).map(bestAverage -> {
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
    public Single<Long> getBestAverageOf(List<Solve> solves, int number) {
        if (number >= 3 && getNumberOfSolves() >= number) {
            return Observable.just(solves)
                    .toSingle()
                    .map(lastSolves -> {
                        long bestAverage = 0;
                        //Iterates through the list, starting with the [number] most recent solves
                        for (int i = 0; lastSolves.size() - (number + i) >= 0; i++) {
                            //Sublist the [number] of solves, offset by i from the most
                            // recent. Gets the average.
                            long average = Utils.getAverageOf(lastSolves.subList(lastSolves.size() - (number + i), lastSolves.size() - i));
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

    public Single<String> getTimestampString(Context context) {
        return getTimestamp(context)
                .map(timestamp -> Utils.dateTimeSecondsStringFromTimestamp(context, timestamp));
    }

    public Single<Long> getTimestamp(Context context) {
        return getLastSolve(context)
                .map(Solve::getTimestamp)
                .defaultIfEmpty(TIMESTAMP_NO_SOLVES)
                .toSingle();
    }

    public Single<String> getStatsDeferred(Context context, String puzzleTypeName,
                                           boolean current, boolean displaySolves,
                                           boolean milliseconds, boolean sign) {
        return Single.defer(() ->
                Single.just(getStats(context, puzzleTypeName, current, displaySolves, milliseconds, sign)))
                .subscribeOn(Schedulers.io());
    }

    @WorkerThread
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

        List<Solve> sortedSolves = getSortedSolves(context).toList().toBlocking().first();

        statsBuilder.append("\n")
                .append(context.getString(R.string.mean))
                .append(getStringMean(sortedSolves, milliseconds));

        Solve bestSolve = Utils.getBestSolveOfList(sortedSolves);
        Solve worstSolve = Utils.getWorstSolveOfList(sortedSolves);

        if (numberOfSolves >= 2) {
            statsBuilder.append("\n")
                    .append(context.getString(R.string.best))
                    .append(bestSolve.getTimeString(milliseconds));
            statsBuilder.append("\n")
                    .append(context.getString(R.string.worst))
                    .append(worstSolve.getTimeString(milliseconds));

            if (numberOfSolves >= 3) {
                long average = Utils.getAverageOf(sortedSolves);
                if (average != Long.MAX_VALUE) {
                    statsBuilder.append("\n")
                            .append(context.getString(R.string.average))
                            .append(Utils.timeStringFromNs(average, milliseconds));
                } else {
                    statsBuilder.append("\n")
                            .append(context.getString(R.string.average))
                            .append("DNF");
                }

                int[] averages = {1000, 100, 50, 12, 5};
                for (int i : averages) {
                    if (numberOfSolves >= i) {
                        if (current) {
                            statsBuilder.append("\n")
                                    .append(String.format(context.getString(R.string.cao), i))
                                    .append(": ").append(getStringCurrentAverageOf(sortedSolves, i, milliseconds).toBlocking().value());
                        } else {
                            statsBuilder.append("\n").append(String.format(context.getString(R.string.lao), i))
                                    .append(": ").append(getStringCurrentAverageOf(sortedSolves, i, milliseconds).toBlocking().value());
                        }
                        statsBuilder.append("\n").append(String.format(context.getString(R.string.bao), i))
                                .append(": ").append(getStringBestAverageOf(sortedSolves, i, milliseconds).toBlocking().value());
                    }
                }
            }
        }
        if (displaySolves) {
            statsBuilder.append("\n\n");
            for (int i = 0; i < sortedSolves.size(); i++) {
                Solve solve = sortedSolves.get(i);
                statsBuilder.append(i + 1).append(". ");
                if (solve == bestSolve || solve == worstSolve) {
                    statsBuilder.append("(").append(solve.getDescriptiveTimeString(milliseconds)).append(")");
                } else {
                    statsBuilder.append(solve.getDescriptiveTimeString(milliseconds));
                }
                statsBuilder.append("\n     ").append(Utils.dateTimeSecondsStringFromTimestamp(context, solve.getTimestamp()))
                        .append("\n     ").append(Utils.getUiScramble(solve.getScramble(), sign, puzzleTypeName))
                        .append("\n\n");
            }
        }

        return statsBuilder.toString();
    }

    public void reset(Context context) {
        mSolves.clear();
        updateCb(context);

        notifyListeners(null, RecyclerViewUpdate.REMOVE_ALL);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(new ArrayList<>(mSolves));
    }

    public interface SolvesListener {
        void notifyChange(RecyclerViewUpdate update, Solve solve);
    }

    public class SolveBuilder extends Solve.Builder {
        public SolveBuilder(Context context) {
            super(context);
        }

        @Override
        public SolveBuilder setRawTime(long time) {
            super.setRawTime(time);
            return this;
        }

        @Override
        public SolveBuilder setTimestamp(long timestamp) {
            super.setTimestamp(timestamp);
            return this;
        }

        @Override
        public SolveBuilder setPenalty(@Solve.Penalty int penalty) {
            super.setPenalty(penalty);
            return this;
        }

        @Override
        public SolveBuilder setScramble(String scramble) {
            super.setScramble(scramble);
            return this;
        }

        @Override
        public Solve build() throws CouchbaseLiteException, IOException {
            super.build();

            mSolves.add(solve.getId());
            updateCb(context);

            notifyListeners(solve, RecyclerViewUpdate.INSERT);

            return solve;
        }
    }
}
