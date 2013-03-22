package org.nuxeo.ecm.platform.oauth2.openid.auth.google;

import org.nuxeo.ecm.platform.oauth2.openid.auth.DefaultOpenIDUserInfo;

import com.google.api.client.util.Key;

public class GoogleUserInfo extends DefaultOpenIDUserInfo {

    @Key("id")
    public String id;

    @Override
    public String getSubject() {
        return id;
    }

}
