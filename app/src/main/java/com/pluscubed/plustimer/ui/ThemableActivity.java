package com.pluscubed.plustimer.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.pluscubed.plustimer.utils.ThemeUtils;


public class ThemableActivity extends ActionBarActivity {

    private ThemeUtils mThemeUtils;

    protected boolean hasNavDrawer() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeUtils = new ThemeUtils(this);
        setTheme(mThemeUtils.getCurrent(hasNavDrawer()));
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mThemeUtils.isChanged()) {
            setTheme(mThemeUtils.getCurrent(hasNavDrawer()));
            recreate();
        }
    }
}
