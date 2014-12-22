package com.pluscubed.plustimer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.afollestad.materialdialogs.Theme;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.ui.SettingsActivity;

public class ThemeUtils {

    private Context mContext;
    private boolean darkMode;
    private boolean trueBlack;

    public ThemeUtils(Activity context) {
        mContext = context;
        isChanged(); // invalidate stored booleans
    }

    public static boolean isDarkMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(SettingsActivity.PREF_THEME_LIST, "0").equals("1");
    }

    public static boolean isTrueBlack(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(SettingsActivity.PREF_THEME_LIST, "0").equals("2");
    }

    public static Theme getDialogTheme(Context context) {
        if (isDarkMode(context) || isTrueBlack(context)) return Theme.DARK;
        else return Theme.LIGHT;
    }

    public boolean isChanged() {
        boolean darkTheme = isDarkMode(mContext);
        boolean blackTheme = isTrueBlack(mContext);

        boolean changed = darkMode != darkTheme || trueBlack != blackTheme;
        darkMode = darkTheme;
        trueBlack = blackTheme;
        return changed;
    }

    public int getCurrent(boolean hasNavDrawer) {
        if (hasNavDrawer) {
            if (trueBlack) {
                return R.style.Theme_PlusTimer_Black_WithNavDrawer;
            } else if (darkMode) {
                return R.style.Theme_PlusTimer_Dark_WithNavDrawer;
            } else {
                return R.style.Theme_PlusTimer_WithNavDrawer;
            }
        } else {
            if (trueBlack) {
                return R.style.Theme_PlusTimer_Black;
            } else if (darkMode) {
                return R.style.Theme_PlusTimer_Dark;
            } else {
                return R.style.Theme_PlusTimer;
            }
        }
    }
}
