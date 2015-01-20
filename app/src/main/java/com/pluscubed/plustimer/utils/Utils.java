package com.pluscubed.plustimer.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pluscubed.plustimer.R;
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

/**
 * Utilities class
 */
public class Utils {

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(Session.class, new Session.Deserializer())
                .create();
        SESSION_LIST_TYPE = new TypeToken<List<Session>>() {
        }.getType();
    }

    private static final Type SESSION_LIST_TYPE;
    private static Gson gson;

    public static int convertDpToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density
                + 0.5f);
    }

    public static void sendHistoryDataEmail(Context context) {
        BufferedReader r = null;
        StringBuilder total = new StringBuilder();
        for (PuzzleType p : PuzzleType.values()) {
            try {
                total.append("\n\n\n").append(p.name()).append("\n");
                InputStream in = context.openFileInput(p.name() + ".json");
                r = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (r != null) {
                    try {
                        r.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("mailto", "plusCubed@gmail.com", null));
        intent.putExtra(Intent.EXTRA_TEXT, total.toString());
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)));
    }

    /**
     * Save a list of sessions to a file.
     */
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
    public static void updateData(Context context, String fileName) {
        List<Session> historySessions = getSessionListFromFile(context, fileName);
        saveSessionListToFile(context, fileName, historySessions);
    }

    /**
     * For updating data w/ old JSON structure
     */
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
     * @param applicationContext the application context
     * @param timestamp          the timestamp to convert into a date & time
     *                           String
     * @return the String converted from the timestamp
     * @see android.text.format.DateFormat
     * @see java.text.DateFormat
     */
    public static String timeDateStringFromTimestamp(Context applicationContext, long timestamp) {
        String timeDate;
        String androidDateTime = android.text.format.DateFormat.getDateFormat
                (applicationContext)
                .format(new Date(timestamp)) + " " +
                android.text.format.DateFormat.getTimeFormat(applicationContext)
                        .format(new Date(timestamp));
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
    public static List<Long> getListTimeTwoNoDnf(List<Solve> list) {
        ArrayList<Long> timeTwo = new ArrayList<>();
        for (Solve i : list) {
            if (!(i.getPenalty() == Solve.Penalty.DNF)) {
                timeTwo.add(i.getTimeTwo());
            }
        }
        return timeTwo;
    }

    /**
     * Gets the best {@code Solve} out of the list (lowest time).
     * <p/>
     * If the list contains no solves, null is returned. If the list contains
     * only DNFs, the last
     * DNF solve is returned.
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
                    if (!(i.getPenalty() == Solve.Penalty.DNF) && i
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
     * <p/>
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
                if (i.getPenalty() == Solve.Penalty.DNF) {
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
    public static String wcaToSignNotation(String wca, String puzzleTypeName) {
        if (Character.isDigit(PuzzleType.valueOf(puzzleTypeName)
                .scramblerSpec.charAt(0))) {
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
    }

    /**
     * Converts a sequence of moves in SiGN notation to WCA notation
     *
     * @param sign the sequence of moves in SiGN notation
     * @return the converted sequence of moves in WCA notation
     */
    public static String signToWcaNotation(String sign, String puzzleTypeName) {
        if (Character.isDigit(PuzzleType.valueOf(puzzleTypeName)
                .scramblerSpec.charAt(0))) {
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
                if (i.getPenalty() == Solve.Penalty.DNF) dnfCount++;
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
}
