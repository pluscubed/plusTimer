package com.pluscubed.plustimer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import rx.Single;

/**
 * Utilities class
 */
public class Utils {

    private static final Type SESSION_LIST_TYPE;
    private static Gson gson;

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(Session.class, (JsonDeserializer<Session>) (json, typeOfT, context) -> {
                    Session s = new Gson().fromJson(json, typeOfT);
                    //TODO: Something something legacy
                    /*for (final Solve solve : s.mSolves) {
                        solve.attachSession(s);
                    }*/
                    return s;
                })
                .create();
        SESSION_LIST_TYPE = new TypeToken<List<Session>>() {
        }.getType();
    }

    public static int convertDpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density
                + 0.5f);
    }

    public static int compare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static void lockOrientation(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        int rotation = display.getRotation();
        int tempOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        switch (tempOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
        activity.setRequestedOrientation(orientation);
    }

    public static void unlockOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    /**
     * Save a list of sessions to a file.
     */
    @Deprecated
    public static void saveSessionListToFile(Context context,
                                             String fileName,
                                             List<Session> sessionList) {
        if (sessionList.size() >= 1) {
            Writer writer = null;
            try {
                OutputStream out = context.openFileOutput(fileName,
                        Context.MODE_PRIVATE);
                writer = new OutputStreamWriter(out);
                gson.toJson(sessionList, SESSION_LIST_TYPE, writer);
            } catch (FileNotFoundException e) {
                //File not found: create new file
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Get list and save new list
     */
    @Deprecated
    private static void updateData(Context context, String fileName) {
        List<Session> historySessions = getSessionListFromFile(context, fileName);
        saveSessionListToFile(context, fileName, historySessions);
    }

    /**
     * For updating data w/ old JSON structure
     */
    @Deprecated
    public static void updateData(Context context, String fileName, Gson oldGson) {
        Gson current = gson;
        gson = oldGson;
        updateData(context, fileName);
        gson = current;
    }

    /**
     * Load up the sessions stored in the list. If the file doesn't exist,
     * create an empty list.
     */
    @Deprecated
    public static List<Session> getSessionListFromFile(Context context,
                                                       String fileName) {
        BufferedReader reader = null;
        try {
            InputStream in = context.openFileInput(fileName);
            reader = new BufferedReader(new InputStreamReader(in));
            List<Session> fileSessions = gson.fromJson(reader, SESSION_LIST_TYPE);
            if (fileSessions != null) {
                return fileSessions;
            }
        } catch (FileNotFoundException e) {
            //File not found: create empty list
        } catch (JsonSyntaxException e) {
            ErrorUtils.showJsonSyntaxError(context, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new ArrayList<>();
    }


    /**
     * Returns a String containing the date and time according the device's
     * settings and locale from
     * the timestamp
     *
     * @param context   the context
     * @param timestamp the timestamp to convert into a date & time
     *                  String
     * @return the String converted from the timestamp
     * @see android.text.format.DateFormat
     * @see java.text.DateFormat
     */
    public static String dateTimeSecondsStringFromTimestamp(Context context, long timestamp) {
        String timeDate;
        String androidDateTime = dateTimeStringFromTimestamp(context, timestamp);
        String javaDateTime = DateFormat.getDateTimeInstance().format(new
                Date(timestamp));
        String AmPm = "";
        if (!Character.isDigit(androidDateTime.charAt(androidDateTime.length
                () - 1))) {
            if (androidDateTime.contains(
                    new SimpleDateFormat().getDateFormatSymbols()
                            .getAmPmStrings()[Calendar.AM])) {
                AmPm = " " + new SimpleDateFormat().getDateFormatSymbols()
                        .getAmPmStrings()[Calendar.AM];
            } else {
                AmPm = " " + new SimpleDateFormat().getDateFormatSymbols()
                        .getAmPmStrings()[Calendar.PM];
            }
            androidDateTime = androidDateTime.replace(AmPm, "");
        }
        if (!Character.isDigit(javaDateTime.charAt(javaDateTime.length() - 1)
        )) {
            javaDateTime = javaDateTime.replace(" " + new SimpleDateFormat()
                    .getDateFormatSymbols()
                    .getAmPmStrings()[Calendar.AM], "");
            javaDateTime = javaDateTime.replace(" " + new SimpleDateFormat()
                    .getDateFormatSymbols()
                    .getAmPmStrings()[Calendar.PM], "");
        }
        javaDateTime = javaDateTime.substring(javaDateTime.length() - 3);
        timeDate = androidDateTime.concat(javaDateTime);
        return timeDate.concat(AmPm);
    }

    @NonNull
    public static String dateTimeStringFromTimestamp(Context context, long timestamp) {
        return android.text.format.DateFormat.getDateFormat(context).format(new Date(timestamp))
                + " " +
                android.text.format.DateFormat.getTimeFormat(context).format(new Date(timestamp));
    }

    /**
     * Returns a String containing hours, minutes,
     * and seconds (to the millisecond) from a duration
     * in nanoseconds.
     *
     * @param nanoseconds the duration to be converted
     * @return the String converted from the nanoseconds
     */
    //TODO: Localization of timeStringFromNs
    public static String timeStringFromNs(long nanoseconds,
                                          boolean enableMilliseconds) {
        String[] array = timeStringsFromNsSplitByDecimal(nanoseconds,
                enableMilliseconds);
        return array[0] + "." + array[1];
    }

    public static String timeStringSecondsFromNs(long nanoseconds,
                                                 boolean enableMilliseconds) {
        double seconds;
        if (enableMilliseconds) {
            seconds = Math.floor(nanoseconds / 1000000000.0 * 1000.0) / 1000.0;
        } else {
            seconds = Math.floor(nanoseconds / 1000000000.0 * 100.0) / 100.0;
        }
        if (seconds == (long) seconds)
            return String.format("%d", (long) seconds);
        else
            return String.valueOf(seconds);
    }


    public static String[] timeStringsFromNsSplitByDecimal(long nanoseconds,
                                                           boolean enableMilliseconds) {
        String[] array = new String[2];

        int hours = (int) ((nanoseconds / 1000000000L / 60 / 60) % 24);
        int minutes = (int) ((nanoseconds / 1000000000L / 60) % 60);
        int seconds = (int) ((nanoseconds / 1000000000L) % 60);

        // 0x is saying add zeroes for how many digits
        if (hours != 0) {
            array[0] = String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes != 0) {
            array[0] = String.format("%d:%02d", minutes, seconds);
        } else {
            array[0] = String.format("%d", seconds);
        }

        if (enableMilliseconds) {
            array[1] = String.format("%03d",
                    (int) (((nanoseconds / 1000000.0) % 1000.0)));
        } else {
            array[1] = String.format("%02d",
                    (int) ((nanoseconds / 10000000.0) % 100.0));
        }

        return array;
    }

    /**
     * Gets a list of times (calculated with +2s) from the list of {@code
     * Solve}s, excluding DNFs.
     * If no times are found, an empty list is returned.
     *
     * @param list the list of solves to extract times from
     * @return the list of nanoseconds of times
     */
    private static List<Long> getListTimeTwoNoDnf(List<Solve> list) {
        ArrayList<Long> timeTwo = new ArrayList<>();
        for (Solve i : list) {
            if (!(i.getPenalty() == Solve.PENALTY_DNF)) {
                timeTwo.add(i.getTimeTwo());
            }
        }
        return timeTwo;
    }

    /**
     * Gets the best {@code Solve} out of the list (lowest time).
     * <p>
     * If the list contains no solves, null is returned. If the list contains
     * only DNFs, the last DNF solve is returned.
     *
     * @param list the list of solves, not empty
     * @return the solve with the lowest time
     */
    public static Solve getBestSolveOfList(List<Solve> list) {
        List<Solve> solveList = new ArrayList<>(list);
        if (solveList.size() > 0) {
            Collections.reverse(solveList);
            List<Long> times = getListTimeTwoNoDnf(solveList);
            if (times.size() > 0) {
                long bestTimeTwo = Collections.min(times);
                for (Solve i : solveList) {
                    if (!(i.getPenalty() == Solve.PENALTY_DNF) && i
                            .getTimeTwo() == bestTimeTwo) {
                        return i;
                    }
                }

            }
            return solveList.get(0);
        }
        return null;
    }

    /**
     * Gets the worst {@code Solve} out of the list (highest time).
     * <p>
     * If the list contains DNFs, the last DNF solve is returned.
     * If the list contains no solves, null is returned.
     *
     * @param list the list of solves, not empty
     * @return the solve with the highest time
     */
    public static Solve getWorstSolveOfList(List<Solve> list) {
        List<Solve> solveList = new ArrayList<>(list);
        if (solveList.size() > 0) {
            Collections.reverse(solveList);
            for (Solve i : solveList) {
                if (i.getPenalty() == Solve.PENALTY_DNF) {
                    return i;
                }
            }
            List<Long> times = getListTimeTwoNoDnf(solveList);
            if (times.size() > 0) {
                long worstTimeTwo = Collections.max(times);
                for (Solve i : solveList) {
                    if (i.getTimeTwo() == worstTimeTwo) {
                        return i;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Converts a sequence of moves in WCA notation to SiGN notation
     *
     * @param wca the sequence of moves in WCA notation
     * @return the converted sequence of moves in SiGN notation
     */
    public static Single<String> wcaToSignNotation(Context context, String wca, String puzzleTypeId) {
        return PuzzleType.get(context, puzzleTypeId)
                .map(puzzleType -> {
                    if (Character.isDigit(puzzleType.getScrambler().charAt(0))) {
                        String[] moves = wca.split(" ");
                        for (int i = 0; i < moves.length; i++) {
                            if (moves[i].contains("w")) {
                                moves[i] = moves[i].replace("w", "");
                                moves[i] = moves[i].toLowerCase();
                            }
                        }
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < moves.length; i++) {
                            builder.append(moves[i]);
                            if (i != moves.length - 1) builder.append(" ");
                        }
                        return builder.toString();
                    } else {
                        return wca;
                    }
                });
    }

    /**
     * Converts a sequence of moves in SiGN notation to WCA notation
     *
     * @param sign the sequence of moves in SiGN notation
     * @return the converted sequence of moves in WCA notation
     */
    public static Single<String> signToWcaNotation(Context context, String sign, String puzzleTypeId) {
        return PuzzleType.get(context, puzzleTypeId)
                .map(puzzleType -> {
                    if (Character.isDigit(puzzleType.getScrambler().charAt(0))) {
                        String[] moves = sign.split(" ");
                        for (int i = 0; i < moves.length; i++) {
                            if (!moves[i].equals(moves[i].toUpperCase())) {
                                char[] possibleMoves = "udfrlb".toCharArray();
                                for (char move : possibleMoves) {
                                    moves[i] = moves[i].replace(String.valueOf(move),
                                            Character.toUpperCase(move) + "w");
                                }
                            }
                        }
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < moves.length; i++) {
                            builder.append(moves[i]);
                            if (i != moves.length - 1) builder.append(" ");
                        }
                        return builder.toString();
                    } else {
                        return sign;
                    }
                });
    }

    /**
     * Gets the average of a list of solves, excluding the best and worst
     * solves (5%).
     * Returns {@link Long#MAX_VALUE} for DNF and {@link
     * Session#GET_AVERAGE_INVALID_NOT_ENOUGH} if the list size is less than 3.
     *
     * @param list the list of solves
     * @return the average of the solves
     */
    public static long getAverageOf(List<Solve> list) {
        if (list.size() >= 3) {
            int trim = (int) Math.ceil(list.size() / 20d);

            List<Long> times = getListTimeTwoNoDnf(list);

            int dnfCount = 0;
            for (Solve i : list) {
                if (i.getPenalty() == Solve.PENALTY_DNF) dnfCount++;
            }

            //If the number of DNFs can be cut off by the trim
            if (dnfCount <= trim) {
                Collections.sort(times);
                times = times.subList(trim, times.size() - trim + dnfCount);
                long sum = 0;
                for (long i : times) {
                    sum += i;
                }
                return sum / (times.size());
            }
            return Long.MAX_VALUE;
        }
        return Session.GET_AVERAGE_INVALID_NOT_ENOUGH;
    }

    //Taken from http://stackoverflow.com/questions/19908003
    public static int getTextViewHeight(TextView textView) {
        WindowManager wm =
                (WindowManager) textView.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int deviceWidth;

        Point size = new Point();
        display.getSize(size);
        deviceWidth = size.x;

        int widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(deviceWidth, View.MeasureSpec.AT_MOST);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        return textView.getMeasuredHeight();
    }

    public static Single<String> getUiScramble(Context context, String scramble,
                                               boolean signEnabled,
                                               String puzzleTypeId) {
        return signEnabled ? Utils.wcaToSignNotation(context, scramble, puzzleTypeId) : Single.just(scramble);
    }


}
