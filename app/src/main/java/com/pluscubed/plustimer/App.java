package com.pluscubed.plustimer;

import android.app.Application;
import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.crashlytics.android.Crashlytics;

import java.io.IOException;

import io.fabric.sdk.android.Fabric;

public class App extends Application {


    private static final String DB_SOLVES = "db_solves";
    private static Database sDatabase;

    public static Database getDatabase(Context context) throws CouchbaseLiteException, IOException {
        if (sDatabase == null) {
            Manager manager = new Manager(new AndroidContext(context.getApplicationContext()), Manager.DEFAULT_OPTIONS);
            sDatabase = manager.getDatabase(DB_SOLVES);
        }
        return sDatabase;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.USE_CRASHLYTICS) {
            Fabric.with(this, new Crashlytics());
        }
    }
}
