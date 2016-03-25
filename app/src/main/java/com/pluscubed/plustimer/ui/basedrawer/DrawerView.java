package com.pluscubed.plustimer.ui.basedrawer;


import android.app.Activity;

public interface DrawerView {
    Activity getContextCompat();

    void displayToast(String message);

    void setProfileImage(String url);

    void setHeaderText(String title, String subtitle);
}
