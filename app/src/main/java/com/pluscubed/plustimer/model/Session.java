package com.pluscubed.plustimer.model;

import android.content.Context;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pluscubed.plustimer.App;
import com.pluscubed.plustimer.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;

/**
 * Session data
 */
@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class Session {
    public static final int GET_AVERAGE_INVALID_NOT_ENOUGH = -1;
    public static final long TIMESTAMP_NO_SOLVES = Long.MIN_VALUE;

    private String mId;

    @JsonProperty("timestamp")
    private long mTimestamp;

    public Session() {
    }


    public Session(String id) {
        mId = id;
        mTimestamp = TIMESTAMP_NO_SOLVES;
    }

    /**
     * ID stays the same.
     */
    public Session(Session s) {
        this(s.getId());
    }

    public void addSessionListener(ChildEventListener listener, String puzzleTypeId) {
        FirebaseDbUtil.getSolvesRef(mId).subscribe(solvesRef -> {
            solvesRef.addChildEventListener(listener);

            App.getChildEventListenerMap().put("sessions/" + puzzleTypeId + "/" + mId, listener);
        });
    }

    public void removeSessionListener(ChildEventListener listener) {
        FirebaseDbUtil.getSolvesRef(mId).subscribe(solvesRef -> {
            solvesRef.removeEventListener(listener);

            for (String key : App.getChildEventListenerMap().keySet()) {
                if (App.getChildEventListenerMap().get(key) == listener) {
                    App.getChildEventListenerMap().remove(key);
                }
            }
        });
    }

    public Single<Solve> getSolve(String solveId) {
        return FirebaseDbUtil.getSolveRef(mId, solveId)
                .flatMap(solveRef -> Single.<Solve>create(subscriber -> {
                    solveRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot solve) {
                            if (solve.exists()) {
                                Solve value = Solve.fromSnapshot(solve);
                                subscriber.onSuccess(value);
                            } else {
                                subscriber.onError(FirebaseError
                                        .fromCode(FirebaseError.UNKNOWN_ERROR).toException());
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            subscriber.onError(firebaseError.toException());
                        }
                    });
                }));
    }

    private Single<List<Solve>> getSolves() {
        return FirebaseDbUtil.getSolvesRef(mId)
                .flatMapObservable(solvesRef -> Observable.<Solve>create(subscriber -> {
                    solvesRef.orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot solves) {
                            for (DataSnapshot solveSs : solves.getChildren()) {
                                Solve solve = solveSs.getValue(Solve.class);
                                solve.setId(solveSs.getKey());
                                subscriber.onNext(solve);
                            }

                            subscriber.onCompleted();
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            subscriber.onError(firebaseError.toException());
                        }
                    });
                }))
                .toList()
                .toSingle();
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }


    public void addSolve(final Solve s, String puzzleTypeId) {
        FirebaseDbUtil.getSolvesRef(mId)
                .doOnSuccess(solves -> {
                    Firebase newSolve = solves.push();
                    newSolve.setValue(s);
                    s.setId(newSolve.getKey());
                })
                .flatMap(o -> FirebaseDbUtil.getSessionRef(puzzleTypeId, mId))
                .subscribe(new SingleSubscriber<Firebase>() {
                    @Override
                    public void onSuccess(Firebase sessionRef) {
                        mTimestamp = s.getTimestamp();
                        sessionRef.setValue(Session.this);
                        update(puzzleTypeId);
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });
    }

    public void deleteSolve(String puzzleTypeId) {
        FirebaseDbUtil.getSolvesRef(mId)
                .subscribe(solvesRef -> {
                    solvesRef.child(mId).removeValue();
                    update(puzzleTypeId);
                });
    }

    protected void update(String puzzleType) {
        getLastSolve()
                .doOnNext(solve -> mTimestamp = solve.getTimestamp())
                .isEmpty()
                .toSingle()
                .flatMap(exists -> FirebaseDbUtil.getSessionRef(puzzleType, mId))
                .subscribe(sessionRef -> {
                    sessionRef.setValue(Session.this);
                });
    }

    /**
     * @return Whether there are solves.
     */
    public Single<Boolean> solvesExist() {
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
    }

    public Single<Integer> getNumberOfSolves() {
        return FirebaseDbUtil.getSolvesRef(mId)
                .flatMap(solves -> Single.create(subscriber -> {
                    solves.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            subscriber.onSuccess((int) dataSnapshot.getChildrenCount());
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            subscriber.onError(firebaseError.toException());
                        }
                    });
                }));
    }

    /**
     * @return Empty if there are no solves
     */
    public Observable<Solve> getLastSolve() {
        return FirebaseDbUtil.getSolvesRef(mId).toObservable()
                .flatMap(solvesRef -> Observable.create(new Observable.OnSubscribe<Solve>() {
                    @Override
                    public void call(Subscriber<? super Solve> subscriber) {
                        solvesRef
                                .orderByChild("timestamp")
                                .limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot solves) {

                                for (DataSnapshot solveSs : solves.getChildren()) {
                                    Solve solve = solveSs.getValue(Solve.class);
                                    solve.setId(solveSs.getKey());
                                    subscriber.onNext(solve);
                                }

                                subscriber.onCompleted();
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                subscriber.onError(firebaseError.toException());
                            }
                        });
                    }
                }));
    }

    public Single<Solve> getSolveByPosition(int position) {
        return getSolves().map(solves -> solves.get(position));
    }

    //TODO

    /**
     * Returns a String of the current average of some number of solves.
     * If the number less than 3 or if the number of solves is less than the
     * number, it'll return a blank String.
     *
     * @param number              the number of solves to average
     * @param millisecondsEnabled whether to display milliseconds
     * @return the current average of some number of solves
     */
    public Single<String> getStringCurrentAverageOf(int number, boolean millisecondsEnabled) {
        return getNumberOfSolves()
                .flatMapObservable(numberOfSolves -> {
                    if (number >= 3 && numberOfSolves >= number) {
                        return getSolves().toObservable();
                    }
                    return Observable.empty();
                }).map(solves -> {
                    List<Solve> recent = new ArrayList<>();

                    //Add the most recent solves
                    for (int i = 0; i < number; i++) {
                        Solve x = solves.get(solves.size() - i - 1);
                        recent.add(x);
                    }

                    long result = Utils.getAverageOf(recent);

                    if (result == Long.MAX_VALUE) {
                        return "DNF";
                    } else {
                        return Utils.timeStringFromNs(result, millisecondsEnabled);
                    }
                })
                .defaultIfEmpty("")
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
    public Single<String> getStringBestAverageOf(int number, boolean millisecondsEnabled) {
        return getBestAverageOf(number).map(bestAverage -> {
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
    public Single<Long> getBestAverageOf(int number) {
        return getNumberOfSolves()
                .flatMapObservable(numberOfSolves -> {
                    if (number >= 3 && numberOfSolves >= number) {
                        return getSolves().toObservable();
                    }
                    return Observable.empty();
                }).map(solves -> {
                    long bestAverage = 0;
                    //Iterates through the list, starting with the [number] most
                    // recent solves
                    for (int i = 0; solves.size() - (number + i) >= 0; i++) {
                        //Sublist the [number] of solves, offset by i from the most
                        // recent. Gets the average.
                        long average = Utils.getAverageOf(solves.subList(solves.size
                                () - (number + i), solves.size() - i));
                        //If the average is less than the current best (or on the
                        // first loop), set the best average to the average
                        if (i == 0 || average < bestAverage) {
                            bestAverage = average;
                        }
                    }
                    return bestAverage;
                })
                .defaultIfEmpty((long) GET_AVERAGE_INVALID_NOT_ENOUGH)
                .toSingle();
    }


    public Single<String> getStringMean(boolean milliseconds) {
        return getSolves().map(solves -> {
            long sum = 0;
            for (Solve i : solves) {
                if (!(i.getPenalty() == Solve.PENALTY_DNF)) {
                    sum += i.getTimeTwo();
                } else {
                    return "DNF";
                }
            }
            return Utils.timeStringFromNs(sum / solves.size(), milliseconds);
        });
    }

    public String getTimestampString(Context context) {
        return Utils.timeDateStringFromTimestamp(context, mTimestamp);
    }

    public long getTimestamp() {
        return mTimestamp;
    }



    /*//TODO
    public void deleteSolve(Solve i, PuzzleType type) {
        App.getFirebaseUserRef().subscribe(userRef -> {
            Firebase solves = userRef.child("solves");
            Firebase newSolve = solves.push();
            s.setId(newSolve.getKey());
            Firebase sessionSolves = userRef.child("session-solves").child(getId()).child(s.getId());
            sessionSolves.setValue(true);
            mTimestamp = s.getTimestamp();
        });
    }*/

    //TODO
    public String toString(Context context, String puzzleTypeName,
                           boolean current, boolean displaySolves,
                           boolean milliseconds, boolean sign) {
        StringBuilder s = new StringBuilder();
        /*if (displaySolves) {
            s.append(PuzzleType.valueOf(puzzleTypeName).getName(context)).append("\n\n");
        }
        s.append(context.getString(R.string.number_solves)).append
                (getNumberOfSolves());
        if (getNumberOfSolves() > 0) {
            s.append("\n").append(context.getString(R.string.mean)).append
                    (getStringMean(milliseconds));
            if (getNumberOfSolves() > 1) {
                s.append("\n").append(context.getString(R.string.best))
                        .append(Utils.getBestSolveOfList(mSolves)
                                .getTimeString(milliseconds));
                s.append("\n").append(context.getString(R.string.worst))
                        .append(Utils.getWorstSolveOfList(mSolves)
                                .getTimeString(milliseconds));

                if (getNumberOfSolves() > 2) {
                    long average = Utils.getAverageOf(mSolves);
                    if (average != Long.MAX_VALUE) {
                        s.append("\n").append(context.getString(R.string
                                .average)).append(Utils.timeStringFromNs
                                (average, milliseconds));
                    } else {
                        s.append("\n").append(context.getString(R.string
                                .average)).append("DNF");
                    }

                    int[] averages = {1000, 100, 50, 12, 5};
                    for (int i : averages) {
                        if (getNumberOfSolves() >= i) {
                            if (current) {
                                s.append("\n").append(String.format(context
                                                .getString(R.string.cao),
                                        i)).append(": ").append
                                        (getStringCurrentAverageOf(i,
                                                milliseconds));
                            } else {
                                s.append("\n").append(String.format(context
                                                .getString(R.string.lao),
                                        i)).append(": ").append
                                        (getStringCurrentAverageOf(i,
                                                milliseconds));
                            }
                            s.append("\n").append(String.format(context
                                            .getString(R.string.bao),
                                    i)).append(": ").append
                                    (getStringBestAverageOf(i, milliseconds));
                        }
                    }
                }
            }
            if (displaySolves) {
                s.append("\n\n");
                int c = 1;
                for (Solve i : mSolves) {
                    Solve best = Utils.getBestSolveOfList(mSolves);
                    Solve worst = Utils.getWorstSolveOfList(mSolves);
                    s.append(c).append(". ");
                    if (i == best || i == worst) {
                        s.append("(").append(i.getDescriptiveTimeString
                                (milliseconds)).append(")");
                    } else {
                        s.append(i.getDescriptiveTimeString(milliseconds));
                    }
                    s.append("\n").append("     ").append(Utils
                            .timeDateStringFromTimestamp(context,
                                    i.getTimestamp()))
                            .append("\n").append("     ").append(i
                            .getScramble().getUiScramble(sign,
                                    puzzleTypeName))
                            .append("\n\n");
                    c++;
                }
            }
        }*/
        return s.toString();
    }

}
