package com.pluscubed.plustimer.ui.currentsessiontimer;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.pluscubed.plustimer.MvpPresenter;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;

public class CurrentSessionTimerPresenter extends MvpPresenter<CurrentSessionTimerView> {

    private final SessionSolvesListener mSessionSolvesListener;
    private boolean mInitialized;

    public CurrentSessionTimerPresenter() {
        mSessionSolvesListener = new SessionSolvesListener();
    }

    public void onDestroy() {
        Session.removeSessionListener(PuzzleType.getCurrent().getCurrentSessionId(), mSessionSolvesListener);
    }

    public void onResume() {

    }


    public void setInitialized() {
        mInitialized = true;

        if (isViewAttached()) {
            PuzzleType.getCurrent()
                    .getCurrentSession()
                    .flatMap(Session::getSolves)
                    .subscribe(solves -> {
                        if (!isViewAttached()) {
                            return;
                        }

                        getView().setInitialized();
                        getView().getTimeBarAdapter().initialize(solves);
                        getView().updateStatsAndTimerText();

                        mSessionSolvesListener.setInitialSize(solves.size());
                        Session.addSessionListener(
                                PuzzleType.getCurrent().getCurrentSessionId(), mSessionSolvesListener);
                    });
        }
    }

    private class SessionSolvesListener implements ChildEventListener {

        private int mInitialSize;
        private int mInitialCount;

        public void setInitialSize(int initialSize) {
            mInitialSize = initialSize;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            if (mInitialCount < mInitialSize) {
                mInitialCount++;
                return;
            }

            if (isViewAttached()) {
                getView().updateStatsAndTimerText();
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(dataSnapshot, RecyclerViewUpdate.INSERT);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildKey) {
            if (isViewAttached()) {
                getView().updateStatsAndTimerText();
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(dataSnapshot, RecyclerViewUpdate.SINGLE_CHANGE);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            if (isViewAttached()) {
                getView().updateStatsAndTimerText();
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(dataSnapshot, RecyclerViewUpdate.REMOVE);
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
