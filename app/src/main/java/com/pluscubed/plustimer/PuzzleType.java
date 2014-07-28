package com.pluscubed.plustimer;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;
import net.gnehzr.tnoodle.utils.BadLazyClassDescriptionException;
import net.gnehzr.tnoodle.utils.LazyInstantiatorException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Puzzle type enum
 */
public enum PuzzleType {
    //TODO: find faster square-1 scrambler
    //SQ1("sq1", "Square 1"),
    SKEWB("skewb", "Skewb"),
    PYRAMINX("pyram", "Pyraminx"),
    MINX("minx", "Megaminx"),
    CLOCK("clock", "Clock"),
    SEVEN("777", "7x7"),
    SIX("666", "6x6"),
    FIVE("555", "5x5"),
    FOURFAST("444fast", "4x4-fast"),
    THREE("333", "3x3"),
    TWO("222", "2x2");
    public static final int CURRENT_SESSION = -1;
    public static final String CURRENT = "current_puzzletype";
    private static final Type SESSION_LIST_TYPE;
    private static final String TAG = "PuzzleType";

    static {
        SESSION_LIST_TYPE = new TypeToken<List<Session>>() {
        }.getType();
        sCurrentPuzzleType = PuzzleType.THREE;
        gson = new GsonBuilder()
                .registerTypeAdapter(ScrambleAndSvg.class, new ScrambleAndSvg.Serializer())
                .registerTypeAdapter(ScrambleAndSvg.class, new ScrambleAndSvg.Deserializer())
                .setPrettyPrinting()
                .create();
    }

    private static final Gson gson;
    private static PuzzleType sCurrentPuzzleType;
    private final String mFilename;
    private final String mScramblerSpec;
    private final String mDisplayName;
    private Session mCurrentSession;
    private List<Session> mHistorySessionsList;
    private Puzzle mPuzzle;


    PuzzleType(String scramblerSpec, String displayName) {
        this.mScramblerSpec = scramblerSpec;
        this.mDisplayName = displayName;
        mCurrentSession = new Session();
        mFilename = mScramblerSpec + ".json";
    }

    public static void setCurrentPuzzleType(PuzzleType p) {
        sCurrentPuzzleType = p;
    }

    public static PuzzleType get(String displayName) {
        if (displayName.equals(CURRENT)) {
            return sCurrentPuzzleType;
        }
        for (PuzzleType i : PuzzleType.values()) {
            if (i.toString().equals(displayName)) {
                return i;
            }
        }
        return null;
    }

    public String toSessionText(Context context, Session session, boolean current) {
        StringBuilder s = new StringBuilder(mDisplayName).append("\n\n")
                .append("Number of Solves: ").append(session.getNumberOfSolves()).append("\n");
        if (session.getNumberOfSolves() > 0) {
            s.append("Best: ").append(Session.getBestSolve(session.getSolves()).getDescriptiveTimeString()).append("\n")
                    .append("Worst: ").append(Session.getWorstSolve(session.getSolves()).getDescriptiveTimeString()).append("\n")
                    .append("Mean: ").append(session.getStringMean()).append("\n");
            int[] averages = {1000, 100, 50, 12, 5};
            for (int i : averages) {
                if (session.getNumberOfSolves() >= i) {
                    if (current) {
                        s.append(context.getString(R.string.cao)).append(i).append(": ").append(session.getStringCurrentAverageOf(i)).append("\n");
                    } else {
                        s.append(context.getString(R.string.lao)).append(i).append(": ").append(session.getStringCurrentAverageOf(i)).append("\n");
                    }
                    s.append(context.getString(R.string.bao)).append(i).append(": ").append(session.getStringBestAverageOf(i)).append("\n");
                }
            }
            s.append("\n\n");
            int c = 1;
            for (Solve i : session.getSolves()) {
                s.append(c).append(". ").append(i.getDescriptiveTimeString()).append("\n")
                        .append("     ").append(Solve.timeDateStringFromTimestamp(context, i.getTimestamp())).append("\n")
                        .append("     ").append(i.getScrambleAndSvg().scramble).append("\n\n");
                c++;
            }
        }
        return s.toString();
    }

    public void submitCurrentSession() {
        mHistorySessionsList.add(mCurrentSession);
        resetCurrentSession();
    }

    public void saveHistorySessionsToFile(Context context) {
        Writer writer = null;
        try {
            OutputStream out = context.openFileOutput(mFilename, Context.MODE_PRIVATE);
            writer = new OutputStreamWriter(out);
            gson.toJson(mHistorySessionsList, SESSION_LIST_TYPE, writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Session> getHistorySessions(Context context) {
        if (mHistorySessionsList == null) {
            BufferedReader reader = null;
            try {
                InputStream in = context.openFileInput(mFilename);
                reader = new BufferedReader(new InputStreamReader(in));
                mHistorySessionsList = gson.fromJson(reader, SESSION_LIST_TYPE);
            } catch (FileNotFoundException e) {
                Log.e(TAG, mDisplayName + ": Session history file not found");
            } finally {
                if (reader != null) try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mHistorySessionsList == null) {
                    Log.e(TAG, "found null history list");
                    mHistorySessionsList = new ArrayList<Session>();
                }
            }
        }
        return new ArrayList<Session>(Collections.unmodifiableList(mHistorySessionsList));
    }

    public Puzzle getPuzzle() {
        if (mPuzzle == null) {
            try {
                mPuzzle = PuzzlePlugins.getScramblers().get(mScramblerSpec).cachedInstance();
            } catch (LazyInstantiatorException e) {
                e.printStackTrace();
            } catch (BadLazyClassDescriptionException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mPuzzle;
    }


    @Override
    public String toString() {
        return mDisplayName;
    }

    public Session getCurrentSession() {
        if (mCurrentSession == null) mCurrentSession = new Session();
        return mCurrentSession;
    }

    public Session getSession(int index, Context context) {
        if (index == CURRENT_SESSION) {
            if (mCurrentSession == null) mCurrentSession = new Session();
            return mCurrentSession;
        } else {
            return getHistorySessions(context).get(index);
        }
    }

    public void resetCurrentSession() {
        mCurrentSession = null;
    }

    public void deleteHistorySession(int index, Context context) {
        mHistorySessionsList.remove(index);
        saveHistorySessionsToFile(context);
    }

    public void deleteHistorySession(Session session, Context context) {
        mHistorySessionsList.remove(session);
        saveHistorySessionsToFile(context);

    }
}
