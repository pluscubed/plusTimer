package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;

import com.couchbase.lite.CouchbaseLiteException;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.Presenter;
import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.base.RecyclerViewUpdate;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.SolveDialogUtils;

import java.io.IOException;
import java.util.Collections;

import rx.Observable;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;

public class SolveListPresenter extends Presenter<SolveListView> {

    public static final String INIT_SESSION_ID = "history_session";
    public static final String INIT_PUZZLETYPE_ID = "history_puzzletype";
    private static final String INIT_CURRENT = "current";

    private final Session.SolvesListener mSessionSolvesListener;
    private final PuzzleType.CurrentSessionChangeListener mPuzzleTypeCurrentChangeListener;

    private String mPuzzleTypeId;
    private String mSessionId;

    private boolean mIsCurrent;

    private boolean mInitialized;
    private boolean mViewInitialized;

    public SolveListPresenter(Bundle arguments) {
        mIsCurrent = arguments.getBoolean(INIT_CURRENT);
        mPuzzleTypeId = arguments.getString(INIT_PUZZLETYPE_ID);
        mSessionId = arguments.getString(INIT_SESSION_ID);

        mSessionSolvesListener = (update, solve) -> {
            updateView();
            updateAdapter(update, solve);
        };

        mPuzzleTypeCurrentChangeListener = oldType -> {
            if (!isViewAttached()) {
                return;
            }

            removeSessionSolvesListener(oldType);

            //noinspection ConstantConditions
            PuzzleType.getCurrent(getView().getContextCompat())
                    .map(PuzzleType::getCurrentSessionId)
                    .subscribe(currentSession -> {
                        mPuzzleTypeId = PuzzleType.getCurrentId(getView().getContextCompat());
                        mSessionId = currentSession;

                        reloadSolveList();
                        updateView();

                        attachSessionSolvesListener(getView());
                    });
        };

        if (mIsCurrent)
            PuzzleType.addCurrentChangeListener(mPuzzleTypeCurrentChangeListener);
    }

    public static SolveListFragment newInstance(boolean current) {
        return newInstance(current, "", "");
    }

