package com.pluscubed.plustimer.ui;

/**
 * On Solve Item
 */
public interface CreateDialogCallback {

    void createSolveDisplayDialog(String displayName, int sessionIndex,
                                  int solveIndex);

    void createSolveAddDialog(String displayName, int sessionIndex);
}
