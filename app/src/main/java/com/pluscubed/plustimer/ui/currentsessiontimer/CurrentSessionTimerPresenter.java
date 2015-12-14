package com.pluscubed.plustimer.ui.currentsessiontimer;

import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.pluscubed.plustimer.MvpPresenter;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;

import rx.SingleSubscriber;

public class CurrentSessionTimerPresenter extends MvpPresenter<CurrentSessionTimerView> {

    private final SessionSolvesListener mSessionSolvesListener;

    private boolean mInitialized;

    public CurrentSessionTimerPresenter() {
        mSessionSolvesListener = new SessionSolvesListener();
    }

    public void onDestroy() {
        PuzzleType.getCurrent().getCurrentSession()
                .subscribe(new SingleSubscriber<Session>() {
                    @Override
                    public void onSuccess(Session session) {
                        session.removeSessionListener(mSessionSolvesListener);
                    }

                    @Override
                    public void onError(Throwable error) {
                        Toast.makeText(getView().getContextCompat(), "Error" + error.getMessage(), Toast.LENGTH_SHORT);
                    }
                });
    }

    public void onResume() {

    }

    public void onTimingFinished(Solve s) {
        PuzzleType.getCurrent().getCurrentSession().subscribe(new SingleSubscriber<Session>() {
            @Override
            public void onSuccess(Session session) {
                session.addSolve(s, PuzzleType.getCurrentId());
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(getView().getContextCompat(), "Error" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void setInitialized() {
        mInitialized = true;

        if (isViewAttached()) {
            getView().setInitialized();
            //getView().getTimeBarAdapter().initialize(solves);
            getView().updateStatsAndTimerText(null, RecyclerViewUpdate.DATA_RESET);

            PuzzleType.getCurrent().getCurrentSession()
                    .subscribe(new SingleSubscriber<Session>() {
                        @Override
                        public void onSuccess(Session session) {
                            session.addSessionListener(mSessionSolvesListener, PuzzleType.getCurrentId());
                        }

                        @Override
                        public void onError(Throwable error) {
                            Toast.makeText(getView().getContextCompat(), "Error" + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private class SessionSolvesListener implements ChildEventListener {

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Solve solve = Solve.fromSnapshot(dataSnapshot);

            if (isViewAttached()) {
                getView().updateStatsAndTimerText(solve, RecyclerViewUpdate.INSERT);
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(solve, RecyclerViewUpdate.INSERT);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildKey) {
            Solve solve = Solve.fromSnapshot(dataSnapshot);
            if (isViewAttached()) {
                getView().updateStatsAndTimerText(solve, RecyclerViewUpdate.SINGLE_CHANGE);
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(solve, RecyclerViewUpdate.SINGLE_CHANGE);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            Solve solve = Solve.fromSnapshot(dataSnapshot);
            if (isViewAttached()) {
                getView().updateStatsAndTimerText(solve, RecyclerViewUpdate.REMOVE);
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(solve, RecyclerViewUpdate.REMOVE);
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
        }
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
