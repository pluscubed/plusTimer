package com.pluscubed.plustimer;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
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
    private static final Type sSessionArrayListType;
    private static final String TAG = "PuzzleType";

    static {
        sSessionArrayListType = new TypeToken<ArrayList<Session>>() {
        }.getType();
        sCurrentPuzzleType=PuzzleType.THREE;
    }

    public static PuzzleType sCurrentPuzzleType;
    private final String mFilename;
    private final String mScramblerSpec;
    private final String mDisplayName;
    private Session mCurrentSession;
    private ArrayList<Session> mHistorySessionsList;
    private Puzzle mPuzzle;


    PuzzleType(String scramblerSpec, String displayName) {
        this.mScramblerSpec = scramblerSpec;
        this.mDisplayName = displayName;
        mCurrentSession = new Session();
        mFilename = mScramblerSpec + ".json";
    }

    public void submitCurrentSession(Context context) throws IOException {
        Writer writer = null;
        try {
            OutputStream out = context.openFileOutput(mFilename, Context.MODE_PRIVATE);
            writer = new OutputStreamWriter(out);
            ArrayList<Session> historySessions = getHistorySessions(context);
            historySessions.add(mCurrentSession);
            new Gson().toJson(historySessions, sSessionArrayListType, writer);
        } finally {
            if (writer != null) writer.close();
        }
        resetCurrentSession();
        updateHistorySessionsFromFile();
    }

    public void updateHistorySessionsFromFile() {
        mHistorySessionsList = null;
    }

    public ArrayList<Session> getHistorySessions(Context context) throws IOException {
        if (mHistorySessionsList == null) {
            BufferedReader reader = null;
            try {
                InputStream in = context.openFileInput(mFilename);
                reader = new BufferedReader(new InputStreamReader(in));
                mHistorySessionsList = new Gson().fromJson(reader, sSessionArrayListType);
            } catch (FileNotFoundException e) {
                Log.e(TAG, mDisplayName + ": Session history file not found");
            } finally {
                if (reader != null) reader.close();
                if (mHistorySessionsList == null) {
                    mHistorySessionsList = new ArrayList<Session>();
                }
            }
        }
        return mHistorySessionsList;
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
        if (mCurrentSession == null)
            mCurrentSession = new Session();
        return mCurrentSession;
    }

    public void resetCurrentSession() {
        mCurrentSession = null;
    }

}
