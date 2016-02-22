package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.couchbase.lite.CouchbaseLiteException;
import com.pluscubed.plustimer.MvpPresenter;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.SolveDialogUtils;

import java.io.IOException;

import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;

public class SolveListPresenter extends MvpPresenter<SolveListView> {

    //TODO: STATE
    private static final String INIT_CURRENT = "current";
    private final Session.SolvesListener mSessionSolvesListener;
    private String mPuzzleTypeId;
    private String mSessionId;
    private boolean mIsCurrent;
    private boolean mInitialized;

    public SolveListPresenter() {
        mSessionSolvesListener = (update, solve) -> {
            updateView();
            updateAdapter(solve, update);
        };
    }

    public static SolveListFragment newInstance(boolean current) {
        Bundle args = new Bundle();
        args.putBoolean(INIT_CURRENT, current);
        SolveListFragment fragment = new SolveListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public SolveListAdapter newAdapter() {
        return new SolveListAdapter(getView(), mIsCurrent);
    }

    public void setInitialized(String puzzleTypeId, String sessionId) {
        mPuzzleTypeId = puzzleTypeId;
        mSessionId = sessionId;
        mInitialized = true;

        if (isViewAttached()) {
            getView().setInitialized();
            updateView();

            PuzzleType.get(mPuzzleTypeId).getSessionDeferred(getView().getContextCompat(), mSessionId)
                    .doOnSuccess(session -> {
                        session.addListener(mSessionSolvesListener);
                    })
                    .flatMapObservable(session ->
                            session.getSortedSolves(getView().getContextCompat())).toList()
                    .subscribe(solves -> {
                        getView().getSolveListAdapter().initialize(solves);
                        updateAdapter(null, RecyclerViewUpdate.DATA_RESET);
                    });
        }
    }

    public void onDestroy() {
        PuzzleType.get(mPuzzleTypeId).getSessionDeferred(getView().getContextCompat(), mSessionId)
                .subscribe(session -> {
                    session.removeListener(mSessionSolvesListener);
                });
    }

    public void onResume() {

    }

    public void onCreate(Bundle arguments) {
        mIsCurrent = arguments.getBoolean(INIT_CURRENT);
    }

    public boolean isCreateResetSubmitViews() {
        return mIsCurrent;
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

    private void updateView() {
        try {
            Session session = PuzzleType.get(mPuzzleTypeId).getSession(getView().getContextCompat(), mSessionId);
            int numberOfSolves = session.getNumberOfSolves();

            if (!isViewAttached()) {
                return;
            }

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

    private void updateAdapter(Solve solve, RecyclerViewUpdate change) {
        if (isViewAttached()) {

            Activity context = getView().getContextCompat();
            try {
                Session session = PuzzleType.get(mPuzzleTypeId).getSession(context, mSessionId);

                String stats = session.getStats(context,
                        mPuzzleTypeId,
                        mIsCurrent,
                        false,
                        PrefUtils.isDisplayMillisecondsEnabled(context),
                        PrefUtils.isSignEnabled(context));

                getView().getSolveListAdapter().notifyChange(mPuzzleTypeId, mSessionId, solve, change, stats);
            } catch (CouchbaseLiteException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
