package com.pluscubed.plustimer.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fernandocejas.frodo.annotation.RxLogObservable;
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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

/**
 * Puzzle Type object
 */
@JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class PuzzleType extends CbObject {
    public static final String TYPE_PUZZLETYPE = "puzzletype";
    public static final String VIEW_PUZZLETYPES = "puzzletypes";

    private static String sCurrentTypeId;
    private static List<PuzzleType> sPuzzleTypes;
    private static boolean sInitialized;

    private static Set<CurrentChangeListener> sCurrentChangeListeners;

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
    @JsonProperty("sessions")
    @NonNull
    private List<String> mSessions;

    //Pre-SQL legacy code
    @Deprecated
    private String mCurrentSessionFileName;
    @Deprecated
    private String mHistoryFileName;
    @Deprecated
    private HistorySessions mHistorySessionsLegacy;
    @Deprecated
    private String mLegacyName;

    public PuzzleType() {
        mSessions = new ArrayList<>();
    }

    @WorkerThread
    public PuzzleType(Context context, String scrambler, String name,
                      String currentSessionId, boolean inspectionOn) throws CouchbaseLiteException, IOException {
        super(context);

        mScrambler = scrambler;
        mName = name;
        mEnabled = true;
        mCurrentSessionId = currentSessionId;
        mInspectionOn = inspectionOn;
        mSessions = new ArrayList<>();

        updateCb(context);
    }

    public static void addCurrentChangeListener(CurrentChangeListener listener) {
        getListeners().add(listener);
    }

    public static void removeCurrentChangeListener(CurrentChangeListener listener) {
        getListeners().remove(listener);
    }

    private static Set<CurrentChangeListener> getListeners() {
        if (sCurrentChangeListeners == null) {
            sCurrentChangeListeners = new HashSet<>();
        }
        return sCurrentChangeListeners;
    }

    private static void notifyChangeCurrentListeners() {
        for (CurrentChangeListener listener : getListeners()) {
            listener.notifyChange();
        }
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
        return get(sCurrentTypeId);
    }

    public static String getCurrentId() {
        return sCurrentTypeId;
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

    public static boolean isInitialized() {
        return sInitialized;
    }

    public synchronized static Completable initialize(Context context) {
        if (sInitialized) {
            return Completable.complete();
        }

        try {
            if (sPuzzleTypes == null) {
                sPuzzleTypes = new ArrayList<>();

                int savedVersionCode = PrefUtils.getVersionCode(context);

                Database database = App.getDatabase(context);
                View puzzletypesView = database.getView(VIEW_PUZZLETYPES);
                puzzletypesView.setMap((document, emitter) -> {
                    if (document.get("type").equals(TYPE_PUZZLETYPE)) {
                        emitter.emit(document.get("name"), document.get("scrambler"));
                    }
                }, "1");

                Completable completable;
                if (savedVersionCode < 24) {
                    completable = initializeFirstRunAsync(context);
                } else {
                    completable = initializePuzzleTypes(database);

                    sCurrentTypeId = PrefUtils.getCurrentPuzzleType(context);
                }

                return completable.doOnComplete(() -> {
                    for (PuzzleType puzzleType : sPuzzleTypes) {
                        //TODO: upgrade database
                        //puzzleType.upgradeDatabase(context);
                    }

                    PrefUtils.saveVersionCode(context);

                    sInitialized = true;
                });
            } else {
                return Completable.complete();
            }
        } catch (CouchbaseLiteException | IOException e) {
            return Completable.error(e);
        }
    }

    @NonNull
    private static Completable initializePuzzleTypes(Database database) {
        return Completable.create(subscriber -> {
            Query puzzleTypesQuery = database.getView(VIEW_PUZZLETYPES).createQuery();
            puzzleTypesQuery.runAsync((rows, error) -> {
                for (QueryRow row : rows) {
                    PuzzleType type = PuzzleType.fromDoc(row.getDocument(), PuzzleType.class);

                    if (sPuzzleTypes != null) {
                        sPuzzleTypes.add(type);
                    }
                }

                subscriber.onCompleted();
            });
        }).subscribeOn(Schedulers.io());
    }

    private static Completable initializeFirstRunAsync(Context context) {
        return Completable.fromCallable(() -> {
            initializeFirstRun(context);
            return null;
        }).subscribeOn(Schedulers.io());
    }

    private static void initializeFirstRun(Context context) throws CouchbaseLiteException, IOException {
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

            if (scramblers.length > i) {
                scrambler = scramblers[i];
            } else {
                defaultCustomType = defaultCustomPuzzleTypes[i - scramblers.length];
                scrambler = defaultCustomType.substring(0, defaultCustomType.indexOf(","));
            }


            if (puzzles.length > i) {
                uiName = puzzles[i];
            } else {
                int order = Integer.parseInt(scrambler.substring(0, 1));
                String addon = null;
                if (scrambler.contains("ni")) {
                    addon = context.getString(R.string.bld);
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

            PuzzleType newPuzzleType = new PuzzleType(context, scrambler, uiName, null, true/*, legacyNames[i]*/);

            if (uiName.equals("3x3")) {
                //Default current puzzle type
                sCurrentTypeId = newPuzzleType.getId();
                newPuzzleType.newSession(context);

                PrefUtils.setCurrentPuzzleType(context, sCurrentTypeId);
            }

            sPuzzleTypes.add(newPuzzleType);
        }

        Collections.sort(sPuzzleTypes,
                (lhs, rhs) -> Collator.getInstance().compare(lhs.getName(), rhs.getName()));

    }

    public static void setCurrent(Context context, String puzzleType) throws IOException, CouchbaseLiteException {
        sCurrentTypeId = puzzleType;
        PrefUtils.setCurrentPuzzleType(context, sCurrentTypeId);

        if (get(sCurrentTypeId).getCurrentSessionId() == null) {
            get(sCurrentTypeId).newSession(context);
        }

        notifyChangeCurrentListeners();
    }

    private Session newSession(Context context) throws IOException, CouchbaseLiteException {
        Session session = new Session(context);

        mCurrentSessionId = session.getId();
        mSessions.add(session.getId());

        updateCb(context);

        return session;
    }

    public String getCurrentSessionId() {
        return mCurrentSessionId;
    }

    public boolean isBld() {
        return mScrambler.contains("ni");
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

    public void setEnabled(Context context, boolean enabled) throws CouchbaseLiteException, IOException {
        //TODO
        this.mEnabled = enabled;
        if (mId.equals(sCurrentTypeId) && !this.mEnabled) {
            for (PuzzleType i : sPuzzleTypes) {
                if (i.mEnabled) {
                    sCurrentTypeId = i.getId();
                    break;
                }
            }
        }

        updateCb(context);
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

    public void deleteSession(Context context, String sessionId) throws CouchbaseLiteException, IOException {
        getSession(context, sessionId)
                .getDocument(context)
                .delete();

        mSessions.remove(sessionId);

        updateCb(context);
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
        mHistoryFileName = mLegacyName + ".json";
        mCurrentSessionFileName = mLegacyName + "-current.json";
        mHistorySessionsLegacy = new HistorySessions(mHistoryFileName);

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
                                /*for (final Solve solve : s.getSolves()) {
                                    //TODO: Legacy
                                    //solve.attachSession(s);
                                }*/
                                return s;
                            })
                    .create();
            Utils.updateData(context, mHistoryFileName, gson);
            Utils.updateData(context, mCurrentSessionFileName, gson);
        }

        mHistorySessionsLegacy.setFilename(null);
        ////////////////////////////
    }

    public void submitCurrentSession(Context context) throws IOException, CouchbaseLiteException {
        newSession(context);

        notifyChangeCurrentListeners();
    }

    public Puzzle getPuzzle() {
        if (mPuzzle == null) {
            try {
                mPuzzle = PuzzlePlugins.getScramblers().get(mScrambler)
                        .cachedInstance();
            } catch (LazyInstantiatorException |
                    BadLazyClassDescriptionException |
                    IOException e) {
                e.printStackTrace();
            }
        }
        return mPuzzle;
    }

    @RxLogObservable
    public Single<Session> getCurrentSessionDeferred(Context context) {
        return getSessionDeferred(context, mCurrentSessionId);
    }

    public Session getCurrentSession(Context context) throws CouchbaseLiteException, IOException {
        return getSession(context, mCurrentSessionId);
    }

    @RxLogObservable
    public Single<Session> getSessionDeferred(Context context, String id) {
        return Single.defer(() -> Single.just(getSession(context, id)))
                .subscribeOn(Schedulers.io());
    }

    public Session getSession(Context context, String id) throws CouchbaseLiteException, IOException {
        return fromDocId(context, id, Session.class);
    }

    public Observable<Session> getSessions(Context context) {
        return Observable.from(new ArrayList<>(mSessions))
                .subscribeOn(Schedulers.io())
                .flatMap(id -> {
                    try {
                        return Observable.just(getSession(context, id));
                    } catch (CouchbaseLiteException | IOException e) {
                        return Observable.error(e);
                    }
                });
    }

    public void resetCurrentSession() {
        //TODO
        /*mCurrentSession.newSession();*/
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PuzzleType && ((PuzzleType) o).getId().equals(mId);
    }

    @Override
    protected String getType() {
        return TYPE_PUZZLETYPE;
    }

    public interface CurrentChangeListener {
        void notifyChange();
    }
}
