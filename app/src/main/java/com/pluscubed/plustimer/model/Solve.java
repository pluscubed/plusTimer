package com.pluscubed.plustimer.model;

import android.support.annotation.IntDef;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pluscubed.plustimer.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Solve data object
 */
public class Solve {

    public static final int PENALTY_DNF = 2;
    public static final int PENALTY_PLUSTWO = 1;
    public static final int PENALTY_NONE = 0;
    @JsonIgnore
    private String mId;
    private String scramble;
    private int penalty;
    private long time;
    private long timestamp;
    public Solve(Solve s) {
        copy(s);
    }

    public Solve(String scramble, long time) {
        this.scramble = scramble;
        this.time = time;
        this.penalty = PENALTY_NONE;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public void copy(Solve s) {
        scramble = s.getScramble();
        time = s.getRawTime();
        penalty = s.getPenalty();
        timestamp = s.getTimestamp();
        mId = s.getId();
    }

    public String getScramble() {
        return scramble;
    }

    public void setScramble(String scramble) {
        this.scramble = scramble;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getTimeTwo() {
        return time + (penalty == PENALTY_PLUSTWO ? 2000000000L : 0);
    }

    public String getTimeString(boolean milliseconds) {
        switch (penalty) {
            case PENALTY_DNF:
                return "DNF";
            case PENALTY_PLUSTWO:
                return Utils.timeStringFromNs(time + 2000000000L,
                        milliseconds) + "+";
            case PENALTY_NONE:
            default:
                return Utils.timeStringFromNs(time, milliseconds);
        }
    }

    public String[] getTimeStringArray(boolean milliseconds) {
        switch (penalty) {
            case PENALTY_DNF:
                return new String[]{"DNF", ""};
            case PENALTY_PLUSTWO:
                long nanoseconds = time + 2000000000L;
                String[] timeStringsSplitByDecimal = Utils
                        .timeStringsFromNsSplitByDecimal(nanoseconds,
                                milliseconds);
                timeStringsSplitByDecimal[1] = timeStringsSplitByDecimal[1] +
                        "+";
                return timeStringsSplitByDecimal;
            case PENALTY_NONE:
            default:
                return Utils.timeStringsFromNsSplitByDecimal(time,
                        milliseconds);
        }
    }

    public String getDescriptiveTimeString(boolean milliseconds) {
        switch (penalty) {
            case PENALTY_DNF:
                if (time != 0) {
                    return "DNF (" + Utils.timeStringFromNs(time,
                            milliseconds) + ")";
                }
            default:
                return getTimeString(milliseconds);
        }
    }

    @Penalty
    public int getPenalty() {
        return penalty;
    }

    public void setPenalty(@Penalty int penalty) {
        this.penalty = penalty;
    }

    public long getRawTime() {
        return time;
    }

    public void setRawTime(long time) {
        if (this.time != time) {
            this.time = time;
        }
    }

    @IntDef({PENALTY_DNF, PENALTY_PLUSTWO, PENALTY_NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Penalty {
    }


}
