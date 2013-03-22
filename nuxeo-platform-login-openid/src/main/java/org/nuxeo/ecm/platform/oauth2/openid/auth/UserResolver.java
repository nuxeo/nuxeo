package org.nuxeo.ecm.platform.oauth2.openid.auth;

import org.nuxeo.ecm.platform.oauth2.openid.OpenIDConnectProvider;

public abstract class UserResolver {

    private OpenIDConnectProvider provider;

    UserResolver(OpenIDConnectProvider provider) {
        this.provider = provider;
    }

    OpenIDConnectProvider getProvider() {
        return provider;
    }

    public abstract String findNuxeoUser(OpenIDUserInfo userInfo);
}