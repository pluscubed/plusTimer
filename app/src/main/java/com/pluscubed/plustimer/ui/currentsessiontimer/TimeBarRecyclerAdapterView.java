package com.pluscubed.plustimer.ui.currentsessiontimer;

import android.os.Bundle;

import com.pluscubed.plustimer.base.RecyclerViewUpdate;
import com.pluscubed.plustimer.model.Solve;

import java.util.List;

public interface TimeBarRecyclerAdapterView {

    void setSolves(List<Solve> solves);

    boolean isInitialized();

    void notifyChange(RecyclerViewUpdate mode, Solve solve);

    void scrollRecyclerViewToLast(CurrentSessionTimerView view);

    void updateMillisecondsMode();

    void onSaveInstanceState(Bundle outState);

    void onPresenterPrepared(CurrentSessionTimerPresenter presenter);

    void onPresenterDestroyed();
}
