package com.pluscubed.plustimer.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
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

    @Nullable
    private static Completable sInitialization;
    @Nullable
    private static List<PuzzleType> sPuzzleTypes;

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
        notifyChangeCurrentListenersDeferred().subscribe();
    }

    @NonNull
    private static Completable notifyChangeCurrentListenersDeferred() {
        return Completable.fromCallable(() -> {
            for (CurrentChangeListener listener : getListeners()) {
                listener.notifyChange();
            }
            return null;
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    public static Observable<PuzzleType> getPuzzleTypes(Context context) {
        return initialize(context)
                .andThen(Observable.defer(() -> Observable.from(sPuzzleTypes)));
    }

    public static Single<PuzzleType> get(Context context, String id) {
        return initialize(context)
                .andThen(getInternal(id).toObservable())
                .toSingle();
    }

    @NonNull
    private static Single<PuzzleType> getInternal(String id) {
        return Single.fromCallable(() -> {
            for (PuzzleType type : sPuzzleTypes) {
                if (type.getId().equals(id)) {
                    return type;
                }
            }
            return sPuzzleTypes.get(0);
        });
    }

    public static Single<PuzzleType> getCurrent(Context context) {
        return initialize(context)
                .andThen(getInternal(getCurrentId(context)).toObservable())
                .toSingle();
    }

    public static String getCurrentId(Context context) {
        return PrefUtils.getCurrentPuzzleType(context);
    }

    public static Observable<PuzzleType> getEnabledPuzzleTypes(Context context) {
        return getPuzzleTypes(context)
                .flatMap(puzzleType -> {
                    if (puzzleType.isEnabled()) {
                        return Observable.just(puzzleType);
                    } else {
                        return Observable.empty();
                    }
                });
    }

    public static boolean isInitialized() {
        return sPuzzleTypes != null;
    }

    public static Completable initialize(Context context) {
        if (isInitialized()) {
            return Completable.complete();
        }

        if (sInitialization == null) {
            try {
                int savedVersionCode = PrefUtils.getVersionCode(context);

                Database database = CouchbaseInstance.get(context).getDatabase();
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
                }

                sInitialization = completable.doOnCompleted(() -> {
                    for (PuzzleType puzzleType : sPuzzleTypes) {
                        //TODO: upgrade database
                        //puzzleType.upgradeDatabase(context);
                    }
                    PrefUtils.saveVersionCode(context);

                    sInitialization = null;
                }).toObservable()
                        .publish().autoConnect()
                        .toCompletable();

            } catch (CouchbaseLiteException | IOException e) {
                return Completable.error(e);
            }
        }

        return sInitialization;
    }

    @NonNull
    private static Completable initializePuzzleTypes(Database database) {
        return Completable.create(completableSubscriber -> {
            Query puzzleTypesQuery = database.getView(VIEW_PUZZLETYPES).createQuery();
            puzzleTypesQuery.runAsync((rows, error) -> {
                List<PuzzleType> puzzleTypes = new ArrayList<>();
                for (QueryRow row : rows) {
                    PuzzleType type = PuzzleType.fromDoc(row.getDocument(), PuzzleType.class);
                    puzzleTypes.add(type);
                }

                sPuzzleTypes = puzzleTypes;

                completableSubscriber.onCompleted();
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

        List<PuzzleType> puzzleTypes = new ArrayList<>();

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
                newPuzzleType.newSession(context);

                PrefUtils.setCurrentPuzzleType(context, newPuzzleType.getId());
            }

            puzzleTypes.add(newPuzzleType);
        }

        Collections.sort(puzzleTypes,
                (lhs, rhs) -> Collator.getInstance().compare(lhs.getName(), rhs.getName()));

        sPuzzleTypes = puzzleTypes;
    }

    public static Completable setCurrent(Context context, String puzzleTypeId) {
        if (!puzzleTypeId.equals(getCurrentId(context))) {

            PrefUtils.setCurrentPuzzleType(context, puzzleTypeId);

            return get(context, puzzleTypeId)
                    .doOnSuccess(puzzleType -> {
                        if (puzzleType.getCurrentSessionId() == null) {
                            try {
                                puzzleType.newSession(context);
                            } catch (IOException | CouchbaseLiteException e) {
                                throw Exceptions.propagate(e);
                            }
                        }
                    })
                    .flatMapObservable(puzzleType ->
                            notifyChangeCurrentListenersDeferred().toObservable())
                    .toCompletable();
        } else {
            return Completable.complete();
        }
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

    public Completable setEnabled(Context context, boolean enabled) {
        return initialize(context)
                .concatWith(Completable.defer(() -> {
                    mEnabled = enabled;
                    if (mId.equals(getCurrentId(context)) && !mEnabled) {
                        for (PuzzleType puzzleType : sPuzzleTypes) {
                            if (puzzleType.mEnabled) {
                                return setCurrent(context, puzzleType.getId())
                                        .doOnCompleted(() -> updateCb(context));
                            }
                        }
                    }

                    return Completable.complete();
                }));

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
    public HistorySessions getHistorySessionsSorted() {
        return mHistorySessionsLegacy;
    }*/

    public void deleteSession(Context context, String sessionId) throws CouchbaseLiteException, IOException {
        getSession(context, sessionId)
                .getDocument(context)
                .delete();

        mSessions.remove(sessionId);

        updateCb(context);
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

    public Single<Session> getCurrentSessionDeferred(Context context) {
        return getSessionDeferred(context, mCurrentSessionId);
    }

    public Session getCurrentSession(Context context) throws CouchbaseLiteException, IOException {
        return getSession(context, mCurrentSessionId);
    }

    public Single<Session> getSessionDeferred(Context context, String id) {
        return Single.defer(() -> Single.just(getSession(context, id)))
                .subscribeOn(Schedulers.io());
    }

    public Session getSession(Context context, String id) throws CouchbaseLiteException, IOException {
        return fromDocId(context, id, Session.class);
    }

    public Observable<Session> getHistorySessionsSorted(Context context) {
        return getHistorySessions(context)
                .toSortedList((session, session2) -> {
                    return Utils.compare(session.getLastSolve(context).toBlocking().first().getTimestamp(),
                            session2.getLastSolve(context).toBlocking().first().getTimestamp());
                }).flatMap(Observable::from);
    }

    public Observable<Session> getHistorySessions(Context context) {
        return Observable.from(new ArrayList<>(mSessions))
                .filter(id -> !id.equals(mCurrentSessionId))
                .subscribeOn(Schedulers.io())
                .flatMap(id -> {
                    try {
                        return Observable.just(getSession(context, id));
                    } catch (CouchbaseLiteException | IOException e) {
                        return Observable.error(e);
                    }
                });
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
