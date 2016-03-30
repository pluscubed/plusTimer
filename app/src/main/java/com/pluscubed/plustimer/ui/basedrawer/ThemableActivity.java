package com.pluscubed.plustimer.ui.basedrawer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;

import com.pluscubed.plustimer.base.BasePresenterActivity;
import com.pluscubed.plustimer.base.Presenter;
import com.pluscubed.plustimer.utils.ThemeUtils;


public abstract class ThemableActivity<P extends Presenter<V>, V> extends BasePresenterActivity<P, V> {

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
            Intent i = getIntent();
            finish();
            startActivity(i);
        }
    }


    // Workaround for LG bug with support library
    // See: http://stackoverflow.com/questions/26833242/

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND) || super
                .onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
