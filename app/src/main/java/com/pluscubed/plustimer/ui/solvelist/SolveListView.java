package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;

public interface SolveListView {

    Activity getContextCompat();

    void setInitialized();

    SolveListAdapterView getSolveListAdapter();

    void enableResetSubmitButtons(boolean enable);

    void showResetWarningDialog();

    void showSessionSubmittedSnackbar();

    void showSessionResetSnackbar();

    void showList(boolean show);

    void scrollRecyclerView(int position);

}
