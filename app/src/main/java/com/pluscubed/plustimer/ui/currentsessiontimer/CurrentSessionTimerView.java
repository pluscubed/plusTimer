package com.pluscubed.plustimer.ui.currentsessiontimer;

import android.app.Activity;

import com.pluscubed.plustimer.MvpView;
import com.pluscubed.plustimer.model.Solve;
import com.pluscubed.plustimer.ui.RecyclerViewUpdate;

public interface CurrentSessionTimerView extends MvpView {
    void updateStatsAndTimerText(Solve solve, RecyclerViewUpdate mode);

    void scrollRecyclerView(int position);

    TimeBarRecyclerAdapter getTimeBarAdapter();

    void setInitialized();

    Activity getContextCompat();
}
