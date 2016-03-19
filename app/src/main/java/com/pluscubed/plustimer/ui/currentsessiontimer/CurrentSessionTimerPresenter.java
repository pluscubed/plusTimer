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
    private final PuzzleType.CurrentChangeListener mPuzzleTypeCurrentChangeListener;

    private boolean mViewInitialized;

    @SuppressWarnings("ConstantConditions")
    public CurrentSessionTimerPresenter() {
        mSessionSolvesListener = (update, solve) -> {
            updateView(update, solve);
            updateAdapter(update, solve);
        };

        mPuzzleTypeCurrentChangeListener = () -> {
            if (!isViewAttached()) {
                return;
            }

            reloadSolveList();
            updateView(RecyclerViewUpdate.DATA_RESET, null);


            //Set timer text to ready, scramble text to scrambling
            getView().setScrambleText(getView().getContextCompat().getString(R.string.scrambling));

            //Update options menu (disable)
            getView().enableMenuItems(false);
            getView().showScrambleImage(false);

            //mBldMode = PuzzleType.getCurrent().isBld();

            getView().resetGenerateScramble();

            getView().resetTimer();

        };
        PuzzleType.addCurrentChangeListener(mPuzzleTypeCurrentChangeListener);
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

        SolveDialogUtils.createSolveDialog(
                getView().getContextCompat(),
                false,
                PuzzleType.getCurrent().getId(),
                PuzzleType.getCurrent().getCurrentSessionId(),
                solve
        );
    }

    @Override
    public void onDestroyed() {
        removeSessionSolvesListener();

        PuzzleType.removeCurrentChangeListener(mPuzzleTypeCurrentChangeListener);
    }

    @SuppressWarnings("ConstantConditions")
    private void removeSessionSolvesListener() {
        if (!isViewAttached()) {
            return;
        }

        PuzzleType.getCurrent().getCurrentSessionDeferred(getView().getContextCompat())
                .subscribe(session -> {
                    session.removeListener(mSessionSolvesListener);
                });
    }

    @Override
    public void onViewAttached(CurrentSessionTimerView view) {
        super.onViewAttached(view);

        if (PuzzleType.isInitialized()) {
            setPuzzleTypeInitialized();
        }

        view.getTimeBarAdapter().updateMillisecondsMode();
    }

    @Override
    public void onViewDetached() {
        super.onViewDetached();

        mViewInitialized = false;
    }

    public void onTimingFinished(Solve s) {
    }


    public void setPuzzleTypeInitialized() {
        if (!isViewAttached()) {
            return;
        }

        //noinspection ConstantConditions
        if (isViewAttached() && !getView().getTimeBarAdapter().isInitialized()) {
            reloadSolveList();
        }

        if (!mViewInitialized) {
            //noinspection ConstantConditions
            getView().setPuzzleTypeInitialized();
            updateView(RecyclerViewUpdate.DATA_RESET, null);

            mViewInitialized = true;
        }
    }

    private void reloadSolveList() {
        PuzzleType.getCurrent().getCurrentSessionDeferred(getView().getContextCompat())
                .doOnSuccess(session -> {
                    session.addListener(mSessionSolvesListener);
                })
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
