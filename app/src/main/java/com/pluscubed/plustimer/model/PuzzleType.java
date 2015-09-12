package com.pluscubed.plustimer.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.pluscubed.plustimer.App;
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

import rx.Observable;

/**
 * Puzzle Type object
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class PuzzleType {
    @Nullable
    private static String sCurrentId;
    @Nullable
    private static List<PuzzleType> sPuzzleTypes;

    @JsonIgnore
    private String mId;
    @JsonIgnore
    private Puzzle mPuzzle;

    @JsonProperty("scrambler")
    private String mScrambler;
    @JsonProperty("currentSessionId")
    private String mCurrentSessionId;
    @JsonProperty("enabled")
    private boolean mEnabled;
    @JsonProperty("inspection")
    private boolean mInspectionOn;
    @JsonProperty("name")
    private String mName;
    @JsonProperty("bld")
    private boolean mIsBld;

    //Pre-SQL legacy code
    @JsonIgnore
    @Deprecated
    private String mCurrentSessionFileName;
    @JsonIgnore
    @Deprecated
    private String mHistoryFileName;
    @JsonIgnore
    @Deprecated
    private HistorySessions mHistorySessionsLegacy;
    @JsonIgnore
    @Deprecated
    private String mLegacyName;

    public PuzzleType() {
        mId = null;
    }

    public PuzzleType(String id, String scrambler, String name, String currentSessionId,
                      boolean inspectionOn, boolean isBld, String legacyName) {
        mScrambler = scrambler;
        mName = name;
        mId = id;
        mEnabled = true;
        mCurrentSessionId = currentSessionId;
        mInspectionOn = inspectionOn;
        mIsBld = isBld;

        mLegacyName = legacyName;
        mHistoryFileName = mLegacyName + ".json";
        mCurrentSessionFileName = mLegacyName + "-current.json";
        mHistorySessionsLegacy = new HistorySessions(mHistoryFileName);
    }

    public static List<PuzzleType> getPuzzleTypes() {
        return sPuzzleTypes;
    }

    @Nullable
    public static PuzzleType get(String id) {
        for (PuzzleType type : sPuzzleTypes) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return sPuzzleTypes.get(0);
    }

    @Nullable
    public static PuzzleType getCurrent() {
        return get(sCurrentId);
    }

    public static void setCurrent(String currentId) {
        //TODO
        sCurrentId = currentId;
    }

    @Nullable
    public static String getCurrentId() {
        return sCurrentId;
    }

    public static List<PuzzleType> getEnabledPuzzleTypes() {
        List<PuzzleType> array = new ArrayList<>();
        for (PuzzleType i : sPuzzleTypes) {
            if (i.isEnabled()) {
                array.add(i);
            }
        }
        return array;
    }

    public synchronized static Observable<Object> initialize(Context context) {
        sPuzzleTypes = new ArrayList<>();
        int savedVersionCode = PrefUtils.getVersionCode(context);
        //TODO: Nicer Rx structure
        return App.getFirebaseUserRef().flatMap(userRef -> {
            Firebase puzzletypesRef = userRef.child("puzzletypes");
            Firebase currentPuzzleTypeRef = userRef.child("current-puzzle-type");

            Observable<?> stuff;
            if (savedVersionCode < 24) {
                stuff = initializePuzzleTypesFirstRun(context, puzzletypesRef, currentPuzzleTypeRef);
            } else {
                stuff = initializePuzzleTypes(puzzletypesRef, currentPuzzleTypeRef);
            }

            return stuff;

        }).flatMap(o -> Observable.create(subscriber -> {
            for (PuzzleType puzzleType : sPuzzleTypes) {
                puzzleType.upgradeDatabase(context);
            }

            PrefUtils.saveVersionCode(context);
            subscriber.onCompleted();
        }));
    }

    @NonNull
    private static Observable<?> initializePuzzleTypes(Firebase puzzletypes, Firebase currentPuzzleType) {
        return Observable.combineLatest(Observable.create(
                subscriber -> puzzletypes.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        while (dataSnapshot.getChildren().iterator().hasNext()) {
                            DataSnapshot puzzleTypeSnapshot = dataSnapshot.getChildren().iterator().next();
                            PuzzleType type = puzzleTypeSnapshot.getValue(PuzzleType.class);
                            type.mId = puzzleTypeSnapshot.getKey();

                            sPuzzleTypes.add(type);
                        }
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        subscriber.onError(firebaseError.toException());
                    }
                })), Observable.create(subscriber -> {
            currentPuzzleType.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    sCurrentId = dataSnapshot.getValue(String.class);
                    subscriber.onCompleted();
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    subscriber.onError(firebaseError.toException());
                }
            });
            subscriber.onCompleted();
        }), (o, o2) -> null);
    }

    @NonNull
    private static Observable<?> initializePuzzleTypesFirstRun(Context context, Firebase puzzletypes, Firebase currentPuzzleType) {
        return Observable.create(subscriber -> {
            //Generate default puzzle types from this...
            String[] scramblers = context.getResources().getStringArray(R.array.scramblers);
            //and this, with the appropriate UI names...
            String[] defaultCustomPuzzleTypes = context.getResources()
                    .getStringArray(R.array.default_custom_puzzletypes);
            //from this
            String[] puzzles = context.getResources().getStringArray(R.array.scrambler_names);
            //and the legacy names from this
            String[] legacyNames = context.getResources().getStringArray(R.array.legacy_names);


            for (int i = 0; i < scramblers.length + defaultCustomPuzzleTypes.length; i++) {
                String scrambler;
                String defaultCustomType = null;
                String uiName;
                boolean bld = false;

                if (scramblers.length > i) {
                    scrambler = scramblers[i];
                } else {
                    defaultCustomType = defaultCustomPuzzleTypes[i - scramblers.length];
                    scrambler = defaultCustomType.substring(0, defaultCustomType.indexOf(","));
                }


                if (puzzles.length > i) {
                    uiName = context.getResources().getStringArray(R.array.scrambler_names)[i];
                } else {
                    int order = Integer.parseInt(scrambler.substring(0, 1));
                    String addon = null;
                    if (scrambler.contains("ni")) {
                        addon = context.getString(R.string.bld);
                        bld = true;
                    }
                    if (defaultCustomType != null) {
                        if (defaultCustomType.contains("feet")) {
                            addon = context.getString(R.string.feet);
                        } else if (defaultCustomType.contains("oh")) {
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
                PuzzleType type = new PuzzleType(newTypeRef.getKey(), scrambler, uiName,
                        null, true, bld, legacyNames[i]);

                if (uiName.equals("3x3")) {
                    //Default current puzzle type
                    currentPuzzleType.setValue(type.getId());
                    sCurrentId = type.getId();
                    type.addNewSession(puzzletypes.getParent());
                }

                newTypeRef.setValue(type);
                sPuzzleTypes.add(type);
            }
            subscriber.onCompleted();
        });
    }

    public void addNewSession(Firebase userRef) {
        Firebase sessions = userRef.child("sessions");

        Firebase newSessionRef = sessions.push();

        Session session = new Session(getId(), newSessionRef.getKey());
        newSessionRef.setValue(this);

        Firebase puzzleTypeSession = userRef.child(
                "puzzletype-sessions/" + getId() + session.getId());
        puzzleTypeSession.setValue(true);

        mCurrentSessionId = session.getId();
    }

    public String getCurrentSessionId() {
        return mCurrentSessionId;
    }

    public boolean isBld() {
        return mIsBld;
    }

    public boolean isScramblerOfficial() {
        return !mScrambler.contains("fast");
    }

    public String getScrambler() {
        return mScrambler;
    }

    public String getName() {
        return mName;
    }

    boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        //TODO
        this.mEnabled = enabled;
        if (mId.equals(sCurrentId) && !this.mEnabled) {
            for (PuzzleType i : sPuzzleTypes) {
                if (i.mEnabled) {
                    sCurrentId = i.getId();
                    break;
                }
            }
        }
    }

    public String getId() {
        return mId;
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
        return sessions;*/
        return null;
    }

    private void upgradeDatabase(Context context) {
        //TODO
        //AFTER UPDATING APP////////////
        int savedVersionCode = PrefUtils.getVersionCode(context);

        if (savedVersionCode <= 10) {
            //Version <=10: Set up history sessions with old
            // name first
            if (!mScrambler.equals("333") || mLegacyName.equals("THREE")) {
                mHistorySessionsLegacy.setFilename(mScrambler + ".json");
                mHistorySessionsLegacy.init(context);
                mHistorySessionsLegacy.setFilename(mHistoryFileName);
                if (mHistorySessionsLegacy.getList().size() > 0) {
                    mHistorySessionsLegacy.save(context);
                }
                File oldFile = new File(context.getFilesDir(),
                        mScrambler + ".json");
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
        mCurrentSessionId = mCurrentSession.getId();
        PrefUtils.saveCurrentSessionIndex(this, context, mCurrentSessionId);*/
    }

    public Puzzle getPuzzle() {
        if (mPuzzle == null) {
            try {
                mPuzzle = PuzzlePlugins.getScramblers().get(mScrambler)
                        .cachedInstance();
            } catch (LazyInstantiatorException |
                    BadLazyClassDescriptionException | IOException e) {
                e.printStackTrace();
            }
        }
        return mPuzzle;
    }

    public Session getCurrentSession() {
        //TODO
        return getSession(mCurrentSessionId);
    }

    @Nullable
    public Session getSession(String id) {
        //TODO
        /*if (id == mCurrentSessionId) {
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

    @Override
    public boolean equals(Object o) {
        return o instanceof PuzzleType && ((PuzzleType) o).getId().equals(mId);
    }
}
