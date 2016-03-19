package com.pluscubed.plustimer.ui.currentsessiontimer;

import android.app.Activity;

import com.pluscubed.plustimer.base.RecyclerViewUpdate;
import com.pluscubed.plustimer.model.Solve;

public interface CurrentSessionTimerView {

    void scrollRecyclerView(int position);

    TimeBarRecyclerAdapter getTimeBarAdapter();

    void updateStatsAndTimerText(RecyclerViewUpdate mode, Solve solve);

    void setPuzzleTypeInitialized();

    Activity getContextCompat();

    void setScrambleText(String string);

    void enableMenuItems(boolean b);

    void showScrambleImage(boolean b);

    //REMOVE WHEN MVP FULLY IMPLEMENTED
    void resetGenerateScramble();

    void updateBld();

    void resetTimer();
}
