package com.pluscubed.plustimer.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import com.couchbase.lite.CouchbaseLiteException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pluscubed.plustimer.utils.Utils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import rx.Completable;

/**
 * Solve data object
 */
@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class Solve extends CbObject implements Parcelable {

    public static final String TYPE_SOLVE = "solve";

    public static final int PENALTY_DNF = 2;
    public static final int PENALTY_PLUSTWO = 1;
    public static final int PENALTY_NONE = 0;
    public static final Parcelable.Creator<Solve> CREATOR = new Parcelable.Creator<Solve>() {
        public Solve createFromParcel(Parcel source) {
            return new Solve(source);
        }

        public Solve[] newArray(int size) {
            return new Solve[size];
        }
    };

    @JsonProperty("scramble")
    private String mScramble;
    @JsonProperty("penalty")
    private int mPenalty;
    @JsonProperty("time")
    private long mTime;

    /**
     * Timestamp in milliseconds
     */
    @JsonProperty("timestamp")
    private long mTimestamp;


    public Solve(Solve s) {
        copy(s);
    }

    protected Solve(Context context) throws CouchbaseLiteException, IOException {
        super(context);

        init();
        updateCb(context);
    }

    /**
     * Creates new disconnected solve
     */
    public Solve() {
        init();
    }

    protected Solve(Parcel in) {
        this.mId = in.readString();
        this.mScramble = in.readString();
        this.mPenalty = in.readInt();
        this.mTime = in.readLong();
        this.mTimestamp = in.readLong();
    }

    private void init() {
        this.mScramble = "";
        this.mTime = 0L;
        this.mPenalty = PENALTY_NONE;
        this.mTimestamp = System.currentTimeMillis();
    }

    public String getId() {
        return mId;
    }

    protected void setId(String id) {
        mId = id;
    }

    /**
     * Doesn't copy ID
     */
    public void copy(Solve s) {
        mScramble = s.getScramble();
        mTime = s.getRawTime();
        mPenalty = s.getPenalty();
        mTimestamp = s.getTimestamp();
    }

    public String getScramble() {
        return mScramble;
    }

    public void setScramble(Context context, String scramble) throws CouchbaseLiteException, IOException {
        this.mScramble = scramble;
        updateCb(context);
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

    public void setPenalty(Context context, @Penalty int penalty) throws CouchbaseLiteException, IOException {
        this.mPenalty = penalty;
        updateCb(context);
    }

    public Completable setPenaltyDeferred(Context context, @Penalty int penalty) {
        return Completable.fromCallable(() -> {
            setPenalty(context, penalty);
            return null;
        });
    }

    public long getRawTime() {
        return mTime;
    }

    //TODO: Update database
    public void setRawTime(Context context, long time) throws CouchbaseLiteException, IOException {
        if (this.mTime != time) {
            this.mTime = time;
            updateCb(context);
        }
    }

    @Override
    public void updateCb(Context context) {
        super.updateCb(context);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mId);
        dest.writeString(this.mScramble);
        dest.writeInt(this.mPenalty);
        dest.writeLong(this.mTime);
        dest.writeLong(this.mTimestamp);
    }

    @Override
    protected String getType() {
        return TYPE_SOLVE;
    }

    @Override
    public String toString() {
        return "Solve{" +
                "time=" + mTime +
                ", scramble='" + mScramble + '\'' +
                ", penalty=" + mPenalty +
                ", timestamp=" + mTimestamp +
                '}';
    }

    @IntDef({PENALTY_DNF, PENALTY_PLUSTWO, PENALTY_NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Penalty {
    }

    protected static class Builder {
        protected Context context;
        protected Solve solve;

        protected Builder(Context context) {
            solve = new Solve();
            this.context = context;
        }

        protected Builder setRawTime(long time) {
            solve.mTime = time;
            return this;
        }

        protected Builder setTimestamp(long timestamp) {
            solve.mTime = timestamp;
            return this;
        }

        protected Builder setPenalty(@Penalty int penalty) {
            solve.mPenalty = penalty;
            return this;
        }

        protected Builder setScramble(String scramble) {
            solve.mScramble = scramble;
            return this;
        }

        protected Solve build() throws CouchbaseLiteException, IOException {
            solve.connectCb(context);
            return solve;
        }
    }
}
