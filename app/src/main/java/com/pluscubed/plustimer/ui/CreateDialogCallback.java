package com.pluscubed.plustimer.ui;

/**
 * On Solve Item
 */
interface CreateDialogCallback {

    void createSolveDisplayDialog(String displayName, int sessionIndex,
                                  int solveIndex);

    void createSolveAddDialog(String displayName, int sessionIndex);
}
