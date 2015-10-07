package com.pluscubed.plustimer.ui.currentsession;

import com.hannesdorfmann.mosby.mvp.MvpView;
import com.pluscubed.plustimer.ui.TimeBarRecyclerAdapter;

public interface CurrentSessionTimerView extends MvpView {
    void updateStatsAndTimerText();

    void scrollRecyclerView(int position);

    TimeBarRecyclerAdapter getTimeBarAdapter();

    void setInitialized();
}
