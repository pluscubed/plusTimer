package com.pluscubed.plustimer.model;

import android.support.annotation.IntDef;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pluscubed.plustimer.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Solve data object
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class Solve {
    public static final int PENALTY_DNF = 2;
    public static final int PENALTY_PLUSTWO = 1;
    public static final int PENALTY_NONE = 0;

    @JsonIgnore
    private String mId;

    @JsonProperty("scramble")
    private String mScramble;
    @JsonProperty("penalty")
    private int mPenalty;
    @JsonProperty("time")
    private long mTime;
    @JsonProperty("timestamp")
    private long mTimestamp;

    public Solve() {
    }

    public Solve(Solve s) {
        copy(s);
    }

    public Solve(String scramble, long time) {
        this.mScramble = scramble;
        this.mTime = time;
        this.mPenalty = PENALTY_NONE;
        this.mTimestamp = System.currentTimeMillis();
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public void copy(Solve s) {
        mScramble = s.getScramble();
        mTime = s.getRawTime();
        mPenalty = s.getPenalty();
        mTimestamp = s.getTimestamp();
        mId = s.getId();
    }

    public String getScramble() {
        return mScramble;
    }

    public void setScramble(String scramble) {
        this.mScramble = scramble;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public long getTimeTwo() {
        return mTime + (mPenalty == PENALTY_PLUSTWO ? 2000000000L : 0);
    }

    public String getTimeString(boolean milliseconds) {
        switch (mPenalty) {
            case PENALTY_DNF:
                return "DNF";
            case PENALTY_PLUSTWO:
                return Utils.timeStringFromNs(mTime + 2000000000L,
                        milliseconds) + "+";
            case PENALTY_NONE:
            default:
                return Utils.timeStringFromNs(mTime, milliseconds);
        }
    }

    public String[] getTimeStringArray(boolean milliseconds) {
        switch (mPenalty) {
            case PENALTY_DNF:
                return new String[]{"DNF", ""};
            case PENALTY_PLUSTWO:
                long nanoseconds = mTime + 2000000000L;
                String[] timeStringsSplitByDecimal = Utils
                        .timeStringsFromNsSplitByDecimal(nanoseconds,
                                milliseconds);
                timeStringsSplitByDecimal[1] = timeStringsSplitByDecimal[1] +
                        "+";
                return timeStringsSplitByDecimal;
            case PENALTY_NONE:
            default:
                return Utils.timeStringsFromNsSplitByDecimal(mTime,
                        milliseconds);
        }
    }

    public String getDescriptiveTimeString(boolean milliseconds) {
        switch (mPenalty) {
            case PENALTY_DNF:
                if (mTime != 0) {
                    return "DNF (" + Utils.timeStringFromNs(mTime,
                            milliseconds) + ")";
                }
            default:
                return getTimeString(milliseconds);
        }
    }

    @Penalty
    public int getPenalty() {
        return mPenalty;
    }

    public void setPenalty(@Penalty int penalty) {
        this.mPenalty = penalty;
    }

    public long getRawTime() {
        return mTime;
    }

    public void setRawTime(long time) {
        if (this.mTime != time) {
            this.mTime = time;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Solve &&
                mId.equals(((Solve) o).getId()) &&
                mTime == ((Solve) o).getRawTime() &&
                mTimestamp == ((Solve) o).getTimestamp() &&
                mScramble.equals(((Solve) o).getScramble()) &&
                mPenalty == ((Solve) o).getPenalty();
    }

    @IntDef({PENALTY_DNF, PENALTY_PLUSTWO, PENALTY_NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Penalty {
    }


}
