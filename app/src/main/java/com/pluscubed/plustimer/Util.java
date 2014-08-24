package com.pluscubed.plustimer;

import android.content.Context;

import com.pluscubed.plustimer.model.Solve;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Utitilies class
 */
public class Util {


    /**
     * Returns a String containing the date and time according the device's settings and locale from the timestamp
     *
     * @param applicationContext the Context used for android.text.format.DateFormat.getDateFormat(Context)
     * @param timestamp          the timestamp to convert into a date & time String
     * @return the String converted from the timestamp
     * @see android.text.format.DateFormat
     * @see java.text.DateFormat
     */
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

    /**
     * Returns a String containing hours, minutes, and seconds (to the millisecond) from a duration in nanoseconds.
     *
     * @param nanoseconds duration to be converted
     * @return the String converted from the nanoseconds
     */
    //TODO: Localization of timeStringFromNanoseconds
    public static String timeStringFromNanoseconds(long nanoseconds) {
        int minutes = (int) ((nanoseconds / (60 * 1000000000L)) % 60);
        int hours = (int) ((nanoseconds / (3600 * 1000000000L)) % 24);
        float seconds = (nanoseconds / 1000000000F) % 60;

        if (hours != 0) {
            return String.format("%d:%02d:%06.3f", hours, minutes, seconds);
        } else if (minutes != 0) {
            return String.format("%d:%06.3f", minutes, seconds);
        } else {
            return String.format("%.3f", seconds);
        }
    }


    public static List<Long> getListTimeTwoNoDnf(List<Solve> solveList) {
        ArrayList<Long> timeTwo = new ArrayList<Long>();
        for (Solve i : solveList) {
            if (!(i.getPenalty() == Solve.Penalty.DNF))
                timeTwo.add(i.getTimeTwo());
        }
        return timeTwo;
    }

    public static Solve getBestSolveOfList(List<Solve> list) {
        List<Solve> solveList = new ArrayList<Solve>(list);
        if (solveList.size() > 0) {
            Collections.reverse(solveList);
            List<Long> times = getListTimeTwoNoDnf(solveList);
            if (times.size() > 0) {
                long bestTimeTwo = Collections.min(times);
                for (Solve i : solveList) {
                    if (!(i.getPenalty() == Solve.Penalty.DNF) && i.getTimeTwo() == bestTimeTwo)
                        return i;
                }

            }
            return solveList.get(solveList.size() - 1);
        }
        return null;
    }

    public static Solve getWorstSolveOfList(List<Solve> list) {
        List<Solve> solveList = new ArrayList<Solve>(list);
        if (solveList.size() > 0) {
            Collections.reverse(solveList);
            for (Solve i : solveList) {
                if (i.getPenalty() == Solve.Penalty.DNF) {
                    return i;
                }
            }
            List<Long> times = getListTimeTwoNoDnf(solveList);
            if (times.size() > 0) {
                long worstTimeTwo = Collections.max(times);
                for (Solve i : solveList) {
                    if (i.getTimeTwo() == worstTimeTwo)
                        return i;
                }
            }
        }
        return null;
    }
}
