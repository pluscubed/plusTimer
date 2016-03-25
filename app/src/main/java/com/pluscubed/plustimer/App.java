package com.pluscubed.plustimer;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.squareup.leakcanary.LeakCanary;

import io.fabric.sdk.android.Fabric;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.USE_CRASHLYTICS) {
            Fabric.with(this, new Crashlytics());
        }

        LeakCanary.install(this);

        /*LockContext.configureLock(
                new Lock.Builder()
                        .loadFromApplication(this)
                        //.useWebView(true)
                        //.defaultDatabaseConnection(null)
                        //.withIdentityProvider(Strategies.UnknownSocial, new WebIdentityProvider(new CallbackParser(), getString(R.string.auth0_client_id), getString(R.string.auth0_domain_name)+"/authorize"))
                        //.useConnections("oauth2")
                        //.defaultDatabaseConnection("oauth2")
                        .closable(true)
        );*/
    }
}
