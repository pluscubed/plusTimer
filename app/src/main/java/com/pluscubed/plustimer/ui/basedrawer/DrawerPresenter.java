package com.pluscubed.plustimer.ui.basedrawer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.auth0.api.authentication.AuthenticationAPIClient;
import com.auth0.api.callback.BaseCallback;
import com.auth0.core.Auth0;
import com.auth0.core.Token;
import com.auth0.core.UserProfile;
import com.auth0.identity.IdentityProvider;
import com.auth0.identity.IdentityProviderCallback;
import com.auth0.identity.WebIdentityProvider;
import com.auth0.identity.web.CallbackParser;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.replicator.Replication;
import com.pluscubed.plustimer.App;
import com.pluscubed.plustimer.R;
import com.pluscubed.plustimer.base.Presenter;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class DrawerPresenter<V extends DrawerView> extends Presenter<V> {

    private WebIdentityProvider mProvider;

    public DrawerPresenter() {
    }

    @Override
    public void onViewAttached(V view) {
        super.onViewAttached(view);
    }

    private void signIn() {
        if (mProvider == null) {
            //getView().getContextCompat().startActivity(new Intent(getView().getContextCompat(), LockActivity.class));

            String clientId = getView().getContextCompat().getString(R.string.auth0_client_id);
            String domain = getView().getContextCompat().getString(R.string.auth0_domain_name);
            Auth0 auth0 = new Auth0(clientId, domain);
            AuthenticationAPIClient client = auth0.newAuthenticationAPIClient();
            mProvider = new WebIdentityProvider(
                    new CallbackParser(),
                    auth0.getClientId(),
                    auth0.getAuthorizeUrl()
            );

            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("scope", "openid offline_access");
            parameters.put("device", Build.MODEL);
            mProvider.setParameters(parameters);
            mProvider.setCallback(new IdentityProviderCallback() {
                @Override
                public void onFailure(Dialog dialog) {
                }

                @Override
                public void onFailure(int titleResource, int messageResource, Throwable cause) {
                }

                @Override
                public void onSuccess(String serviceName, String accessToken) {
                }

                @Override
                public void onSuccess(Token token) {
                    client.tokenInfo(token.getIdToken())
                            .start(new BaseCallback<UserProfile>() {
                                @Override
                                public void onSuccess(UserProfile payload) {
                                    Toast.makeText(getView().getContextCompat(), "profile acquired: " + payload.getName(), Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Throwable error) {

                                }
                            });

                    try {
                        URL url = new URL("http://192.168.1.4:5984/test");
                        Replication push = App.getDatabase(getView().getContextCompat()).createPushReplication(url);
                        Replication pull = App.getDatabase(getView().getContextCompat()).createPullReplication(url);
                        pull.setContinuous(true);
                        push.setContinuous(true);
                        HashMap<String, Object> requestHeadersParam = new HashMap<>();
                        requestHeadersParam.put("Authorization", "Bearer " + token.getIdToken());
                        push.setHeaders(requestHeadersParam);
                        push.start();
                        pull.start();
                    } catch (CouchbaseLiteException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            mProvider.start(getView().getContextCompat(), "WCA");
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mProvider.authorize(getView().getContextCompat(), requestCode, resultCode, data);
    }

    public void onNewIntent(Intent intent) {
        mProvider.authorize(getView().getContextCompat(), IdentityProvider.WEBVIEW_AUTH_REQUEST_CODE, Activity.RESULT_OK, intent);
    }

    @Override
    public void onViewDetached() {
        super.onViewDetached();
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();
    }
}
