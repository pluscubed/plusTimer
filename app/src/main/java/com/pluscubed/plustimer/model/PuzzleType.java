package com.pluscubed.plustimer.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.sql.SolveDataSource;
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
import java.util.Collections;
import java.util.Comparator;
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

    private static final int NOT_SPECIAL_STRING_INDEX = -1;

    private static PuzzleType sCurrentPuzzleType;
    private static List<Observer> mObservers;

    private static SolveDataSource mDataSource;

    static {
        mObservers = new ArrayList<>();
    }

    public final String scramblerSpec;
    public final boolean official;
    private final int mStringIndex;
    //Pre-SQL legacy code
    @Deprecated
    private final String currentSessionFileName;
    @Deprecated
    private final String historyFileName;
    private int mCurrentSessionId;
    @Deprecated
    private HistorySessions mHistorySessionsLegacy;


    private Session mCurrentSession;
    @Nullable
    private Set<Session> mAllSessions;

    private boolean mEnabled;
    private Puzzle mPuzzle;

    PuzzleType(String scramblerSpec, int stringIndex) {
        this.scramblerSpec = scramblerSpec;
        mStringIndex = stringIndex;
        official = !this.scramblerSpec.contains("fast");
        historyFileName = name() + ".json";
        currentSessionFileName = name() + "-current.json";
        mHistorySessionsLegacy = new HistorySessions(historyFileName);
    }

    PuzzleType(String scramblerSpec) {
        this(scramblerSpec, NOT_SPECIAL_STRING_INDEX);
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
            sCurrentPuzzleType.initializeCurrentSessionData();
            notifyPuzzleTypeChanged();
        }
    }

    public static List<PuzzleType> valuesExcludingDisabled() {
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
        mDataSource = new SolveDataSource(context);

        for (PuzzleType puzzleType : values()) {
            puzzleType.init(context);
            puzzleType.upgradeDatabase(context);
        }
        PrefUtils.saveVersionCode(context);
    }

    @NonNull
    public static SolveDataSource getDataSource() {
        return mDataSource;
    }

    @Deprecated
    public String getHistoryFileName() {
        return historyFileName;
    }

    @Deprecated
    public String getCurrentSessionFileName() {
        return currentSessionFileName;
    }

    /*@Deprecated
    public HistorySessions getHistorySessions() {
        return mHistorySessionsLegacy;
    }*/

    public void deleteSession(Session session) {
        mDataSource.deleteSession(this, session.getId());
    }

    public List<Session> getSortedHistorySessions() {
        List<Session> sessions = new ArrayList<>(mAllSessions);
        if (sessions.size() > 0) {
            sessions.remove(getSession(mCurrentSessionId));
            Collections.sort(sessions, new Comparator<Session>() {
                @Override
                public int compare(Session lhs, Session rhs) {
                    if (lhs.getTimestamp() > rhs.getTimestamp()) {
                        return 1;
                    } else if (lhs.getTimestamp() < rhs.getTimestamp()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });
        }
        return sessions;
    }


    public int getCurrentSessionId() {
        return mCurrentSessionId;
    }

    private synchronized void init(Context context) {
        Set<String> selected = PrefUtils.getSelectedPuzzleTypeNames(context);
        mEnabled = (selected.size() == 0 || selected.contains(name()));
        mCurrentSessionId = PrefUtils.getCurrentSessionIndex(this, context);
    }

    public synchronized void initializeAllSessionData() {
        mAllSessions = mDataSource.loadHistorySessionsForPuzzleType(this);
    }

    public synchronized void initializeCurrentSessionData() {
        mCurrentSession = mDataSource.loadSessionForPuzzleType(this, mCurrentSessionId);
    }

    private void upgradeDatabase(Context context) {
        //AFTER UPDATING APP////////////
        int savedVersionCode = PrefUtils.getVersionCode(context);

        if (savedVersionCode <= 10) {
            //Version <=10: Set up history sessions with old
            // name first
            if (!scramblerSpec.equals("333") || name().equals("THREE")) {
                mHistorySessionsLegacy.setFilename(scramblerSpec + ".json");
                mHistorySessionsLegacy.init(context);
                mHistorySessionsLegacy.setFilename(historyFileName);
                if (mHistorySessionsLegacy.getList().size() > 0) {
                    mHistorySessionsLegacy.save(context);
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
                                    .registerTypeAdapter(ScrambleAndSvg.class, new
                                            JsonDeserializer<ScrambleAndSvg>() {
                                                @Override
                                                public ScrambleAndSvg deserialize(JsonElement json,
                                                                                  Type typeOfT,
                                                                                  JsonDeserializationContext context)
                                                        throws JsonParseException {
                                                    return new ScrambleAndSvg(json.getAsJsonPrimitive
                                                            ().getAsString(), null);
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

        mHistorySessionsLegacy.setFilename(null);
        ////////////////////////////
    }

    public void submitCurrentSession(Context context) {
        //Insert a copy of the current session in the second to last position. The "new" current session
        //will be at the last position.
        if (mAllSessions != null)
            mAllSessions.add(new Session(mCurrentSession));
        mCurrentSession.newSession();
        mCurrentSessionId = mCurrentSession.getId();
        PrefUtils.saveCurrentSessionIndex(this, context, mCurrentSessionId);
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
        if (mStringIndex == NOT_SPECIAL_STRING_INDEX) {
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

    public Session getCurrentSession() {
        return getSession(mCurrentSessionId);
    }

    @Nullable
    public Session getSession(int id) {
        if (id == mCurrentSessionId) {
            //Check that if the current session is null (from reset or
            // initializing)
            return mCurrentSession;
        } else if (mAllSessions != null) {
            for (Session session : mAllSessions) {
                if (session.getId() == id) {
                    return session;
                }
            }
        }
        return null;
    }

    public void resetCurrentSession() {
        mCurrentSession.newSession();
    }

    boolean isEnabled() {
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
