package org.nuxeo.ecm.platform.oauth.tokens;

import java.util.List;

public interface OAuthTokenStore {

    // Request token

    OAuthToken createRequestToken(String consumerKey, String callBack);

    OAuthToken addVerifierToRequestToken(String token);

    OAuthToken getRequestToken(String token);

    void removeRequestToken(String token);


    // Access token

    OAuthToken createAccessTokenFromRequestToken(OAuthToken requestToken);

    OAuthToken getAccessToken(String token);

    void removeAccessToken(String token) throws Exception;

    List<OAuthToken> listAccessTokenForUser(String login);

    List<OAuthToken> listAccessTokenForConsumer(String consumerKey);

}
