package org.nuxeo.ecm.platform.oauth.api;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;

public interface OAuthService {
  boolean verify(OAuthMessage message,
      String consumerKey);

  /**
   * Return an OAuthAccessor depending on the key informations that
   * were contributed
   * @param consumerKey
   * @return
   */
  OAuthConsumer getOAuthConsumer(String consumerKey);
}
