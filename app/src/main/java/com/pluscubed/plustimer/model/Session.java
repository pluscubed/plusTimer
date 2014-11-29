package com.pluscubed.plustimer.model;

import android.content.Context;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Session data
 */
public class Session {

    public static final int GET_AVERAGE_INVALID_NOT_ENOUGH = -1;

    private ArrayList<Solve> mSolves;

    /**
     * Constructs a Session with an empty list of Solves
     */
    public Session() {
        mSolves = new ArrayList<>();
    }

    public int getPosition(Solve i) {
        return mSolves.indexOf(i);
    }

    public List<Solve> getSolves() {
        return new ArrayList<>(Collections.unmodifiableList(mSolves));
    }

    public void addSolve(Solve s) {
        mSolves.add(s);
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

            long result = Util.getAverageOf(solves);

            if (result == Long.MAX_VALUE) {
                return "DNF";
            } else {
                return Util.timeStringFromNs(result, millisecondsEnabled);
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
        return Util.timeStringFromNs(bestAverage, millisecondsEnabled);
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
                long average = Util.getAverageOf(mSolves.subList(mSolves.size
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
        return Util.timeStringFromNs(sum / mSolves.size(), milliseconds);
    }

    public String getTimestampString(Context context) {
        return Util.timeDateStringFromTimestamp(context,
                getLastSolve().getTimestamp());
    }

    public long getTimestamp() {
        return getLastSolve().getTimestamp();
    }

    public void deleteSolve(int position) {
        mSolves.remove(position);
    }

    public void deleteSolve(Solve i) {
        mSolves.remove(i);
    }

    public String toString(Context context, String puzzleTypeName,
                           boolean current, boolean displaySolves,
                           boolean milliseconds, boolean sign) {
        StringBuilder s = new StringBuilder();
        if (displaySolves) {
            s.append(puzzleTypeName).append("\n\n");
        }
        s.append(context.getString(R.string.number_solves)).append
                (getNumberOfSolves());
        if (getNumberOfSolves() > 0) {
            s.append("\n").append(context.getString(R.string.mean)).append
                    (getStringMean(milliseconds));
            if (getNumberOfSolves() > 1) {
                s.append("\n").append(context.getString(R.string.best))
                        .append(Util.getBestSolveOfList(mSolves)
                                .getTimeString(milliseconds));
                s.append("\n").append(context.getString(R.string.worst))
                        .append(Util.getWorstSolveOfList(mSolves)
                                .getTimeString(milliseconds));

                if (getNumberOfSolves() > 2) {
                    long average = Util.getAverageOf(mSolves);
                    if (average != Long.MAX_VALUE) {
                        s.append("\n").append(context.getString(R.string
                                .average)).append(Util.timeStringFromNs
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
                    Solve best = Util.getBestSolveOfList(mSolves);
                    Solve worst = Util.getWorstSolveOfList(mSolves);
                    s.append(c).append(". ");
                    if (i == best || i == worst) {
                        s.append("(").append(i.getDescriptiveTimeString
                                (milliseconds)).append(")");
                    } else {
                        s.append(i.getDescriptiveTimeString(milliseconds));
                    }
                    s.append("\n").append("     ").append(Util
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

}
