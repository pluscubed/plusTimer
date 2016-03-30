package com.pluscubed.plustimer.ui.historysessions;

import android.app.Activity;

import com.pluscubed.plustimer.model.PuzzleType;

import java.util.List;

public interface HistorySessionsView {

    Activity getContextCompat();

    void showList(boolean show);

    HistorySessionsAdapterView getHistorySessionsAdapter();

    void initPuzzleSpinner(List<PuzzleType> puzzleTypes, int position);
}
