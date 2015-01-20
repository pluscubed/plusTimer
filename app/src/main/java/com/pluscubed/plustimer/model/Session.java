package com.pluscubed.plustimer.model;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.utils.Utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Session data
 */
public class Session {

    public static final int GET_AVERAGE_INVALID_NOT_ENOUGH = -1;

    private List<Solve> mSolves;

    private transient List<Observer> mObservers;

    /**
     * Constructs a Session with an empty list of Solves
     */
    public Session() {
        mObservers = new ArrayList<>();
        mSolves = new ArrayList<>();
    }

    public Session(Session s) {
        this();
        mSolves = new ArrayList<>(s.getSolves());
    }

    public int getPosition(Solve i) {
        return mSolves.indexOf(i);
    }

    public List<Solve> getSolves() {
        return new ArrayList<>(Collections.unmodifiableList(mSolves));
    }

    public void reset() {
        mSolves.clear();
        notifyReset();
    }

    public void notifySolveAdded() {
        for (Observer s : mObservers) {
            s.onSolveAdded();
        }
    }

    public void notifySolveDeleted(int index) {
        for (Observer s : mObservers) {
            s.onSolveRemoved(index);
        }
    }

    public void notifySolveChanged(int index) {
        for (Observer s : mObservers) {
            s.onSolveChanged(index);
        }
    }

    public void notifyReset() {
        for (Observer s : mObservers) {
            s.onReset();
        }
    }

    public void registerObserver(Observer observer) {
        mObservers.add(observer);
    }

    public void unregisterObserver(Observer observer) {
        mObservers.remove(observer);
    }

    public void unregisterAllObservers() {
        mObservers.clear();
    }

    public void addSolve(final Solve s) {
        mSolves.add(s);
        s.attachSession(this);

        notifySolveAdded();
    }

    public int getNumberOfSolves() {
        return mSolves.size();
    }

    public Solve getLastSolve() {
        return mSolves.get(mSolves.size() - 1);
    }

    public Solve getSolveByPosition(int position) {
        return mSolves.get(position);
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
    public String getStringCurrentAverageOf(int number,
                                            boolean millisecondsEnabled) {
        if (number >= 3 && mSolves.size() >= number) {
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
    public String getStringBestAverageOf(int number,
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
        if (number >= 3 && mSolves.size() >= number) {
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
        }
        return GET_AVERAGE_INVALID_NOT_ENOUGH;
    }

    public String getStringMean(boolean milliseconds) {
        long sum = 0;
        for (Solve i : mSolves) {
            if (!(i.getPenalty() == Solve.Penalty.DNF)) {
                sum += i.getTimeTwo();
            } else {
                return "DNF";
            }
        }
        return Utils.timeStringFromNs(sum / mSolves.size(), milliseconds);
    }

    public String getTimestampString(Context context) {
        return Utils.timeDateStringFromTimestamp(context,
                getLastSolve().getTimestamp());
    }

    public long getTimestamp() {
        return getLastSolve().getTimestamp();
    }

    public void deleteSolve(int position) {
        mSolves.remove(position);
        notifySolveDeleted(position);
    }

    public void deleteSolve(Solve i) {
        int position = mSolves.indexOf(i);
        mSolves.remove(i);
        notifySolveDeleted(position);
    }

    public String toString(Context context, String puzzleTypeName,
                           boolean current, boolean displaySolves,
                           boolean milliseconds, boolean sign) {
        StringBuilder s = new StringBuilder();
        if (displaySolves) {
            s.append(PuzzleType.valueOf(puzzleTypeName).getUiName(context)).append("\n\n");
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
                            .getScrambleAndSvg().getUiScramble(sign,
                                    puzzleTypeName))
                            .append("\n\n");
                    c++;
                }
            }
        }
        return s.toString();
    }

    public static class Observer {
        public void onSolveAdded() {
        }

        public void onSolveChanged(int index) {
        }

        public void onSolveRemoved(int index) {
        }

        public void onReset() {
        }
    }

    public static class Deserializer implements JsonDeserializer<Session> {
        @Override
        public Session deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws
                JsonParseException {
            Session s = new Gson().fromJson(json, typeOfT);
            for (final Solve solve : s.mSolves) {
                solve.attachSession(s);
            }
            return s;
        }
    }

}
