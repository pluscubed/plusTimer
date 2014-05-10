package com.pluscubed.plustimer;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * solve times data object
 */
public class Solve {
    private ScrambleAndSvg mScrambleAndSvg;
    private Penalty mPenalty;
    private long mRawTime;
    private long mTimestamp;

    public Solve(ScrambleAndSvg scramble, long time) {
        mScrambleAndSvg = scramble;
        mRawTime = time;
        mPenalty = Penalty.NONE;
        mTimestamp = System.currentTimeMillis();
    }

    //TODO: Localization of timeStringFromlong
    public static String timeStringFromLong(long nano) {
        int minutes = (int) ((nano / (60 * 1000000000L)) % 60);
        int hours = (int) ((nano / (3600 * 1000000000L)) % 24);
        float seconds = (nano / 1000000000F) % 60;

        if (hours != 0) {
            return String.format("%d:%02d:%06.3f", hours, minutes, seconds);
        } else if (minutes != 0) {
            return String.format("%d:%06.3f", minutes, seconds);
        } else {
            return String.format("%.3f", seconds);
        }
    }

    public static String timeDateStringFromTimestamp(Context applicationContext, long timestamp) {
        String timeDate;
        String androidDateTime = android.text.format.DateFormat.getDateFormat(applicationContext).format(new Date(timestamp)) + " " +
                android.text.format.DateFormat.getTimeFormat(applicationContext).format(new Date(timestamp));
        String javaDateTime = DateFormat.getDateTimeInstance().format(new Date(timestamp));
        String AmPm = "";
        if (!Character.isDigit(androidDateTime.charAt(androidDateTime.length() - 1))) {
            if (androidDateTime.contains(new SimpleDateFormat().getDateFormatSymbols().getAmPmStrings()[Calendar.AM])) {
                AmPm = " " + new SimpleDateFormat().getDateFormatSymbols().getAmPmStrings()[Calendar.AM];
            } else {
                AmPm = " " + new SimpleDateFormat().getDateFormatSymbols().getAmPmStrings()[Calendar.PM];
            }
            androidDateTime = androidDateTime.replace(AmPm, "");
        }
        if (!Character.isDigit(javaDateTime.charAt(javaDateTime.length() - 1))) {
            javaDateTime = javaDateTime.replace(" " + new SimpleDateFormat().getDateFormatSymbols().getAmPmStrings()[Calendar.AM], "");
            javaDateTime = javaDateTime.replace(" " + new SimpleDateFormat().getDateFormatSymbols().getAmPmStrings()[Calendar.PM], "");
        }
        javaDateTime = javaDateTime.substring(javaDateTime.length() - 3);
        timeDate = androidDateTime.concat(javaDateTime);
        return timeDate.concat(AmPm);
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

    public String getTimeString() {
        switch (mPenalty) {
            case DNF:
                return "DNF";
            case PLUSTWO:
                return timeStringFromLong(mRawTime + 2000000000L) + "+";
            case NONE:
            default:
                return timeStringFromLong(mRawTime);
        }
    }

    public String getDescriptiveTimeString() {
        switch (mPenalty) {
            case DNF:
                return "DNF (" + timeStringFromLong(mRawTime) + ")";
            case PLUSTWO:
                return timeStringFromLong(mRawTime) + " +2";
            case NONE:
            default:
                return timeStringFromLong(mRawTime);
        }
    }

    public Penalty getPenalty() {
        return mPenalty;
    }

    public void setPenalty(Penalty penalty) {
        mPenalty = penalty;
    }

    public void setRawTime(long time) {
        this.mRawTime = time;
    }

    public enum Penalty {
        NONE, PLUSTWO, DNF
    }


}
