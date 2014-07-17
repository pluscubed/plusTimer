package com.pluscubed.plustimer;


import android.os.Handler;

/**
 * Allows CurrentSTimerRetainedFragment to communicate with timerfragment
 */
public interface CurrentSTimerFragmentCallback {
    Handler getUiHandler();

    void updateScrambleTextAndImageToCurrent();

    void enableOptionsMenu(boolean enable);
}
