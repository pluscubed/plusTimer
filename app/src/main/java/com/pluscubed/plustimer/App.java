package com.pluscubed.plustimer;

import android.app.Application;
import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.crashlytics.android.Crashlytics;
import com.squareup.leakcanary.LeakCanary;

import java.io.IOException;

import io.fabric.sdk.android.Fabric;

public class App extends Application {


    private static final String DB_SOLVES = "db_solves";
    private static Database sDatabase;

    public static Database getDatabase(Context context) throws CouchbaseLiteException, IOException {
        if (sDatabase == null) {
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(true);
            options.setStorageType(Manager.FORESTDB_STORAGE);
            Manager manager = new Manager(new AndroidContext(context.getApplicationContext()), Manager.DEFAULT_OPTIONS);
            sDatabase = manager.openDatabase(DB_SOLVES, options);
        }
        return sDatabase;
    }

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
