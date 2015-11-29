package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;
import android.os.Bundle;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.pluscubed.plustimer.MvpPresenter;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;
import com.pluscubed.plustimer.utils.PrefUtils;

import rx.android.schedulers.AndroidSchedulers;

public class SolveListPresenter extends MvpPresenter<SolveListView> {

    //TODO: STATE
    private static final String INIT_CURRENT = "current";
    private final SessionSolvesListener mSessionSolvesListener;
    private String mPuzzleTypeId;
    private String mSessionId;
    private boolean mIsCurrent;
    private boolean mInitialized;

    public SolveListPresenter() {
        mSessionSolvesListener = new SessionSolvesListener();
    }

    public static SolveListFragment newInstance(boolean current) {
        Bundle args = new Bundle();
        args.putBoolean(INIT_CURRENT, current);
        SolveListFragment fragment = new SolveListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public void setInitialized(String puzzleTypeId, String sessionId) {
        mPuzzleTypeId = puzzleTypeId;
        mSessionId = sessionId;
        mInitialized = true;

        if (isViewAttached()) {
            PuzzleType.get(mPuzzleTypeId)
                    .getSession(mSessionId)
                    .flatMap(Session::getSolves)
                    .subscribe(solves -> {
                        if (!isViewAttached()) {
                            return;
                        }

                        getView().setInitialized();
                        getView().getSolveListAdapter().initialize(mPuzzleTypeId, mSessionId, solves);
                        onSessionSolvesChanged();

                        mSessionSolvesListener.setInitialSize(solves.size());
                        Session.addSessionListener(mSessionId, mSessionSolvesListener);
                    });
        }
    }

    public void onDestroy() {
        Session.removeSessionListener(mSessionId, mSessionSolvesListener);
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


    private void onSessionSolvesChanged() {
        /*mSession = getPuzzleType().getSession(mSessionId);*/

        final Session[] session = {null};

        PuzzleType.get(mPuzzleTypeId).getSession(mSessionId)
                .doOnNext(s -> session[0] = s)
                .flatMap(Session::getNumberOfSolves)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(numberOfSolves -> {
                    if (!isViewAttached()) {
                        return;
                    }

                    if (numberOfSolves <= 0) {
                        if (mIsCurrent) {
                            //noinspection ConstantConditions
                            getView().enableResetSubmitButtons(false);
                        } else {
                            PuzzleType.get(mPuzzleTypeId).deleteSession(mSessionId);
                            if (isViewAttached())
                                //noinspection ConstantConditions
                                ((Activity) getView().getContextCompat()).finish();
                            return;
                        }

                        //noinspection ConstantConditions
                        getView().showList(false);
                    } else {

                        if (mIsCurrent) {
                            //noinspection ConstantConditions
                            getView().enableResetSubmitButtons(true);
                        } else {
                            session[0].getLastSolve().subscribe(solve -> {
                                //noinspection ConstantConditions
                                Activity act = (Activity) getView().getContextCompat();
                                boolean displayMillisecondsEnabled =
                                        PrefUtils.isDisplayMillisecondsEnabled(getView().getContextCompat());
                                act.setTitle(solve.getTimeString(displayMillisecondsEnabled));
                            });
                        }

                        //noinspection ConstantConditions
                        getView().showList(true);
                    }
                });
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
                //getView().updateStatsAndTimerText();
                SolveListAdapter adapter = getView().getSolveListAdapter();
                adapter.notifyChange(mPuzzleTypeId, mSessionId, dataSnapshot, RecyclerViewUpdate.INSERT);
                onSessionSolvesChanged();
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildKey) {

            if (isViewAttached()) {
                //getView().updateStatsAndTimerText();
                SolveListAdapter adapter = getView().getSolveListAdapter();
                adapter.notifyChange(mPuzzleTypeId, mSessionId, dataSnapshot, RecyclerViewUpdate.SINGLE_CHANGE);
                onSessionSolvesChanged();
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            if (isViewAttached()) {
                //getView().updateStatsAndTimerText();
                SolveListAdapter adapter = getView().getSolveListAdapter();
                adapter.notifyChange(mPuzzleTypeId, mSessionId, dataSnapshot, RecyclerViewUpdate.REMOVE);
                onSessionSolvesChanged();
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
