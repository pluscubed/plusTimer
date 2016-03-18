package com.pluscubed.plustimer.ui.historysessions;

import android.app.Activity;

public interface HistorySessionsView {

    Activity getContextCompat();

    void showList(boolean show);

    HistorySessionsAdapterView getHistorySessionsAdapter();
}
