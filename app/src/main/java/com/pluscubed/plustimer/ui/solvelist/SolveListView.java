package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;

public interface SolveListView {

    Activity getContextCompat();

    void setInitialized();

    SolveListAdapter getSolveListAdapter();

    void enableResetSubmitButtons(boolean enable);

    void showResetWarningDialog();

    void setAdapter(SolveListAdapter adapter);

    void addResetSubmitButtons();

    void showSessionSubmitted();

    void showSessionResetToast();

    void showList(boolean show);

    void scrollRecyclerView(int position);

}
