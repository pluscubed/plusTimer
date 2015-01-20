package com.pluscubed.plustimer.model;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;
import net.gnehzr.tnoodle.utils.BadLazyClassDescriptionException;
import net.gnehzr.tnoodle.utils.LazyInstantiatorException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private static final int NOT_SPECIAL_STRING = -1;
    private static PuzzleType sCurrentPuzzleType;
    private static List<Observer> mObservers;

    static {
        mObservers = new ArrayList<>();
    }

    public final String scramblerSpec;
    public final boolean official;
    private final String currentSessionFileName;
    private final String historyFileName;
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
        historyFileName = name() + ".json";
        currentSessionFileName = name() + "-current.json";
        mHistorySessions = new HistorySessions(historyFileName);
    }

    PuzzleType(String scramblerSpec) {
        this(scramblerSpec, NOT_SPECIAL_STRING);
    }

    public static void registerObserver(Observer observer) {
        mObservers.add(observer);
    }

    public static void unregisterObserver(Observer observer) {
        mObservers.remove(observer);
    }

    public static void unregisterAllObservers() {
        mObservers.clear();
    }

    private static void notifyPuzzleTypeChanged() {
        for (Observer o : mObservers) {
            o.onPuzzleTypeChanged();
        }
    }

    public static PuzzleType getCurrent() {
        return sCurrentPuzzleType;
    }

    public static void setCurrent(PuzzleType type, Context context) {
        PrefUtils.saveCurrentPuzzleType(context, type.name());
        if (type != sCurrentPuzzleType) {
            sCurrentPuzzleType = type;
            notifyPuzzleTypeChanged();
        }
    }

    public static List<PuzzleType> valuesExcludeDisabled() {
        List<PuzzleType> array = new ArrayList<>();
        for (PuzzleType i : values()) {
            if (i.isEnabled()) {
                array.add(i);
            }
        }
        return array;
    }

    public synchronized static void initialize(Context context) {
        if (sCurrentPuzzleType == null)
            sCurrentPuzzleType = PrefUtils.getCurrentPuzzleType(context);
        for (PuzzleType puzzleType : values()) {
            puzzleType.init(context);
        }
        PrefUtils.saveVersionCode(context);
    }

    public HistorySessions getHistorySessions() {
        return mHistorySessions;
    }

    private synchronized void init(Context context) {
        if (!mInitialized) {

            //AFTER UPDATING APP////////////
            int savedVersionCode = PrefUtils.getVersionCode(context);

            if (savedVersionCode <= 10) {
                //Version <=10: Set up history sessions with old
                // name first
                if (!scramblerSpec.equals("333") || name().equals("THREE")) {
                    mHistorySessions.setFilename(scramblerSpec + ".json");
                    mHistorySessions.init(context);
                    mHistorySessions.setFilename(historyFileName);
                    if (mHistorySessions.getList().size() > 0) {
                        mHistorySessions.save(context);
                    }
                    File oldFile = new File(context.getFilesDir(),
                            scramblerSpec + ".json");
                    oldFile.delete();
                }
            }

            if (savedVersionCode <= 13) {
                //Version <=13: ScrambleAndSvg json structure changes
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Session.class, new JsonDeserializer<Session>() {
                            @Override
                            public Session deserialize(JsonElement json, Type typeOfT,
                                                       JsonDeserializationContext context)
                                    throws JsonParseException {

                                Gson gson = new GsonBuilder()
                                        .registerTypeAdapter(ScrambleAndSvg.class, new JsonDeserializer<ScrambleAndSvg>() {
                                            @Override
                                            public ScrambleAndSvg deserialize(JsonElement json, Type typeOfT,
                                                                              JsonDeserializationContext context)
                                                    throws JsonParseException {
                                                return new ScrambleAndSvg(json.getAsJsonPrimitive().getAsString(), null);
                                            }
                                        }).create();

                                Session s = gson.fromJson(json, typeOfT);
                                for (final Solve solve : s.getSolves()) {
                                    solve.attachSession(s);
                                }
                                return s;
                            }
                        })
                        .create();
                Utils.updateData(context, historyFileName, gson);
                Utils.updateData(context, currentSessionFileName, gson);
            }

            ////////////////////////////

            mHistorySessions.init(context);
            Set<String> selected = PrefUtils.getSelectedPuzzleTypeNames(context);
            mEnabled = (selected.size() == 0 || selected.contains(name()));
            List<Session> currentSessions =
                    Utils.getSessionListFromFile(context, currentSessionFileName);
            if (currentSessions.size() > 0) {
                mCurrentSession = currentSessions.get(0);
            } else {
                mCurrentSession = new Session();
            }
        }
        mInitialized = true;
    }

    public void saveCurrentSession(Context context) {
        ArrayList<Session> session = new ArrayList<>();
        session.add(mCurrentSession);
        Utils.saveSessionListToFile(context, currentSessionFileName, session);
    }

    public void submitCurrentSession(Context context) {
        if (mCurrentSession != null &&
                mCurrentSession.getNumberOfSolves() > 0) {
            mHistorySessions.addSession(mCurrentSession, context);
            resetCurrentSession();
        }
    }

    public Puzzle getPuzzle() {
        if (mPuzzle == null) {
            try {
                mPuzzle = PuzzlePlugins.getScramblers().get(scramblerSpec)
                        .cachedInstance();
            } catch (LazyInstantiatorException |
                    BadLazyClassDescriptionException | IOException e) {
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
        return context.getResources().getStringArray(R.array.puzzles)
                [mStringIndex];
    }

    public Session getSession(int index) {
        if (index == CURRENT_SESSION) {
            //Check that if the current session is null (from reset or
            // initializing)
            return mCurrentSession;
        } else {
            return mHistorySessions.getList().get(index);
        }
    }

    public void resetCurrentSession() {
        mCurrentSession.reset();
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
                    notifyPuzzleTypeChanged();
                    break;
                }
            }
        }
    }

    public static class Observer {
        public void onPuzzleTypeChanged() {
        }
    }
}
