package org.nuxeo.ecm.platform.oauth2.openid.auth;

public interface OpenIDUserInfoStore {
    void storeUserInfo(String userId, OpenIDUserInfo userInfo);
    OpenIDUserInfo getUserInfo(String nuxeoLogin);
    String getNuxeoLogin(OpenIDUserInfo userInfo);
}
