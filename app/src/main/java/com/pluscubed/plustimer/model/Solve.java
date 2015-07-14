package com.pluscubed.plustimer.model;

import android.database.Cursor;
import android.support.annotation.IntRange;

import com.pluscubed.plustimer.sql.SolveDataSource;
import com.pluscubed.plustimer.utils.Utils;

/**
 * Solve data object
 */
public class Solve {

    private ScrambleAndSvg mScrambleAndSvg;

    private Penalty mPenalty;

    private long mRawTime;

    private long mTimestamp;

    private transient Session mAttachedSession;

    public Solve(Solve s) {
        copy(s);
    }

    public Solve(Cursor cursor){
        String scramble = cursor.getString(cursor.getColumnIndex(SolveDataSource.SolveDbEntry.COLUMN_NAME_SCRAMBLE));
        mScrambleAndSvg = new ScrambleAndSvg(scramble, null);
        int penalty = cursor.getInt(cursor.getColumnIndex(SolveDataSource.SolveDbEntry.COLUMN_NAME_PENALTY));
        setPenaltyInt(penalty);
        mRawTime = cursor.getLong(cursor.getColumnIndex(SolveDataSource.SolveDbEntry.COLUMN_NAME_TIME));
        mTimestamp = cursor.getLong(cursor.getColumnIndex(SolveDataSource.SolveDbEntry.COLUMN_NAME_TIMESTAMP));
    }

    public Solve(ScrambleAndSvg scramble, long time) {
        mScrambleAndSvg = scramble;
        mRawTime = time;
        mPenalty = Penalty.NONE;
        mTimestamp = System.currentTimeMillis();
    }

    public void copy(Solve s) {
        mScrambleAndSvg = s.getScrambleAndSvg();
        mRawTime = s.getRawTime();
        mPenalty = s.getPenalty();
        mTimestamp = s.getTimestamp();
        notifyChanged();
    }

    private void notifyChanged() {
        if (mAttachedSession != null) {
            mAttachedSession.notifySolveChanged(mAttachedSession.getSolves().indexOf(this));
        }
    }

    public ScrambleAndSvg getScrambleAndSvg() {
        return mScrambleAndSvg;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public long getTimeTwo() {
        return mRawTime + (mPenalty == Penalty.PLUSTWO ? 2000000000L : 0);
    }

    public String getTimeString(boolean milliseconds) {
        switch (mPenalty) {
            case DNF:
                return "DNF";
            case PLUSTWO:
                return Utils.timeStringFromNs(mRawTime + 2000000000L,
                        milliseconds) + "+";
            case NONE:
            default:
                return Utils.timeStringFromNs(mRawTime, milliseconds);
        }
    }

    public String[] getTimeStringArray(boolean milliseconds) {
        switch (mPenalty) {
            case DNF:
                return new String[]{"DNF", ""};
            case PLUSTWO:
                long nanoseconds = mRawTime + 2000000000L;
                String[] timeStringsSplitByDecimal = Utils
                        .timeStringsFromNsSplitByDecimal(nanoseconds,
                                milliseconds);
                timeStringsSplitByDecimal[1] = timeStringsSplitByDecimal[1] +
                        "+";
                return timeStringsSplitByDecimal;
            case NONE:
            default:
                return Utils.timeStringsFromNsSplitByDecimal(mRawTime,
                        milliseconds);
        }
    }

    public String getDescriptiveTimeString(boolean milliseconds) {
        switch (mPenalty) {
            case DNF:
                if (mRawTime != 0) {
                    return "DNF (" + Utils.timeStringFromNs(mRawTime,
                            milliseconds) + ")";
                }
            default:
                return getTimeString(milliseconds);
        }
    }

    public void attachSession(Session session) {
        mAttachedSession = session;
    }

    public Penalty getPenalty() {
        return mPenalty;
    }

    public void setPenalty(Penalty penalty) {
        if (mPenalty != penalty) {
            mPenalty = penalty;
            notifyChanged();
        }
    }

    @IntRange(from=0,to=2)
    public int getPenaltyInt(){
        switch(mPenalty){
            case NONE:
                return 0;
            case PLUSTWO:
                return 1;
            case DNF:
                return 2;
        }
        return 0;
    }

    public void setPenaltyInt(@IntRange(from=0,to=2) int penalty){
        switch(penalty){
            case 0:
                mPenalty = Penalty.NONE;
                break;
            case 1:
                mPenalty = Penalty.PLUSTWO;
                break;
            case 2:
                mPenalty = Penalty.DNF;
                break;
        }
    }

    public long getRawTime() {
        return mRawTime;
    }

    public void setRawTime(long time) {
        if (mRawTime != time) {
            mRawTime = time;
            notifyChanged();
        }
    }

    public enum Penalty {
        NONE, PLUSTWO, DNF
    }


}
