package com.pluscubed.plustimer.model;

import android.content.Context;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pluscubed.plustimer.App;
import com.pluscubed.plustimer.utils.Utils;

import java.util.List;

import rx.Observable;

/**
 * Session data
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Session {
    public static final int GET_AVERAGE_INVALID_NOT_ENOUGH = -1;
    public static final long TIMESTAMP_NO_SOLVES = Long.MIN_VALUE;
    @JsonIgnore
    private final String mId;
    @JsonProperty("puzzletype")
    private String mPuzzleTypeId;
    @JsonProperty("timestamp")
    private long mTimestamp;

    public Session() {
        mId = null;
    }


    public Session(String puzzleTypeId, String id) {
        mPuzzleTypeId = puzzleTypeId;
        mId = id;
        mTimestamp = TIMESTAMP_NO_SOLVES;
    }

    /**
     * ID stays the same.
     */
    public Session(Session s) {
        this(s.getPuzzleTypeId(), s.getId());
    }

    public String getPuzzleTypeId() {
        return mPuzzleTypeId;
    }

    public int getPosition(Solve i) {
        return 0;
    }

    //TODO
    public List<Solve> getSolves() {
        return null;
    }

    public String getId() {
        return mId;
    }

    public void addSolve(final Solve s) {
        App.getFirebaseUserRef().subscribe(firebase -> {
            Firebase solves = firebase.child("solves");
            Firebase newSolve = solves.push();
            s.setId(newSolve.getKey());
            Firebase sessionSolves = firebase.child("session-solves/" + getId() + "/" + s.getId());
            sessionSolves.setValue(true);
            mTimestamp = s.getTimestamp();
        });
    }

    public Observable<Long> getNumberOfSolves() {
        return App.getFirebaseUserRef()
                .flatMap(firebase -> Observable.create(subscriber -> {
                    Firebase sessionSolves = firebase.child("session-solves/" + getId());
                    sessionSolves.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            subscriber.onNext(dataSnapshot.getChildrenCount());
                            subscriber.onCompleted();
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                        }
                    });
                }));
    }

    //TODO
    public Solve getLastSolve() {
        return null;
    }

    //TODO
    public Solve getSolveByPosition(int position) {
        return null;
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
    public String getStringCurrentAverageOf(int number,
                                            boolean millisecondsEnabled) {
        /*if (number >= 3 && mSolves.size() >= number) {
            List<Solve> solves = new ArrayList<>();

            //Add the most recent solves
            for (int i = 0; i < number; i++) {
                Solve x = mSolves.get(mSolves.size() - i - 1);
                solves.add(x);
            }

            long result = Utils.getAverageOf(solves);

            if (result == Long.MAX_VALUE) {
                return "DNF";
            } else {
                return Utils.timeStringFromNs(result, millisecondsEnabled);
            }
        }
        return "";*/
        return "";
    }

    /**
     * Returns a String of the best average of some number of solves.
     * Returns a blank String if the number is less than 3.
     *
     * @param number              the number of solves to average
     * @param millisecondsEnabled whether to display milliseconds
     * @return the best average of some number of solves
     */
    String getStringBestAverageOf(int number,
                                  boolean millisecondsEnabled) {
        long bestAverage = getBestAverageOf(number);
        if (bestAverage == GET_AVERAGE_INVALID_NOT_ENOUGH) {
            return "";
        }
        if (bestAverage == Long.MAX_VALUE) {
            return "DNF";
        }
        return Utils.timeStringFromNs(bestAverage, millisecondsEnabled);
    }

    //TODO

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
    public long getBestAverageOf(int number) {
        /*if (number >= 3 && mSolves.size() >= number) {
            long bestAverage = 0;
            //Iterates through the list, starting with the [number] most
            // recent solves
            for (int i = 0; mSolves.size() - (number + i) >= 0; i++) {
                //Sublist the [number] of solves, offset by i from the most
                // recent. Gets the average.
                long average = Utils.getAverageOf(mSolves.subList(mSolves.size
                        () - (number + i), mSolves.size() - i));
                //If the average is less than the current best (or on the
                // first loop), set the best average to the average
                if (i == 0 || average < bestAverage) {
                    bestAverage = average;
                }
            }
            return bestAverage;
        }*/
        return GET_AVERAGE_INVALID_NOT_ENOUGH;
    }

    //TODO
    public String getStringMean(boolean milliseconds) {
        /*long sum = 0;
        for (Solve i : mSolves) {
            if (!(i.getPenalty() == Solve.Penalty.DNF)) {
                sum += i.getTimeTwo();
            } else {
                return "DNF";
            }
        }*/
        /*return Utils.timeStringFromNs(sum / mSolves.size(), milliseconds);*/
        return "";
    }

    public String getTimestampString(Context context) {
        return Utils.timeDateStringFromTimestamp(context,
                getLastSolve().getTimestamp());
    }

    public long getTimestamp() {
        return getLastSolve().getTimestamp();
    }

    //TODO
    public void deleteSolve(int position, PuzzleType type) {
        /*mSolves.remove(position);
        PuzzleType.getDataSource().deleteSolve(type, mId, position);
        notifySolveDeleted(position);*/
    }

    //TODO
    public void deleteSolve(Solve i, PuzzleType type) {
        /*deleteSolve(mSolves.indexOf(i), type);*/
    }

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
