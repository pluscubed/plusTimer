package com.pluscubed.plustimer.ui.currentsession;

import android.widget.Toast;

import com.pluscubed.plustimer.base.Presenter;
import com.pluscubed.plustimer.model.PuzzleType;

import rx.Completable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class CurrentSessionPresenter extends Presenter<CurrentSessionView> {

    private boolean mInitialized;

    public void onCreate() {
        PuzzleType.initialize(getView().getContextCompat())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Completable.CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        if (isViewAttached()) {
                            mInitialized = true;

                            if (getView().getCurrentSessionTimerFragment() != null) {
                                getView().getCurrentSessionTimerFragment().getPresenter().setInitialized();
                            }
                            if(getView().getSolveListFragment()!=null){
                                getView().getSolveListFragment().getPresenter()
                                        .setInitialized(PuzzleType.getCurrentId(),
                                                PuzzleType.getCurrent().getCurrentSessionId());
                            }
                            getView().supportInvalidateOptionsMenu();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (isViewAttached()) {
                            Toast.makeText(getView().getContextCompat(), e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onSubscribe(Subscription d) {

                    }
                });
    }
}
