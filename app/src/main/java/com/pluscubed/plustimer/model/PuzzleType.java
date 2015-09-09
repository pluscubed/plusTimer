package com.pluscubed.plustimer.model;

import android.content.Context;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.Utils;

import net.gnehzr.tnoodle.scrambles.Puzzle;
import net.gnehzr.tnoodle.scrambles.PuzzlePlugins;
import net.gnehzr.tnoodle.utils.BadLazyClassDescriptionException;
import net.gnehzr.tnoodle.utils.LazyInstantiatorException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Enum for puzzle
 */
public class PuzzleType {
    @JsonIgnore
    private static String sCurrentId;
    @JsonIgnore
    private static List<PuzzleType> sPuzzleTypes;
    @JsonIgnore
    private final String mId;
    //Pre-SQL legacy code
    @JsonIgnore
    @Deprecated
    private final String mCurrentSessionFileName;
    @JsonIgnore
    @Deprecated
    private final String mHistoryFileName;
    private String scrambler;
    private String currentSessionId;
    private boolean enabled;
    private boolean inspection;
    private String name;
    private boolean bld;
    @JsonIgnore
    private Puzzle mPuzzle;
    @JsonIgnore
    @Deprecated
    private HistorySessions mHistorySessionsLegacy;
    @JsonIgnore
    @Deprecated
    private String mLegacyName;

    PuzzleType(String scrambler, String uiName, String legacyName, String id) {
        this.scrambler = scrambler;
        this.name = uiName;
        mId = id;

        mLegacyName = legacyName;
        mHistoryFileName = mLegacyName + ".json";
        mCurrentSessionFileName = mLegacyName + "-current.json";
        mHistorySessionsLegacy = new HistorySessions(mHistoryFileName);
    }

    public static List<PuzzleType> getPuzzleTypes() {
        return sPuzzleTypes;
    }

    public static PuzzleType get(String id) {
        for (PuzzleType type : sPuzzleTypes) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return sPuzzleTypes.get(0);
    }

    public static PuzzleType getCurrent() {
        return get(sCurrentId);
    }

    public static void setCurrent(String currentId) {
        //TODO
        sCurrentId = currentId;
    }

    public static String getCurrentId() {
        return sCurrentId;
    }

    public static List<PuzzleType> valuesExcludingDisabled() {
        List<PuzzleType> array = new ArrayList<>();
        for (PuzzleType i : sPuzzleTypes) {
            if (i.isEnabled()) {
                array.add(i);
            }
        }
        return array;
    }