    public static SolveListFragment newInstance(boolean current, String puzzleTypeId, String sessionId) {
        Bundle args = new Bundle();
        args.putBoolean(INIT_CURRENT, current);
        args.putString(INIT_PUZZLETYPE_ID, puzzleTypeId);
        args.putString(INIT_SESSION_ID, sessionId);
        SolveListFragment fragment = new SolveListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewAttached(SolveListView view) {
        super.onViewAttached(view);

        if (mIsCurrent) {
            PuzzleType.getCurrent(view.getContextCompat())
                    .map(PuzzleType::getCurrentSessionId)
                    .subscribe(currentSession -> {
                        initializeView(PuzzleType.getCurrentId(view.getContextCompat()), currentSession);
                    });

        } else if (mPuzzleTypeId != null && mSessionId != null) {
            initializeView(mPuzzleTypeId, mSessionId);
        } else {
            throw new RuntimeException("Must be current or have specified puzzletype and session");
        }

        view.getSolveListAdapter().updateSignAndMillisecondsMode();

        if (!mInitialized) {
            attachSessionSolvesListener(getView());

            mInitialized = true;
        }
    }

    @Override
    public void onViewDetached() {
        super.onViewDetached();

        mViewInitialized = false;
    }

    @SuppressWarnings("ConstantConditions")
    private void attachSessionSolvesListener(SolveListView view) {
        if (!isViewAttached()) {
            return;
        }

        PuzzleType.getCurrent(view.getContextCompat())
                .flatMap(puzzleType -> puzzleType.getCurrentSessionDeferred(getView().getContextCompat()))
                .subscribe(session -> {
                    session.addListener(mSessionSolvesListener);
                });
    }

    @SuppressWarnings("ConstantConditions")
    private void removeSessionSolvesListener(String puzzleTypeId) {
        if (!isViewAttached()) {
            return;
        }

        String id = puzzleTypeId == null ? PuzzleType.getCurrentId(getView().getContextCompat()) : puzzleTypeId;
        PuzzleType.get(getView().getContextCompat(), id)
                .flatMap(puzzleType -> puzzleType.getCurrentSessionDeferred(getView().getContextCompat()))
                .subscribe(session -> {
                    session.removeListener(mSessionSolvesListener);
                });
    }

    public void initializeView(String puzzleTypeId, String sessionId) {
        mPuzzleTypeId = puzzleTypeId;
        mSessionId = sessionId;

        if (!isViewAttached()) {
            return;
        }

        //noinspection ConstantConditions
        if (!getView().getSolveListAdapter().isInitialized()) {
            reloadSolveList();
        }

        if (!mViewInitialized) {
            //noinspection ConstantConditions
            getView().setInitialized();
            updateView();

            mViewInitialized = true;
        }
    }

    private void reloadSolveList() {
        PuzzleType.get(getView().getContextCompat(), mPuzzleTypeId)
                .flatMap(puzzleType -> puzzleType.getSessionDeferred(getView().getContextCompat(), mSessionId))
                .flatMapObservable(session ->
                        session.getSortedSolves(getView().getContextCompat())).toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(solves -> {
                    Collections.reverse(solves);
                    getView().getSolveListAdapter().setSolves(mPuzzleTypeId, solves);
                    updateAdapter(RecyclerViewUpdate.DATA_RESET, null);
                });
    }

    @Override
    public void onDestroyed() {
        removeSessionSolvesListener(null);

        if (mIsCurrent)
            PuzzleType.removeCurrentChangeListener(mPuzzleTypeCurrentChangeListener);
    }

    public void onResetButtonClicked() {
        if (isViewAttached()) {
            //noinspection ConstantConditions
            getView().showResetWarningDialog();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onResetDialogConfirmed() {
        if (isViewAttached()) {
            PuzzleType.get(getView().getContextCompat(), mPuzzleTypeId)
                    .flatMap(puzzleType -> puzzleType.getCurrentSessionDeferred(getView().getContextCompat()))
                    .subscribe(new SingleSubscriber<Session>() {
                        @Override
                        public void onSuccess(Session session) {
                            session.reset(getView().getContextCompat());
                        }

                        @Override
                        public void onError(Throwable error) {

                        }
                    });

            //noinspection ConstantConditions
            getView().showSessionResetSnackbar();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onSubmitClicked() {
        if (isViewAttached()) {
            PuzzleType.get(getView().getContextCompat(), mPuzzleTypeId)
                    .flatMapObservable(puzzleType -> Observable.fromCallable(() -> {
                        puzzleType.submitCurrentSession(getView().getContextCompat());
                        return null;
                    }))
                    .subscribe();
            getView().showSessionSubmittedSnackbar();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onAddSolvePressed() {
        if (isViewAttached()) {
            SolveDialogUtils.createSolveDialog(getView().getContextCompat(), true, mPuzzleTypeId, mSessionId, null);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void updateView() {
        if (!isViewAttached()) {
            return;
        }

        PuzzleType.get(getView().getContextCompat(), mPuzzleTypeId)
                .flatMap(puzzleType -> puzzleType.getSessionDeferred(getView().getContextCompat(), mSessionId))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(session -> {
                    int numberOfSolves = session.getNumberOfSolves();

                    if (numberOfSolves <= 0) {
                        if (mIsCurrent) {
                            getView().enableResetSubmitButtons(false);
                        } else {
                            deleteSession();
                            return;
                        }
                        getView().showList(false);
                    } else {
                        if (mIsCurrent) {
                            getView().enableResetSubmitButtons(true);
                        }
                        getView().showList(true);
                    }
                });
    }

    private void deleteSession() {
        PuzzleType.get(getView().getContextCompat(), mPuzzleTypeId)
                .subscribe(puzzleType -> {
                    try {
                        puzzleType.deleteSession(getView().getContextCompat(), mSessionId);
                    } catch (CouchbaseLiteException | IOException e) {
                        e.printStackTrace();
                    }
                });
        if (isViewAttached())
            getView().getContextCompat().finish();
    }

    public void onSharePressed() {
        if (!isViewAttached()) {
            return;
        }

        //noinspection ConstantConditions
        Context context = getView().getContextCompat();
        PuzzleType.get(context, mPuzzleTypeId)
                .flatMap(puzzleType -> puzzleType.getSessionDeferred(context, mSessionId))
                .flatMap(session ->
                        session.getStatsDeferred(context,
                                mPuzzleTypeId,
                                mIsCurrent,
                                true,
                                PrefUtils.isDisplayMillisecondsEnabled(context),
                                PrefUtils.isSignEnabled(context)
                        ))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<String>() {
                    @Override
                    public void onSuccess(String statsString) {
                        Intent intent = ShareCompat.IntentBuilder.from(getView().getContextCompat())
                                .setType("text/plain")
                                .setText(statsString)
                                .setChooserTitle(R.string.share_dialog_title)
                                .createChooserIntent();
                        context.startActivity(intent);
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });

    }

    public void onSolveClicked(Solve solve) {
        if (!isViewAttached()) {
            return;
        }

        //noinspection ConstantConditions
        SolveDialogUtils.createSolveDialog(
                getView().getContextCompat(),
                false,
                mPuzzleTypeId,
                mSessionId,
                solve
        );
    }

    @SuppressWarnings("ConstantConditions")
    private void updateAdapter(RecyclerViewUpdate change, Solve solve) {
        if (!isViewAttached()) {
            return;
        }

        Activity context = getView().getContextCompat();

        PuzzleType.get(context, mPuzzleTypeId)
                .flatMap(puzzleType -> puzzleType.getSessionDeferred(context, mSessionId))
                .flatMap(session -> session.getStatsDeferred(context,
                        mPuzzleTypeId,
                        mIsCurrent,
                        false,
                        PrefUtils.isDisplayMillisecondsEnabled(context),
                        PrefUtils.isSignEnabled(context)
                ))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<String>() {
                    @Override
                    public void onSuccess(String stats) {
                        getView().getSolveListAdapter().notifyChange(change, solve, stats);
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });
    }

    public void onDeletePressed() {
        deleteSession();
    }

    public static class Factory implements PresenterFactory<SolveListPresenter> {

        private Bundle arguments;

        public Factory(Bundle bundle) {
            arguments = bundle;
        }

        @Override
        public SolveListPresenter create() {
            return new SolveListPresenter(arguments);
        }
    }
}
