package com.pluscubed.plustimer.ui.currentsessiontimer;

import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.Presenter;
import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.base.RecyclerViewUpdate;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.utils.SolveDialogUtils;

import rx.android.schedulers.AndroidSchedulers;

public class CurrentSessionTimerPresenter extends Presenter<CurrentSessionTimerView> {

    private final Session.SolvesListener mSessionSolvesListener;
    private final PuzzleType.CurrentSessionChangeListener mCurrentSessionChangeListener;

    private boolean mInitialized;
    private boolean mViewInitialized;

    @SuppressWarnings("ConstantConditions")
    public CurrentSessionTimerPresenter() {
        mSessionSolvesListener = (update, solve) -> {
            updateView(update, solve);
            updateAdapter(update, solve);
        };

        mCurrentSessionChangeListener = oldType -> {
            if (!isViewAttached()) {
                return;
            }

            removeSessionSolvesListener(oldType);

            reloadSolveList();
            updateView(RecyclerViewUpdate.DATA_RESET, null);


            //Set timer text to ready, scramble text to scrambling
            getView().setScrambleText(getView().getContextCompat().getString(R.string.scrambling));

            //Update options menu (disable)
            getView().enableMenuItems(false);
            getView().showScrambleImage(false);

            getView().updateBld();

            getView().resetGenerateScramble();
            getView().resetTimer();

            attachSessionSolvesListener(getView());
        };
        PuzzleType.addCurrentChangeListener(mCurrentSessionChangeListener);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateView(RecyclerViewUpdate update, Solve solve) {
        if (!isViewAttached()) {
            return;
        }
        getView().updateStatsAndTimerText(update, solve);

        TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
        adapter.scrollRecyclerViewToLast(getView());

        getView().updateBld();
    }

    @SuppressWarnings("ConstantConditions")
    private void updateAdapter(RecyclerViewUpdate change, Solve solve) {
        if (!isViewAttached()) {
            return;
        }

        TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
        adapter.notifyChange(change, solve);

        switch (change) {
            case DATA_RESET:
                adapter.scrollRecyclerViewToLast(getView());
                break;
            case INSERT:
                adapter.scrollRecyclerViewToLast(getView());
                break;
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onSolveClicked(Solve solve) {
        if (!isViewAttached()) {
            return;
        }

        PuzzleType.getCurrent(getView().getContextCompat())
                .map(PuzzleType::getCurrentSessionId)
                .subscribe(session ->
                        SolveDialogUtils.createSolveDialog(
                                getView().getContextCompat(),
                                false,
                                PuzzleType.getCurrentId(getView().getContextCompat()),
                                session,
                                solve
                        )
                );

    }

    @Override
    public void onDestroyed() {
        removeSessionSolvesListener(null);

        PuzzleType.removeCurrentChangeListener(mCurrentSessionChangeListener);
    }

    @SuppressWarnings("ConstantConditions")
    private void attachSessionSolvesListener(CurrentSessionTimerView view) {
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

    @Override
    public void onViewAttached(CurrentSessionTimerView view) {
        super.onViewAttached(view);

        initializeView();

        view.getTimeBarAdapter().updateMillisecondsMode();

        if (!mInitialized) {
            attachSessionSolvesListener(view);

            mInitialized = true;
        }
    }

    @Override
    public void onViewDetached() {
        super.onViewDetached();

        mViewInitialized = false;
    }

    public void onTimingFinished(Solve s) {
    }


    public void initializeView() {
        if (!isViewAttached()) {
            return;
        }

        //noinspection ConstantConditions
        if (!getView().getTimeBarAdapter().isInitialized()) {
            reloadSolveList();
        }

        if (!mViewInitialized) {
            //noinspection ConstantConditions
            getView().setInitialized();
            updateView(RecyclerViewUpdate.DATA_RESET, null);

            mViewInitialized = true;
        }
    }

    private void reloadSolveList() {
        PuzzleType.getCurrent(getView().getContextCompat())
                .flatMap(puzzleType -> puzzleType.getCurrentSessionDeferred(getView().getContextCompat()))
                .flatMapObservable(session ->
                        session.getSortedSolves(getView().getContextCompat())).toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(solves -> {
                    getView().getTimeBarAdapter().setSolves(solves);
                    updateAdapter(RecyclerViewUpdate.DATA_RESET, null);
                });
    }

    public static class Factory implements PresenterFactory<CurrentSessionTimerPresenter> {

        @Override
        public CurrentSessionTimerPresenter create() {
            return new CurrentSessionTimerPresenter();
        }
    }
}
