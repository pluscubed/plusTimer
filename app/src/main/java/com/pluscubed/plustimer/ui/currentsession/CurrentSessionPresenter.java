package com.pluscubed.plustimer.ui.currentsession;

import android.widget.Toast;

import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.basedrawer.DrawerPresenter;

import rx.Completable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class CurrentSessionPresenter extends DrawerPresenter<CurrentSessionView> {

    @Override
    public void onViewAttached(CurrentSessionView view) {
        super.onViewAttached(view);

        PuzzleType.initialize(view.getContextCompat())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Completable.CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        if (isViewAttached()) {
                            if (getView().getCurrentSessionTimerFragment() != null &&
                                    getView().getCurrentSessionTimerFragment().getPresenter() != null) {
                                getView().getCurrentSessionTimerFragment().getPresenter().setPuzzleTypeInitialized();
                            }
                            if (getView().getSolveListFragment() != null &&
                                    getView().getSolveListFragment().getPresenter() != null) {
                                getView().getSolveListFragment().getPresenter()
                                        .setPuzzleTypeInitialized(PuzzleType.getCurrentId(),
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

    public static class Factory implements PresenterFactory<CurrentSessionPresenter> {

        @Override
        public CurrentSessionPresenter create() {
            return new CurrentSessionPresenter();
        }
    }
}
