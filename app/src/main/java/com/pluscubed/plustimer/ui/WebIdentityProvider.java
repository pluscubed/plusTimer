package com.pluscubed.plustimer.ui;

import android.app.Activity;

import com.auth0.identity.web.CallbackParser;

public class WebIdentityProvider extends com.auth0.identity.WebIdentityProvider {
    public WebIdentityProvider(CallbackParser parser, String clientId, String authorizeUrl) {
        super(parser, clientId, authorizeUrl);
    }

    @Override
    public void start(Activity activity, String serviceName) {
        super.start(activity, serviceName);
    }
}
