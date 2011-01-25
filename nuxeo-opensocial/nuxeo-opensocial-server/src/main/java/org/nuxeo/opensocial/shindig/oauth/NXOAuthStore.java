/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.shindig.oauth;

import net.oauth.OAuth;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import net.oauth.signature.RSA_SHA1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.BasicOAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.nuxeo.ecm.platform.oauth.providers.NuxeoOAuthServiceProvider;
import org.nuxeo.ecm.platform.oauth.providers.OAuthServiceProviderRegistry;
import org.nuxeo.ecm.platform.oauth.tokens.NuxeoOAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStore;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Singleton;

/**
 * Forwards calls for OAuth Token storage to Nuxeo OAuth Services
 *
 * @author tiry
 *
 */
@Singleton
public class NXOAuthStore extends BasicOAuthStore {

    protected static final Log log = LogFactory.getLog(NXOAuthStore.class);

    protected String nxDefaultCallBackUrl;
    protected BasicOAuthStoreConsumerKeyAndSecret nxDefaultKey;

    public NXOAuthStore() {
        super();
    }

    @Override
    public void setDefaultKey(BasicOAuthStoreConsumerKeyAndSecret defaultKey) {
        nxDefaultKey = defaultKey;
        super.setDefaultKey(defaultKey);
    }

    @Override
    public void setDefaultCallbackUrl(String defaultCallbackUrl) {
        nxDefaultCallBackUrl = defaultCallbackUrl;
        super.setDefaultCallbackUrl(defaultCallbackUrl);
    }

    @Override
    public ConsumerInfo getConsumerKeyAndSecret(
            SecurityToken securityToken, String serviceName, OAuthServiceProvider provider)
            throws GadgetException {

        OAuthServiceProviderRegistry spr = Framework.getLocalService(OAuthServiceProviderRegistry.class);
        OpenSocialService os = Framework.getLocalService(OpenSocialService.class);

        NuxeoOAuthServiceProvider sp = spr.getProvider(securityToken.getAppUrl(), serviceName);

        if (sp==null) {
            
            return super.getConsumerKeyAndSecret(securityToken, serviceName, provider);
        } else {
            String consumerKey = sp.getConsumerKey();
            String secret = sp.getConsumerSecret();
            KeyType kt = KeyType.HMAC_SYMMETRIC;
            String name = serviceName;
            if (secret==null || "".equals(secret.trim())) {
                // should be moved to OAuth service
                secret = nxDefaultKey.getConsumerSecret();
                kt = KeyType.RSA_PRIVATE;
                name = nxDefaultKey.getKeyName();
            }
            String callBack = nxDefaultCallBackUrl;

            OAuthConsumer consumer = new OAuthConsumer(callBack, consumerKey, secret, provider);
            if (kt == KeyType.RSA_PRIVATE) {
                // The oauth.net java code has lots of magic.  By setting this property here, code thousands
                // of lines away knows that the consumerSecret value in the consumer should be treated as
                // an RSA private key and not an HMAC key.
                consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.RSA_SHA1);
                consumer.setProperty(RSA_SHA1.PRIVATE_KEY, secret);
              } else {
                consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
            }
            // Can not transmit the provider because urls may be not set ...
            //OAuthConsumer consumer = new OAuthConsumer(callBack, consumerKey, secret, sp);
            return new ConsumerInfo(consumer, serviceName, callBack);
        }

    }


    @Override
    public TokenInfo getTokenInfo(SecurityToken securityToken, ConsumerInfo consumerInfo,
            String serviceName, String tokenName) {

        OAuthTokenStore nxStore = Framework.getLocalService(OAuthTokenStore.class);

        String appId = securityToken.getAppId();
        String owner = securityToken.getOwnerId();

        try {
            NuxeoOAuthToken nxToken = nxStore.getClientAccessToken(appId, owner);
            if (nxToken!=null) {
                String accessToken = nxToken.getToken();
                String tokenSecret = nxToken.getTokenSecret();
                String sessionHandle = null;
                long tokenExpireMillis=0;
                TokenInfo tokenInfo = new TokenInfo(accessToken, tokenSecret, sessionHandle, tokenExpireMillis);
                return tokenInfo;
            }
        } catch (Exception e) {
            log.error("Error while try to get Client Token from store", e);
        }
        TokenInfo tokenInfo =  super.getTokenInfo(securityToken, consumerInfo, serviceName, tokenName);
        return tokenInfo;
    }

    @Override
    public void setTokenInfo(SecurityToken securityToken, ConsumerInfo consumerInfo,
            String serviceName, String tokenName, TokenInfo tokenInfo) {

        OAuthTokenStore nxStore = Framework.getLocalService(OAuthTokenStore.class);

        String consumerKey = consumerInfo.getConsumer().consumerKey;
        String callBack = consumerInfo.getConsumer().callbackURL;
        String appId = securityToken.getAppId();
        String owner = securityToken.getOwnerId();

        String token = tokenInfo.getAccessToken();
        String tokenSecret = tokenInfo.getTokenSecret();
        nxStore.storeClientAccessToken(consumerKey, callBack, token, tokenSecret, appId, owner);

        super.setTokenInfo(securityToken, consumerInfo, serviceName, tokenName, tokenInfo);
    }

    @Override
    public void removeToken(SecurityToken securityToken, ConsumerInfo consumerInfo,
            String serviceName, String tokenName) {

        OAuthTokenStore nxStore = Framework.getLocalService(OAuthTokenStore.class);

        String appId = securityToken.getAppId();
        String owner = securityToken.getOwnerId();


        super.removeToken(securityToken, consumerInfo, serviceName, tokenName);
    }

}
