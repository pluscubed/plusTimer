package com.pluscubed.plustimer.ui.basedrawer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;

import com.auth0.core.Token;
import com.auth0.core.UserProfile;
import com.auth0.identity.IdentityProvider;
import com.auth0.identity.IdentityProviderCallback;
import com.auth0.identity.WebIdentityProvider;
import com.auth0.identity.web.CallbackParser;
import com.couchbase.lite.CouchbaseLiteException;
import com.pluscubed.plustimer.base.Presenter;
import com.pluscubed.plustimer.base.PresenterFactory;
import com.pluscubed.plustimer.model.CouchbaseInstance;

import java.io.IOException;
import java.util.HashMap;

import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;

public class DrawerPresenter<V extends DrawerView> extends Presenter<V> {

    private UserProfile mDisplayedUser;
    private WebIdentityProvider mProvider;

    public DrawerPresenter() {
    }

    @Override
    public void onViewAttached(V view) {
        super.onViewAttached(view);

        try {
            CouchbaseInstance instance = CouchbaseInstance.get(getView().getContextCompat());

            instance.getLoggedInUser()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onUserLoaded(instance));
        } catch (CouchbaseLiteException | IOException e) {
            e.printStackTrace();
            if (isViewAttached())
                getView().displayToast(e.getMessage());
        }
    }

    @NonNull
    private SingleSubscriber<UserProfile> onUserLoaded(final CouchbaseInstance instance) {
        return new SingleSubscriber<UserProfile>() {
            @Override
            public void onSuccess(UserProfile user) {
                mDisplayedUser = user;

                if (mDisplayedUser != null) {
                    updateView();

                    instance.startReplication();
                }
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
                if (isViewAttached())
                    getView().displayToast(error.getMessage());
            }
        };
    }

    @SuppressWarnings("ConstantConditions")
    private void signIn() throws CouchbaseLiteException, IOException {
        if (!isViewAttached()) {
            return;
        }

        CouchbaseInstance instance = CouchbaseInstance.get(getView().getContextCompat());

        //getView().getContextCompat().startActivity(new Intent(getView().getContextCompat(), LockActivity.class));
        mProvider = new WebIdentityProvider(
                new CallbackParser(),
                instance.getAuth0().getClientId(),
                instance.getAuth0().getAuthorizeUrl()
        );

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("scope", "openid roles offline_access");
        parameters.put("device", Build.MODEL);
        mProvider.setParameters(parameters);
        mProvider.setCallback(new IdentityProviderCallback() {
            @Override
            public void onFailure(Dialog dialog) {
            }

            @Override
            public void onFailure(int titleResource, int messageResource, Throwable cause) {
                getView().displayToast(getView().getContextCompat().getString(titleResource));
            }

            @Override
            public void onSuccess(String serviceName, String accessToken) {
            }

            @Override
            public void onSuccess(Token token) {
                instance.signIn(token.getIdToken())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(onUserLoaded(instance));
            }
        });
        mProvider.start(getView().getContextCompat(), "WCA");
    }

    @SuppressWarnings("ConstantConditions")
    private void updateView() {
        if (!isViewAttached()) {
            return;
        }

        getView().setProfileImage(mDisplayedUser.getPictureURL());
        getView().setHeaderText(mDisplayedUser.getName(), mDisplayedUser.getEmail());
    }

    @SuppressWarnings("ConstantConditions")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (isViewAttached() && mProvider != null)
            mProvider.authorize(getView().getContextCompat(), requestCode, resultCode, data);
    }

    @SuppressWarnings("ConstantConditions")
    public void onNewIntent(Intent intent) {
        if (isViewAttached() && mProvider != null)
            mProvider.authorize(getView().getContextCompat(), IdentityProvider.WEBVIEW_AUTH_REQUEST_CODE, Activity.RESULT_OK, intent);
    }

    @Override
    public void onViewDetached() {
        mDisplayedUser = null;
        CouchbaseInstance.getDeferred(getView().getContextCompat())
                .subscribe(new SingleSubscriber<CouchbaseInstance>() {
                    @Override
                    public void onSuccess(CouchbaseInstance value) {
                        value.stopReplication();
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });

        super.onViewDetached();
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();
    }

    public void onNavDrawerHeaderClicked() {
        /*if (mDisplayedUser == null) {
            try {
                signIn();
            } catch (CouchbaseLiteException | IOException e) {
                e.printStackTrace();
                if (isViewAttached())
                    getView().displayToast(e.getMessage());
            }
        } else {
            //signOutFlow();
        }*/

        getView().displayToast("Coming very soon!");
    }

    public static class Factory<V extends DrawerView> implements PresenterFactory<DrawerPresenter<V>> {

        @Override
        public DrawerPresenter<V> create() {
            return new DrawerPresenter<>();
        }
    }
}
