package com.pluscubed.plustimer.ui.solvelist;

import android.os.Bundle;

import com.pluscubed.plustimer.base.RecyclerViewUpdate;
import com.pluscubed.plustimer.model.Solve;

import java.util.List;

public interface SolveListAdapterView {

    boolean isInitialized();

    void setSolves(String puzzleTypeId, List<Solve> solves);

    void notifyChange(RecyclerViewUpdate mode, Solve solve, String stats);

    void onSaveInstanceState(Bundle outState);

    void onPresenterPrepared(SolveListPresenter presenter);

    void onPresenterDestroyed();

    void updateSignAndMillisecondsMode();
}
