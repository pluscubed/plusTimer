package com.pluscubed.plustimer.model;

import android.content.Context;

import com.pluscubed.plustimer.R;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;
import net.gnehzr.tnoodle.utils.BadLazyClassDescriptionException;
import net.gnehzr.tnoodle.utils.LazyInstantiatorException;

import java.io.IOException;

/**
 * Puzzle type enum
 */
public enum PuzzleType {
    SQ1FAST("sq1fast", 0, false),
    SKEWB("skewb", 1),
    PYRAMINX("pyram", 2),
    MINX("minx", 3),
    CLOCK("clock", 4),
    SEVEN("777", 5),
    SIX("666", 6),
    FIVE("555", 7),
    FOUR("444", 8),
    FOURFAST("444fast", 9, false),
    THREE("333", 10),
    TWO("222", 11);

    public static final int CURRENT_SESSION = -1;

    public static final String CURRENT = "current_puzzletype";

    static {
        sCurrentPuzzleType = PuzzleType.THREE;
    }


    private static PuzzleType sCurrentPuzzleType;

    private final String mScramblerSpec;

    private final int mIndex;

    private String mDisplayName;

    private boolean mOfficial;

    private Session mCurrentSession;

    private HistorySessions mHistorySessions;

    private Puzzle mPuzzle;

    PuzzleType(String scramblerSpec, int index) {
        mScramblerSpec = scramblerSpec;
        mIndex = index;
        mCurrentSession = new Session();
        mHistorySessions = new HistorySessions(mScramblerSpec + ".json");
        mOfficial = true;
    }


    PuzzleType(String scramblerSpec, int index, boolean official) {
        this(scramblerSpec, index);
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

    public void init(Context context) {
        mHistorySessions.init(context);
        mDisplayName = context.getResources().getStringArray(R.array.puzzle_types)[mIndex];
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
            if (mCurrentSession == null) {
                mCurrentSession = new Session();
            }
            return mCurrentSession;
        } else {
            return mHistorySessions.getList().get(index);
        }
    }

    public void resetCurrentSession() {
        mCurrentSession = null;
    }

}
