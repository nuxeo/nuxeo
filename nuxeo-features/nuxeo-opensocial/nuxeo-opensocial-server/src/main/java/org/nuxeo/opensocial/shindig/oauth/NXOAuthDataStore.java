package org.nuxeo.opensocial.shindig.oauth;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;

import com.google.inject.Singleton;

@Singleton
public class NXOAuthDataStore implements OAuthDataStore {

    public void authorizeToken(OAuthEntry arg0, String arg1)
            throws OAuthProblemException {
        // TODO Auto-generated method stub

    }

    public OAuthEntry convertToAccessToken(OAuthEntry arg0)
            throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public void disableToken(OAuthEntry arg0) {
        // TODO Auto-generated method stub

    }

    public OAuthEntry generateRequestToken(String arg0, String arg1, String arg2)
            throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public OAuthConsumer getConsumer(String arg0) throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public OAuthEntry getEntry(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public SecurityToken getSecurityTokenForConsumerRequest(String arg0,
            String arg1) throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public void removeToken(OAuthEntry arg0) {
        // TODO Auto-generated method stub

    }

}
