package com.pluscubed.plustimer.ui.currentsession;

import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.basedrawer.DrawerPresenter;

import rx.android.schedulers.AndroidSchedulers;

public class CurrentSessionPresenter extends DrawerPresenter<CurrentSessionView> {

    @Override
    public void onViewAttached(CurrentSessionView view) {
        super.onViewAttached(view);

        /*PuzzleType.initialize(view.getContextCompat())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Completable.CompletableSubscriber() {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void onCompleted() {
                        if (isViewAttached()) {
                            if (getView().getCurrentSessionTimerFragment() != null &&
                                    getView().getCurrentSessionTimerFragment().getPresenter() != null) {
                                getView().getCurrentSessionTimerFragment().getPresenter().initializeView();
                            }
                            if (getView().getSolveListFragment() != null &&
                                    getView().getSolveListFragment().getPresenter() != null) {
                                getView().getSolveListFragment().getPresenter()
                                        .initializeView(PuzzleType.getCurrentId(),
                                                PuzzleType.getCurrent().getCurrentSessionId());
                            }
                            getView().supportInvalidateOptionsMenu();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (isViewAttached()) {
                            //noinspection ConstantConditions
                            Toast.makeText(getView().getContextCompat(), e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onSubscribe(Subscription d) {

                    }
                });*/

        PuzzleType.getEnabledPuzzleTypes(view.getContextCompat())
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    String id = PuzzleType.getCurrentId(getView().getContextCompat());
                    int position = 0;
                    for (int i = 0; i < list.size(); i++) {
                        PuzzleType type = list.get(i);
                        if (type.getId().equals(id)) {
                            position = i;
                            break;
                        }
                    }
                    getView().initPuzzleSpinner(list, position);
                });
    }

    public void onPuzzleSelected(PuzzleType newPuzzleType) {
        if (!isViewAttached()) {
            return;
        }

        PuzzleType.setCurrent(getView().getContextCompat(), newPuzzleType.getId())
                .subscribe();
    }

    public void onCreateOptionsMenu() {
        if (!isViewAttached()) {
            return;
        }


    }

    public static class Factory implements PresenterFactory<CurrentSessionPresenter> {

        @Override
        public CurrentSessionPresenter create() {
            return new CurrentSessionPresenter();
        }
    }
}
