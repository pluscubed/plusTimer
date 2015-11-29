package com.pluscubed.plustimer.ui.currentsessiontimer;

import android.app.Activity;

import com.pluscubed.plustimer.MvpView;

public interface CurrentSessionTimerView extends MvpView {
    void updateStatsAndTimerText();

    void scrollRecyclerView(int position);

    TimeBarRecyclerAdapter getTimeBarAdapter();

    void setInitialized();

    Activity getContextCompat();
}
