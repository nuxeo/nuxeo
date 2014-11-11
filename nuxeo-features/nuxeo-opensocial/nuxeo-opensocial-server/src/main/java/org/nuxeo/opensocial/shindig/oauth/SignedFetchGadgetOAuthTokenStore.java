/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.shindig.oauth;

import net.oauth.OAuthServiceProvider;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.AccessorInfo;
import org.apache.shindig.gadgets.oauth.AccessorInfoBuilder;
import org.apache.shindig.gadgets.oauth.GadgetOAuthTokenStore;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth.OAuthClientState;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.OAuthStore.ConsumerInfo;
import org.apache.shindig.gadgets.oauth.OAuthStore.TokenInfo;

import com.google.inject.Inject;

public class SignedFetchGadgetOAuthTokenStore extends GadgetOAuthTokenStore {
  private final OAuthStore store;


  /**
   * Public constructor.
   *
   * @param store an {@link OAuthStore} that can store and retrieve OAuth
   *              tokens, as well as information about service providers.
   */
  @Inject
  public SignedFetchGadgetOAuthTokenStore(OAuthStore store) {
    super(store, null);
    this.store = store;

  }

  /**
   * Retrieve an AccessorInfo and OAuthAccessor that are ready for signing OAuthMessages.  To do
   * this, we need to figure out:
   *
   * - what consumer key/secret to use for signing.
   * - if an access token should be used for the request, and if so what it is.   *
   * - the OAuth request/authorization/access URLs.
   * - what HTTP method to use for request token and access token requests
   * - where the OAuth parameters are located.
   *
   * Note that most of that work gets skipped for signed fetch, we just look up the consumer key
   * and secret for that.  Signed fetch always sticks the parameters in the query string.
   */
  @Override
  public AccessorInfo getOAuthAccessor(SecurityToken securityToken,
      OAuthArguments arguments, OAuthClientState clientState) throws GadgetException {

    AccessorInfoBuilder accessorBuilder = new AccessorInfoBuilder();

    // Does the gadget spec tell us any details about the service provider, like where to put the
    // OAuth parameters and what methods to use for their URLs?
    OAuthServiceProvider provider = null;

    // This is plain old signed fetch.
    accessorBuilder.setParameterLocation(AccessorInfo.OAuthParamLocation.URI_QUERY);

    // What consumer key/secret should we use?
    ConsumerInfo consumer = store.getConsumerKeyAndSecret(
        securityToken, arguments.getServiceName(), provider);
    accessorBuilder.setConsumer(consumer);

    // Should we use the OAuth access token?  We never do this unless the client allows it, and
    // if owner == viewer.
    if (arguments.mayUseToken()
        && securityToken.getOwnerId() != null
        && securityToken.getViewerId().equals(securityToken.getOwnerId())) {
      lookupToken(securityToken, consumer, arguments, clientState, accessorBuilder);
    }

    return accessorBuilder.create();
  }


  /**
   * Figure out the OAuth token that should be used with this request.  We check for this in three
   * places.  In order of priority:
   *
   * 1) From information we cached on the client.
   *    We encrypt the token and cache on the client for performance.
   *
   * 2) From information we have in our persistent state.
   *    We persist the token server-side so we can look it up if necessary.
   *
   * 3) From information the gadget developer tells us to use (a preapproved request token.)
   *    Gadgets can be initialized with preapproved request tokens.  If the user tells the service
   *    provider they want to add a gadget to a gadget container site, the service provider can
   *    create a preapproved request token for that site and pass it to the gadget as a user
   *    preference.
   * @throws GadgetException
   */
  private void lookupToken(SecurityToken securityToken, ConsumerInfo consumerInfo,
      OAuthArguments arguments, OAuthClientState clientState, AccessorInfoBuilder accessorBuilder)
      throws GadgetException {
    if (clientState.getRequestToken() != null) {
      // We cached the request token on the client.
      accessorBuilder.setRequestToken(clientState.getRequestToken());
      accessorBuilder.setTokenSecret(clientState.getRequestTokenSecret());
    } else if (clientState.getAccessToken() != null) {
      // We cached the access token on the client
      accessorBuilder.setAccessToken(clientState.getAccessToken());
      accessorBuilder.setTokenSecret(clientState.getAccessTokenSecret());
      accessorBuilder.setSessionHandle(clientState.getSessionHandle());
      accessorBuilder.setTokenExpireMillis(clientState.getTokenExpireMillis());
    } else {
      // No useful client-side state, check persistent storage
      TokenInfo tokenInfo = store.getTokenInfo(securityToken, consumerInfo,
          arguments.getServiceName(), arguments.getTokenName());
      if (tokenInfo != null && tokenInfo.getAccessToken() != null) {
        // We have an access token in persistent storage, use that.
        accessorBuilder.setAccessToken(tokenInfo.getAccessToken());
        accessorBuilder.setTokenSecret(tokenInfo.getTokenSecret());
        accessorBuilder.setSessionHandle(tokenInfo.getSessionHandle());
        accessorBuilder.setTokenExpireMillis(tokenInfo.getTokenExpireMillis());
      } else {
        // We don't have an access token yet, but the client sent us a (hopefully) preapproved
        // request token.
        accessorBuilder.setRequestToken(arguments.getRequestToken());
        accessorBuilder.setTokenSecret(arguments.getRequestTokenSecret());
      }
    }
  }



}
