package com.pluscubed.plustimer.model;

import android.content.Context;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;
import net.gnehzr.tnoodle.utils.BadLazyClassDescriptionException;
import net.gnehzr.tnoodle.utils.LazyInstantiatorException;

import java.io.IOException;

/**
 * Puzzle type enum
 */
public enum PuzzleType {
    SQ1FAST("sq1fast", "Square-1", false),
    SKEWB("skewb", "Skewb"),
    PYRAMINX("pyram", "Pyraminx"),
    MINX("minx", "Megaminx"),
    CLOCK("clock", "Clock"),
    SEVEN("777", "7x7"),
    SIX("666", "6x6"),
    FIVE("555", "5x5"),
    FOUR("444", "4x4"),
    FOURFAST("444fast", "4x4", false),
    THREE("333", "3x3"),
    TWO("222", "2x2");
    public static final int CURRENT_SESSION = -1;
    public static final String CURRENT = "current_puzzletype";

    static {
        sCurrentPuzzleType = PuzzleType.THREE;
    }


    private static PuzzleType sCurrentPuzzleType;

    private final String mScramblerSpec;
    private final String mDisplayName;
    private boolean mOfficial;
    private Session mCurrentSession;
    private HistorySessions mHistorySessions;
    private Puzzle mPuzzle;

    PuzzleType(String scramblerSpec, String displayName) {
        this.mScramblerSpec = scramblerSpec;
        this.mDisplayName = displayName;
        mCurrentSession = new Session();
        mHistorySessions = new HistorySessions(mScramblerSpec + ".json");
        mOfficial = true;
    }


    PuzzleType(String scramblerSpec, String displayName, boolean official) {
        this(scramblerSpec, displayName);
        mOfficial = official;
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

    public HistorySessions getHistorySessions() {
        return mHistorySessions;
    }

    public void initHistorySessions(Context context) {
        mHistorySessions.init(context);
    }

    public boolean isOfficial() {
        return mOfficial;
    }

    public void submitCurrentSession(Context context) {
        mHistorySessions.addSession(mCurrentSession, context);
        resetCurrentSession();
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

    public Session getSession(int index) {
        if (index == CURRENT_SESSION) {
            if (mCurrentSession == null) mCurrentSession = new Session();
            return mCurrentSession;
        } else {
            return mHistorySessions.getList().get(index);
        }
    }

    public void resetCurrentSession() {
        mCurrentSession = null;
    }


}
