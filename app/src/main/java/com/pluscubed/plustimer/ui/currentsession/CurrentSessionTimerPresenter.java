package com.pluscubed.plustimer.ui.currentsession;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import com.pluscubed.plustimer.App;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.TimeBarRecyclerAdapter;

public class CurrentSessionTimerPresenter extends MvpBasePresenter<CurrentSessionTimerView> {

    private final SessionSolvesListener mSessionSolvesListener;

    private boolean mInitialized;

    public CurrentSessionTimerPresenter() {
        mSessionSolvesListener = new SessionSolvesListener();
    }

    public void onPause() {
        removeSessionListener();
    }

    public void onResume() {
        if (mInitialized) {
            setInitialized();
        }
    }


    public void setInitialized() {
        mInitialized = true;
        addSessionListener();
        if (isViewAttached()) {
            getView().setInitialized();
        }
    }

    private void removeSessionListener() {
        App.getFirebaseUserRef().subscribe(userRef -> {
            String currentSessionId = PuzzleType.getCurrent().getCurrentSessionId();
            Firebase sessionSolves = userRef.child("session-solves").child(currentSessionId);
            sessionSolves.removeEventListener(mSessionSolvesListener);
        });
    }

    private void addSessionListener() {
        App.getFirebaseUserRef().subscribe(userRef -> {
            if (!PuzzleType.getPuzzleTypes().isEmpty()) {
                String currentSessionId = PuzzleType.getCurrent().getCurrentSessionId();
                Firebase sessionSolves = userRef.child("session-solves").child(currentSessionId);
                sessionSolves.addChildEventListener(mSessionSolvesListener);

                App.getChildEventListenerMap().put("session-solves/" + currentSessionId, mSessionSolvesListener);
            }
        });
    }


    private class SessionSolvesListener implements ChildEventListener {

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            if (isViewAttached()) {
                getView().updateStatsAndTimerText();
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(dataSnapshot, TimeBarRecyclerAdapter.Update.INSERT);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildKey) {
            if (isViewAttached()) {
                getView().updateStatsAndTimerText();
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(dataSnapshot, TimeBarRecyclerAdapter.Update.SINGLE_CHANGE);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            if (isViewAttached()) {
                getView().updateStatsAndTimerText();
                TimeBarRecyclerAdapter adapter = getView().getTimeBarAdapter();
                adapter.notifyChange(dataSnapshot, TimeBarRecyclerAdapter.Update.REMOVE);
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
        }
    }
}