    public synchronized static void initialize(Context context) {
        int savedVersionCode = PrefUtils.getVersionCode(context);
        Firebase puzzletypes = new Firebase("https://plustimer.firebaseio.com/web/data/users/test1/puzzletypes");

        if (savedVersionCode < 24) {

            //Generate default puzzle types from this...
            String[] scramblers = context.getResources().getStringArray(R.array.scramblers);
            //and this, with the appropriate UI names...
            String[] defaultCustomPuzzleTypes = context.getResources().getStringArray(R.array.default_custom_puzzletypes);
            //from this
            String[] puzzles = context.getResources().getStringArray(R.array.scrambler_names);
            //and the legacy names from this
            String[] legacyNames = context.getResources().getStringArray(R.array.legacy_names);


            for (int i = 0; i < scramblers.length + defaultCustomPuzzleTypes.length; i++) {
                String scrambler;
                String customType = null;
                if (scramblers.length > i) {
                    scrambler = scramblers[i];
                } else {
                    customType = defaultCustomPuzzleTypes[i - scramblers.length];
                    scrambler = customType.substring(0, customType.indexOf(","));
                }

                String uiName;
                if (puzzles.length > i) {
                    uiName = context.getResources().getStringArray(R.array.scrambler_names)[i];
                } else {
                    int order = Integer.parseInt(scrambler.substring(0, 1));
                    String addon = null;
                    if (scrambler.contains("ni")) {
                        addon = context.getString(R.string.bld);
                    }
                    if (customType != null) {
                        if (customType.contains("feet")) {
                            addon = context.getString(R.string.feet);
                        } else if (customType.contains("oh")) {
                            addon = context.getString(R.string.oh);
                        }
                    }
                    if (addon != null) {
                        uiName = order + "x" + order + "-" + addon;
                    } else {
                        uiName = order + "x" + order;
                    }
                }

                Firebase newTypeRef = puzzletypes.push();
                PuzzleType type = new PuzzleType(scrambler, uiName, legacyNames[i], newTypeRef.getKey());
                newTypeRef.setValue(type);
                sPuzzleTypes.add(type);
            }
        } else {
            puzzletypes.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    while (dataSnapshot.getChildren().iterator().hasNext()) {
                        PuzzleType type = dataSnapshot.getChildren().iterator().next().getValue(PuzzleType.class);
                        sPuzzleTypes.add(type);
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                }
            });
        }

        for (PuzzleType puzzleType : sPuzzleTypes) {
            puzzleType.upgradeDatabase(context);
        }

        PrefUtils.saveVersionCode(context);
    }

    public boolean isBld() {
        return bld;
    }

    public String getId() {
        return mId;
    }

    public boolean isScramblerOfficial() {
        return !scrambler.contains("fast");
    }

    public String getScrambler() {
        return scrambler;
    }

    @Deprecated
    public String getHistoryFileName() {
        return mHistoryFileName;
    }

    @Deprecated
    public String getCurrentSessionFileName() {
        return mCurrentSessionFileName;
    }

    //TODO
    /*@Deprecated
    public HistorySessions getHistorySessions() {
        return mHistorySessionsLegacy;
    }*/

    public void deleteSession(Session session) {
        //TODO
        //mDataSource.deleteSession(this, session.getId());
    }

    public List<Session> getSortedHistorySessions() {
        //TODO
        /*if (sessions.size() > 0) {
            sessions.remove(getSession(currentSessionId));
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
        return sessions;*/
        return null;
    }


    public String getCurrentSessionId() {
        return currentSessionId;
    }

    private void upgradeDatabase(Context context) {
        //TODO
        //AFTER UPDATING APP////////////
        int savedVersionCode = PrefUtils.getVersionCode(context);

        if (savedVersionCode <= 10) {
            //Version <=10: Set up history sessions with old
            // name first
            if (!scrambler.equals("333") || mLegacyName.equals("THREE")) {
                mHistorySessionsLegacy.setFilename(scrambler + ".json");
                mHistorySessionsLegacy.init(context);
                mHistorySessionsLegacy.setFilename(mHistoryFileName);
                if (mHistorySessionsLegacy.getList().size() > 0) {
                    mHistorySessionsLegacy.save(context);
                }
                File oldFile = new File(context.getFilesDir(),
                        scrambler + ".json");
                oldFile.delete();
            }
        }

        if (savedVersionCode <= 13) {
            //Version <=13: ScrambleAndSvg json structure changes
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Session.class,
                            (JsonDeserializer<Session>) (json, typeOfT, sessionJsonDeserializer) -> {

                                Gson gson1 = new GsonBuilder()
                                        .registerTypeAdapter(ScrambleAndSvg.class,
                                                (JsonDeserializer<ScrambleAndSvg>) (json1, typeOfT1, context1) ->
                                                        new ScrambleAndSvg(json1.getAsJsonPrimitive().getAsString(), null))
                                        .create();

                                Session s = gson1.fromJson(json, typeOfT);
                                for (final Solve solve : s.getSolves()) {
                                    //TODO: Legacy
                                    //solve.attachSession(s);
                                }
                                return s;
                            })
                    .create();
            Utils.updateData(context, mHistoryFileName, gson);
            Utils.updateData(context, mCurrentSessionFileName, gson);
        }

        mHistorySessionsLegacy.setFilename(null);
        ////////////////////////////
    }

    public void submitCurrentSession(Context context) {
        //TODO
        //Insert a copy of the current session in the second to last position. The "new" current session
        //will be at the last position.
        /*if (mAllSessions != null)
            mAllSessions.add(new Session(mCurrentSession));
        mCurrentSession.newSession();
        currentSessionId = mCurrentSession.getId();
        PrefUtils.saveCurrentSessionIndex(this, context, currentSessionId);*/
    }

    public Puzzle getPuzzle() {
        if (mPuzzle == null) {
            try {
                mPuzzle = PuzzlePlugins.getScramblers().get(scrambler)
                        .cachedInstance();
            } catch (LazyInstantiatorException |
                    BadLazyClassDescriptionException | IOException e) {
                e.printStackTrace();
            }
        }
        return mPuzzle;
    }

    public String getUiName() {
        return name;
    }

    public Session getCurrentSession() {
        //TODO
        return getSession(currentSessionId);
    }

    @Nullable
    public Session getSession(String id) {
        //TODO
        /*if (id == currentSessionId) {
            //Check that if the current session is null (from reset or
            // initializing)
            return mCurrentSession;
        } else if (mAllSessions != null) {
            for (Session session : mAllSessions) {
                if (session.getId() == id) {
                    return session;
                }
            }
        }*/
        return null;
    }

    public void resetCurrentSession() {
        //TODO
        /*mCurrentSession.newSession();*/
    }

    boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        //TODO
        this.enabled = enabled;
        if (mId.equals(sCurrentId) && !this.enabled) {
            for (PuzzleType i : sPuzzleTypes) {
                if (i.enabled) {
                    sCurrentId = i.getId();
                    break;
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PuzzleType && ((PuzzleType) o).getId().equals(mId);
    }
}
