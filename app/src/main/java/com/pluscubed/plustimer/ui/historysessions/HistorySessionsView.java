package com.pluscubed.plustimer.ui.historysessions;

import android.app.Activity;

import com.pluscubed.plustimer.model.PuzzleType;
import com.pluscubed.plustimer.ui.SpinnerPuzzleTypeAdapter;

public interface HistorySessionsView {

    Activity getContextCompat();

    void showList(boolean show);

    HistorySessionsAdapterView getHistorySessionsAdapter();

    SpinnerPuzzleTypeAdapter getPuzzleTypeSpinnerAdapter();

    void initPuzzleSpinnerSelection(PuzzleType type);

    void invalidateOptionsMenu();
}
