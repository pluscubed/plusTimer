package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

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

    private static final String INIT_CURRENT = "current";
    private final Session.SolvesListener mSessionSolvesListener;
    private String mPuzzleTypeId;
    private String mSessionId;
    private boolean mIsCurrent;
    private boolean mViewInitialized;

    public SolveListPresenter(Bundle arguments) {
        mIsCurrent = arguments.getBoolean(INIT_CURRENT);
        mSessionSolvesListener = (update, solve) -> {
            updateView();
            updateAdapter(update, solve);
        };
    }

    public static SolveListFragment newInstance(boolean current) {
        Bundle args = new Bundle();
        args.putBoolean(INIT_CURRENT, current);
        SolveListFragment fragment = new SolveListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewAttached(SolveListView view) {
        super.onViewAttached(view);

        if (PuzzleType.isInitialized()) {
            setInitialized(PuzzleType.getCurrentId(), PuzzleType.getCurrent().getCurrentSessionId());
        }

        view.getSolveListAdapter().setHeaderEnabled(mIsCurrent);
        view.getSolveListAdapter().updateSignAndMillisecondsMode();
    }

    @Override
    public void onViewDetached() {
        super.onViewDetached();

        mViewInitialized = false;
    }

    public void setInitialized(String puzzleTypeId, String sessionId) {
        mPuzzleTypeId = puzzleTypeId;
        mSessionId = sessionId;

        //noinspection ConstantConditions
        if (isViewAttached() && !getView().getSolveListAdapter().isInitialized()) {
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

        if (!mViewInitialized && isViewAttached()) {
            //noinspection ConstantConditions
            getView().setInitialized();
            updateView();

            mViewInitialized = true;
        }
    }

    public void onDestroyed() {
        if (isViewAttached()) {
            //noinspection ConstantConditions
            PuzzleType.get(mPuzzleTypeId).getSessionDeferred(getView().getContextCompat(), mSessionId)
                    .subscribe(session -> {
                        session.removeListener(mSessionSolvesListener);
                    });
        }
    }

    public void onResetButtonClicked() {
        if (isViewAttached()) {
            //noinspection ConstantConditions
            getView().showResetWarningDialog();
        }
    }

    public void onResetDialogConfirmed() {
        PuzzleType.get(mPuzzleTypeId).resetCurrentSession();
        if (isViewAttached()) {
            //noinspection ConstantConditions
            getView().showSessionResetToast();
        }
    }

    public void onSubmitButtonClicked() {
        if (isViewAttached()) {
            PuzzleType.get(mPuzzleTypeId).submitCurrentSession(getView().getContextCompat());
            getView().showSessionSubmitted();
        }
    }

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
                    //noinspection ConstantConditions
                    getView().enableResetSubmitButtons(false);
                } else {
                    PuzzleType.get(mPuzzleTypeId).deleteSession(getView().getContextCompat(), mSessionId);
                    if (isViewAttached())
                        //noinspection ConstantConditions
                        getView().getContextCompat().finish();
                    return;
                }

                //noinspection ConstantConditions
                getView().showList(false);
            } else {
                if (mIsCurrent) {
                    //noinspection ConstantConditions
                    getView().enableResetSubmitButtons(true);
                } else {
                    session.getLastSolve(getView().getContextCompat()).subscribe(solve -> {
                        //noinspection ConstantConditions
                        Activity act = getView().getContextCompat();
                        boolean displayMillisecondsEnabled =
                                PrefUtils.isDisplayMillisecondsEnabled(getView().getContextCompat());
                        act.setTitle(solve.getTimeString(displayMillisecondsEnabled));
                    });
                }

                //noinspection ConstantConditions
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
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, statsString);
                        Activity context = getView().getContextCompat();
                        Intent chooser = Intent.createChooser(intent, context.getString(R.string.share_dialog_title));
                        context.startActivity(chooser);
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
