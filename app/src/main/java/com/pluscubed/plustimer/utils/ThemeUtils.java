package com.pluscubed.plustimer.utils;

import android.app.Activity;
import android.content.Context;

import com.afollestad.materialdialogs.Theme;
import com.pluscubed.plustimer.R;

public class ThemeUtils {

    private Context mContext;
    private boolean darkMode;
    private boolean blackMode;

    public ThemeUtils(Activity context) {
        mContext = context;
        isChanged(); // invalidate stored booleans
    }

    public static Theme getDialogTheme(Context context) {
        if (PrefUtils.getTheme(context) == PrefUtils.Theme.DARK
                || PrefUtils.getTheme(context) == PrefUtils.Theme.BLACK) {
            return Theme.DARK;
        } else {
            return Theme.LIGHT;
        }
    }

    public boolean isChanged() {
        boolean darkTheme = PrefUtils.getTheme(mContext) == PrefUtils.Theme.DARK;
        boolean blackTheme = PrefUtils.getTheme(mContext) == PrefUtils.Theme.BLACK;

        boolean changed = darkMode != darkTheme || blackMode != blackTheme;
        darkMode = darkTheme;
        blackMode = blackTheme;
        return changed;
    }

    public int getCurrent(boolean hasNavDrawer) {
        if (hasNavDrawer) {
            if (blackMode) {
                return R.style.Theme_PlusTimer_Black_WithNavDrawer;
            } else if (darkMode) {
                return R.style.Theme_PlusTimer_Dark_WithNavDrawer;
            } else {
                return R.style.Theme_PlusTimer_WithNavDrawer;
            }
        } else {
            if (blackMode) {
                return R.style.Theme_PlusTimer_Black;
            } else if (darkMode) {
                return R.style.Theme_PlusTimer_Dark;
            } else {
                return R.style.Theme_PlusTimer;
            }
        }
    }
}
