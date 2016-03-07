package com.pluscubed.plustimer.ui.currentsessiontimer;

import com.pluscubed.plustimer.base.Presenter;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;

import rx.android.schedulers.AndroidSchedulers;

public class CurrentSessionTimerPresenter extends Presenter<CurrentSessionTimerView> {

    private final Session.SolvesListener mSessionSolvesListener;

    private boolean mInitialized;

    public CurrentSessionTimerPresenter() {
        mSessionSolvesListener = (update, solve) -> {
            updateView(update, solve);
            updateAdapter(update, solve);
        };
    }

    private void updateView(RecyclerViewUpdate update, Solve solve) {
        getView().updateStatsAndTimerText(solve, update);
    }

    private void updateAdapter(RecyclerViewUpdate update, Solve solve) {
        TimeBarRecyclerAdapter adapter = CurrentSessionTimerPresenter.this.getView().getTimeBarAdapter();
        adapter.notifyChange(solve, update);
    }

    public void onDestroy() {
        //TODO: Re-register whenever current session changes
        PuzzleType.getCurrent().getCurrentSessionDeferred(getView().getContextCompat())
                .subscribe(session -> {
                    session.removeListener(mSessionSolvesListener);
                });
    }

    public void onResume() {
        if (PuzzleType.isInitialized()) {
            setInitialized();
        }
    }

    public void onTimingFinished(Solve s) {
    }


    public void setInitialized() {
        if (!mInitialized && isViewAttached()) {
            getView().setInitialized();
            updateView(RecyclerViewUpdate.DATA_RESET, null);

            PuzzleType.getCurrent().getCurrentSessionDeferred(getView().getContextCompat())
                    .doOnSuccess(session -> {
                        session.addListener(mSessionSolvesListener);
                    })
                    .flatMapObservable(session ->
                            session.getSortedSolves(getView().getContextCompat())).toList()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(solves -> {
                        getView().getTimeBarAdapter().initialize(solves);
                        getView().getTimeBarAdapter().notifyChange(null, RecyclerViewUpdate.DATA_RESET);
                    });
        }

        mInitialized = true;
    }

    private class PuzzleTypeObserver {

        /*@Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            //Update quick stats and hlistview
            onPuzzleTypeChanged();

            //Set timer text to ready, scramble text to scrambling
            setScrambleText(getString(R.string.scrambling));

            //Update options menu (disable)
            enableMenuItems(false);
            showScrambleImage(false);

            mBldMode = PuzzleType.getCurrent().isBld();

            resetGenerateScramble();

            resetTimer();

            //TODO
*//*
            PuzzleType.getCurrentId().getCurrentSession()
                    .registerObserver(sessionSolvesListener);*//*
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }*/
    }

    ;
}
