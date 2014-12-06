package com.pluscubed.plustimer.model;

import android.database.DataSetObservable;
import android.database.DataSetObserver;

import com.pluscubed.plustimer.Util;

/**
 * solve times data object
 */
public class Solve {

    private ScrambleAndSvg mScrambleAndSvg;

    private Penalty mPenalty;

    private long mRawTime;

    private long mTimestamp;

    private transient DataSetObservable mDataSetObservable;

    /**
     * No-args constructor for GSON
     */
    public Solve() {
        mDataSetObservable = new DataSetObservable();
    }

    public Solve(ScrambleAndSvg scramble, long time) {
        mScrambleAndSvg = scramble;
        mRawTime = time;
        mPenalty = Penalty.NONE;
        mTimestamp = System.currentTimeMillis();
        mDataSetObservable = new DataSetObservable();
    }

    public ScrambleAndSvg getScrambleAndSvg() {
        return mScrambleAndSvg;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public long getTimeTwo() {
        if (mPenalty == Penalty.PLUSTWO) {
            return mRawTime + 2000000000L;
        }
        return mRawTime;
    }

    public String getTimeString(boolean milliseconds) {
        switch (mPenalty) {
            case DNF:
                return "DNF";
            case PLUSTWO:
                return Util.timeStringFromNs(mRawTime + 2000000000L,
                        milliseconds) + "+";
            case NONE:
            default:
                return Util.timeStringFromNs(mRawTime, milliseconds);
        }
    }

    public String[] getTimeStringArray(boolean milliseconds) {
        switch (mPenalty) {
            case DNF:
                return new String[]{"DNF", ""};
            case PLUSTWO:
                long nanoseconds = mRawTime + 2000000000L;
                String[] timeStringsSplitByDecimal = Util
                        .timeStringsFromNsSplitByDecimal(nanoseconds,
                                milliseconds);
                timeStringsSplitByDecimal[1] = timeStringsSplitByDecimal[1] +
                        "+";
                return timeStringsSplitByDecimal;
            case NONE:
            default:
                return Util.timeStringsFromNsSplitByDecimal(mRawTime,
                        milliseconds);
        }
    }

    public String getDescriptiveTimeString(boolean milliseconds) {
        switch (mPenalty) {
            case DNF:
                if (mRawTime != 0) {
                    return "DNF (" + Util.timeStringFromNs(mRawTime,
                            milliseconds) + ")";
                }
            default:
                return getTimeString(milliseconds);
        }
    }

    public Penalty getPenalty() {
        return mPenalty;
    }

    public void setPenalty(Penalty penalty) {
        if (mPenalty != penalty) {
            mDataSetObservable.notifyChanged();
        }
        mPenalty = penalty;
    }

    public long getRawTime() {
        return mRawTime;
    }

    public void setRawTime(long time) {
        if (mRawTime != time) {
            mDataSetObservable.notifyChanged();
        }
        mRawTime = time;
    }

    public void registerObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterAll() {
        mDataSetObservable.unregisterAll();
    }

    public void unregisterObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    public enum Penalty {
        NONE, PLUSTWO, DNF
    }


}
