package com.pluscubed.plustimer.ui.solvelist;

import android.app.Activity;

import com.pluscubed.plustimer.MvpView;

public interface SolveListView extends MvpView {

    Activity getContextCompat();

    void setInitialized();

    SolveListAdapter getSolveListAdapter();

    void enableResetSubmitButtons(boolean enable);

    void showResetWarningDialog();

    void showSessionSubmitted();

    void showSessionResetToast();

    void showList(boolean show);

    void scrollRecyclerView(int position);

}
