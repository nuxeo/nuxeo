package org.nuxeo.opensocial.services;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;

public class NXOAuthDataStore implements OAuthDataStore {

  private static final String OAUTH_NOT_IMPLEMENTED = "OAuth not implemented";

  public void authorizeToken(OAuthEntry arg0, String arg1)
      throws OAuthProblemException {
    throw new OAuthProblemException(OAUTH_NOT_IMPLEMENTED);

  }

  public OAuthEntry convertToAccessToken(OAuthEntry arg0)
      throws OAuthProblemException {
    throw new OAuthProblemException(OAUTH_NOT_IMPLEMENTED);
  }

  public void disableToken(OAuthEntry arg0) {
    // TODO Auto-generated method stub

  }

  public OAuthEntry generateRequestToken(String arg0, String arg1, String arg2)
      throws OAuthProblemException {
    throw new OAuthProblemException(OAUTH_NOT_IMPLEMENTED);
  }

  public OAuthConsumer getConsumer(String arg0) throws OAuthProblemException {
    throw new OAuthProblemException(OAUTH_NOT_IMPLEMENTED);
  }

  public OAuthEntry getEntry(String arg0) {
    return null;
  }

  public SecurityToken getSecurityTokenForConsumerRequest(String arg0,
      String arg1) throws OAuthProblemException {
    throw new OAuthProblemException(OAUTH_NOT_IMPLEMENTED);
  }

  public void removeToken(OAuthEntry arg0) {
    // TODO Auto-generated method stub

  }

}
