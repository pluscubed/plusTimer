package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.pluscubed.plustimer.MvpPresenter;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.model.Session;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;
import com.pluscubed.plustimer.utils.PrefUtils;
import com.pluscubed.plustimer.utils.SolveDialogUtils;

import rx.SingleSubscriber;
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
                    .subscribe(new SingleSubscriber<Session>() {
                        @Override
                        public void onSuccess(Session session) {
                            if (!isViewAttached()) {
                                return;
                            }

                            getView().setInitialized();
                            //getView().getSolveListAdapter().initialize(mPuzzleTypeId, mSessionId, solves);
                            onSessionSolvesChanged();

                            session.addSessionListener(mSessionSolvesListener, mPuzzleTypeId);
                        }

                        @Override
                        public void onError(Throwable error) {
                            Toast.makeText(getView().getContextCompat(), "Error", Toast.LENGTH_SHORT);
                        }
                    });
        }
    }

    public void onDestroy() {
        PuzzleType.get(mPuzzleTypeId)
                .getSession(mSessionId)
                .subscribe(session -> {
                    if (!isViewAttached()) {
                        return;
                    }
                    session.removeSessionListener(mSessionSolvesListener);
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

    private void onSessionSolvesChanged() {
        /*mSession = getPuzzleType().getSession(mSessionId);*/

        final Session[] session = {null};

        PuzzleType.get(mPuzzleTypeId).getSession(mSessionId)
                .doOnSuccess(s -> session[0] = s)
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


        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Solve solve = Solve.fromSnapshot(dataSnapshot);
            if (isViewAttached()) {
                //getView().updateStatsAndTimerText();
                SolveListAdapter adapter = getView().getSolveListAdapter();
                adapter.notifyChange(mPuzzleTypeId, mSessionId, solve, RecyclerViewUpdate.INSERT);
                onSessionSolvesChanged();
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildKey) {
            Solve solve = Solve.fromSnapshot(dataSnapshot);
            if (isViewAttached()) {
                //getView().updateStatsAndTimerText();
                SolveListAdapter adapter = getView().getSolveListAdapter();
                adapter.notifyChange(mPuzzleTypeId, mSessionId, solve, RecyclerViewUpdate.SINGLE_CHANGE);
                onSessionSolvesChanged();
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            Solve solve = Solve.fromSnapshot(dataSnapshot);
            if (isViewAttached()) {
                //getView().updateStatsAndTimerText();
                SolveListAdapter adapter = getView().getSolveListAdapter();
                adapter.notifyChange(mPuzzleTypeId, mSessionId, solve, RecyclerViewUpdate.REMOVE);
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
