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

import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;

public class SolveListPresenter extends Presenter<SolveListView> {

    public static final String INIT_SESSION_ID = "history_session";
    public static final String INIT_PUZZLETYPE_ID = "history_puzzletype";
    private static final String INIT_CURRENT = "current";
    private final Session.SolvesListener mSessionSolvesListener;
    private final PuzzleType.CurrentChangeListener mPuzzleTypeCurrentChangeListener;
    private String mPuzzleTypeId;
    private String mSessionId;
    private boolean mIsCurrent;
    private boolean mViewInitialized;

    public SolveListPresenter(Bundle arguments) {
        mIsCurrent = arguments.getBoolean(INIT_CURRENT);
        mPuzzleTypeId = arguments.getString(INIT_PUZZLETYPE_ID);
        mSessionId = arguments.getString(INIT_SESSION_ID);

        mSessionSolvesListener = (update, solve) -> {
            updateView();
            updateAdapter(update, solve);
        };

        mPuzzleTypeCurrentChangeListener = () -> {
            if (!isViewAttached()) {
                return;
            }

            mPuzzleTypeId = PuzzleType.getCurrentId();
            mSessionId = PuzzleType.getCurrent().getCurrentSessionId();

            reloadSolveList();
            updateView();
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

        if (PuzzleType.isInitialized()) {
            if (mIsCurrent) {
                setPuzzleTypeInitialized(PuzzleType.getCurrentId(), PuzzleType.getCurrent().getCurrentSessionId());
            } else if (mPuzzleTypeId != null && mSessionId != null) {
                setPuzzleTypeInitialized(mPuzzleTypeId, mSessionId);
            }
        }

        view.getSolveListAdapter().updateSignAndMillisecondsMode();
    }

    @Override
    public void onViewDetached() {
        super.onViewDetached();

        mViewInitialized = false;
    }

    @SuppressWarnings("ConstantConditions")
    private void removeSessionSolvesListener() {
        if (!isViewAttached()) {
            return;
        }

        PuzzleType.get(mPuzzleTypeId).getCurrentSessionDeferred(getView().getContextCompat())
                .subscribe(session -> {
                    session.removeListener(mSessionSolvesListener);
                });
    }

    public void setPuzzleTypeInitialized(String puzzleTypeId, String sessionId) {
        mPuzzleTypeId = puzzleTypeId;
        mSessionId = sessionId;

        //noinspection ConstantConditions
        if (isViewAttached() && !getView().getSolveListAdapter().isInitialized()) {
            reloadSolveList();
        }

        if (!mViewInitialized && isViewAttached()) {
            //noinspection ConstantConditions
            getView().setInitialized();
            updateView();

            mViewInitialized = true;
        }
    }

    private void reloadSolveList() {
        PuzzleType.get(mPuzzleTypeId).getSessionDeferred(getView().getContextCompat(), mSessionId)
                .doOnSuccess(session -> {
                    session.addListener(mSessionSolvesListener);
                })
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
        removeSessionSolvesListener();

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
            try {
                PuzzleType.get(mPuzzleTypeId)
                        .getCurrentSession(getView().getContextCompat())
                        .reset(getView().getContextCompat());
            } catch (CouchbaseLiteException | IOException e) {
                e.printStackTrace();
            }

            //noinspection ConstantConditions
            getView().showSessionResetSnackbar();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onSubmitClicked() {
        if (isViewAttached()) {
            try {
                PuzzleType.get(mPuzzleTypeId).submitCurrentSession(getView().getContextCompat());
            } catch (IOException | CouchbaseLiteException e) {
                e.printStackTrace();
            }
            getView().showSessionSubmittedSnackbar();
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onToolbarAddSolvePressed() {
        if (isViewAttached()) {
            SolveDialogUtils.createSolveDialog(getView().getContextCompat(), true, mPuzzleTypeId, mSessionId, null);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void updateView() {
        if (!isViewAttached()) {
            return;
        }

        try {
            Session session = PuzzleType.get(mPuzzleTypeId).getSession(getView().getContextCompat(), mSessionId);
            int numberOfSolves = session.getNumberOfSolves();

            if (numberOfSolves <= 0) {
                if (mIsCurrent) {
                    getView().enableResetSubmitButtons(false);
                } else {
                    PuzzleType.get(mPuzzleTypeId).deleteSession(getView().getContextCompat(), mSessionId);
                    if (isViewAttached())
                        getView().getContextCompat().finish();
                    return;
                }
                getView().showList(false);
            } else {
                if (mIsCurrent) {
                    getView().enableResetSubmitButtons(true);
                }
                getView().showList(true);
            }
        } catch (CouchbaseLiteException | IOException e) {
            e.printStackTrace();
        }
    }

    public void share() {
        if (!isViewAttached()) {
            return;
        }

        //noinspection ConstantConditions
        Context context = getView().getContextCompat();
        PuzzleType.get(mPuzzleTypeId).getSessionDeferred(getView().getContextCompat(), mSessionId)
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

        PuzzleType.get(mPuzzleTypeId).getSessionDeferred(context, mSessionId)
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
