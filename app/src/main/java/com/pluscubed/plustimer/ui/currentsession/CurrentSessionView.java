package com.pluscubed.plustimer.ui.currentsession;

import android.app.Activity;

import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.basedrawer.DrawerView;
import com.pluscubed.plustimer.ui.currentsessiontimer.CurrentSessionTimerFragment;
import com.pluscubed.plustimer.ui.solvelist.SolveListFragment;

import java.util.List;

public interface CurrentSessionView extends DrawerView {

    void supportInvalidateOptionsMenu();

    CurrentSessionTimerFragment getCurrentSessionTimerFragment();

    SolveListFragment getSolveListFragment();

    Activity getContextCompat();

    void initPuzzleSpinner(List<PuzzleType> list, int selectedPosition);
}
