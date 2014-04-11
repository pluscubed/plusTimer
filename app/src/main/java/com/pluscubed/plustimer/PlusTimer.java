package com.pluscubed.plustimer;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;

/**
 * ACRA Bug Reporting Application class
 */
public class PlusTimer extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /*
         * Code from http://commonsware.com/blog/2013/01/28/what-not-to-put-in-your-repo.html
         * by Mark Murphy
         * Copyright © 2010-2013 CommonsWare, LLC
         * Used under the Creative Commons Attribution Non-Commercial Share Alike 3.0 License: http://creativecommons.org/licenses/by-nc-sa/3.0/
         *
         * Changed the string reference name
         */
/*
        if (!BuildConfig.DEBUG) {
            ACRAConfiguration config=ACRA.getNewDefaultConfig(this);
            config.setFormUri(getString(R.string.bugsense_form_uri));
            ACRA.setConfig(config);
            ACRA.init(this);
        }*/
    }
}
