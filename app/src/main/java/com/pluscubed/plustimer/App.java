package com.pluscubed.plustimer;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import io.fabric.sdk.android.Fabric;
import rx.Observable;
import rx.schedulers.Schedulers;

public class App extends Application {
    private static Firebase sFirebaseUserRef;

    public static Observable<Firebase> getFirebaseUserRef() {
        return Observable.<Firebase>create(subscriber -> {
            //TODO: Make synchronized
            if (sFirebaseUserRef == null) {
                Firebase ref = new Firebase("https://plustimer.firebaseio.com/");

                if (ref.getAuth() != null) {
                    sFirebaseUserRef = new Firebase(ref.toString() + "/users/" + ref.getAuth().getUid());
                    sFirebaseUserRef.keepSynced(true);
                    subscriber.onNext(sFirebaseUserRef);
                    subscriber.onCompleted();
                    return;
                }

                ref.authAnonymously(new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        sFirebaseUserRef = new Firebase(ref.toString() + "/users/" + authData.getUid());
                        sFirebaseUserRef.keepSynced(true);
                        subscriber.onNext(sFirebaseUserRef);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError) {
                        subscriber.onError(firebaseError.toException());
                    }
                });
            } else {
                subscriber.onNext(sFirebaseUserRef);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.USE_CRASHLYTICS) {
            Fabric.with(this, new Crashlytics());
        }
        Firebase.setAndroidContext(this);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);
    }
}
