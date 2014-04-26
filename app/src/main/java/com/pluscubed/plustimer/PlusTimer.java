package com.pluscubed.plustimer;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * ACRA Bug Reporting Application class
 */

@ReportsCrashes(formKey = "")
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
         * Changed the string reference name & added dialog settings
         */

        if (!BuildConfig.DEBUG) {
            ACRAConfiguration config = ACRA.getNewDefaultConfig(this);
            config.setFormUri(getString(R.string.bugsense_form_uri));
            try {
                config.setMode(ReportingInteractionMode.DIALOG);
            } catch (ACRAConfigurationException e) {
                e.printStackTrace();
            }
            config.setResDialogText(R.string.acra_crash_dialog_text);
            config.setResDialogTitle(R.string.acra_crash_dialog_title);
            ACRA.setConfig(config);
            ACRA.init(this);
        }
    }
}
