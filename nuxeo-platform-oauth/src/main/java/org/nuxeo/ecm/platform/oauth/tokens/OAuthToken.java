package org.nuxeo.ecm.platform.oauth.tokens;

import java.util.Calendar;

public interface OAuthToken {

    public static enum Type {
        REQUEST, ACCESS
    }

    String getAppId();

    String getCallbackUrl();

    String getNuxeoLogin();

    String getToken();

    String getTokenSecret();

    boolean isAuthorized();

    String getConsumerKey();

    Type getType();

    Calendar getCreationDate();

    String getValue(String keyName);

    void setValue(String keyName, String value);

    String getVerifier();

    boolean isExpired();

    void setNuxeoLogin(String login);
}
