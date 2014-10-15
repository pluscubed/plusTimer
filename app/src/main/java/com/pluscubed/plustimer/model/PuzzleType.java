package com.pluscubed.plustimer.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.ui.SettingsActivity;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;
import net.gnehzr.tnoodle.utils.BadLazyClassDescriptionException;
import net.gnehzr.tnoodle.utils.LazyInstantiatorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enum for puzzle
 */
public enum PuzzleType {
    SQ1FAST("sq1fast", 0),
    SKEWB("skewb", 1),
    PYRAMINX("pyram", 2),
    MINX("minx", 3),
    CLOCK("clock", 4),
    SEVEN("777"),
    SIX("666"),
    FIVE("555"),
    FIVE_BLD("555ni"),
    FOUR("444"),
    FOUR_BLD("444ni"),
    FOURFAST("444fast"),
    THREE("333"),
    THREE_OH("333"),
    THREE_FEET("333"),
    THREE_BLD("333ni"),
    TWO("222");

    public static final int CURRENT_SESSION = -1;
    public static final String PREF_CURRENT_PUZZLETYPE = "current_puzzletype";

    private static final int NOT_SPECIAL_STRING = -1;
    private static PuzzleType sCurrentPuzzleType;
    public final String scramblerSpec;
    public final boolean official;
    private final int mStringIndex;
    private HistorySessions mHistorySessions;
    private Session mCurrentSession;
    private boolean mEnabled;
    private Puzzle mPuzzle;
    private boolean mInitialized;

    PuzzleType(String scramblerSpec, int stringIndex) {
        this.scramblerSpec = scramblerSpec;
        mStringIndex = stringIndex;
        official = !this.scramblerSpec.contains("fast");
        mHistorySessions = new HistorySessions(scramblerSpec + ".json");
    }

    PuzzleType(String scramblerSpec) {
        this(scramblerSpec, NOT_SPECIAL_STRING);
    }

    public static PuzzleType getCurrent() {
        return sCurrentPuzzleType;
    }

    public static void setCurrent(PuzzleType type, Context context) {
        sCurrentPuzzleType = type;
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit().putString(PREF_CURRENT_PUZZLETYPE, sCurrentPuzzleType.name()).apply();
    }

    public static List<PuzzleType> valuesExcludeDisabled() {
        List<PuzzleType> array = new ArrayList<PuzzleType>();
        for (PuzzleType i : values()) {
            if (i.isEnabled()) {
                array.add(i);
            }
        }
        return array;
    }

    public static void initialize(Context context) {
        if (sCurrentPuzzleType == null)
            sCurrentPuzzleType = valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_CURRENT_PUZZLETYPE, THREE.name()));
        for (PuzzleType puzzleType : values()) {
            puzzleType.init(context);
        }
    }

    public HistorySessions getHistorySessions() {
        return mHistorySessions;
    }

    private void init(Context context) {
        if (!mInitialized) {
            mHistorySessions.init(context);
            mEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.PREF_PUZZLETYPE_ENABLE_PREFIX + name().toLowerCase(), true);
        }
        mInitialized = true;
    }

    public void submitCurrentSession(Context context) {
        if (mCurrentSession != null && mCurrentSession.getNumberOfSolves() > 0) {
            mHistorySessions.addSession(mCurrentSession, context);
            resetCurrentSession();
        }
    }

    public Puzzle getPuzzle() {
        if (mPuzzle == null) {
            try {
                mPuzzle = PuzzlePlugins.getScramblers().get(scramblerSpec).cachedInstance();
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

    public String getUiName(Context context) {
        if (mStringIndex == NOT_SPECIAL_STRING) {
            int order = Integer.parseInt(scramblerSpec.substring(0, 1));
            String addon = null;
            if (name().contains("BLD")) {
                addon = context.getString(R.string.bld);
            }
            if (name().contains("FEET")) {
                addon = context.getString(R.string.feet);
            }
            if (name().contains("OH")) {
                addon = context.getString(R.string.oh);
            }
            if (addon != null) {
                return order + "x" + order + "-" + addon;
            } else {
                return order + "x" + order;
            }
        }
        return context.getResources().getStringArray(R.array.puzzles)[mStringIndex];
    }

    public Session getSession(int index) {
        if (index == CURRENT_SESSION) {
            //Check that if the current session is null (from reset or initializing)
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

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (this == sCurrentPuzzleType && !mEnabled) {
            for (PuzzleType i : PuzzleType.values()) {
                if (i.mEnabled) {
                    sCurrentPuzzleType = i;
                    break;
                }
            }
        }
    }
}
