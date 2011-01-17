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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.oauth.BasicOAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.nuxeo.ecm.platform.oauth.providers.NuxeoOAuthServiceProvider;
import org.nuxeo.ecm.platform.oauth.providers.OAuthServiceProviderRegistry;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Singleton;

@Singleton
public class NXOAuthStore extends BasicOAuthStore {

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
            secret=null; // Tmp Hack
            KeyType kt = KeyType.HMAC_SYMMETRIC;
            String name = serviceName;
            if (secret==null) {
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
            // Can not transmis the provider because urls may be not set ...
            //OAuthConsumer consumer = new OAuthConsumer(callBack, consumerKey, secret, sp);
            return new ConsumerInfo(consumer, serviceName, callBack);
        }

/*          BasicOAuthStoreConsumerIndex pk = new BasicOAuthStoreConsumerIndex();
          pk.setGadgetUri(securityToken.getAppUrl());
          pk.setServiceName(serviceName);
          BasicOAuthStoreConsumerKeyAndSecret cks = consumerInfos.get(pk);
          if (cks == null) {
            cks = defaultKey;
          }
          if (cks == null) {
            throw new GadgetException(GadgetException.Code.INTERNAL_SERVER_ERROR,
                "No key for gadget " + securityToken.getAppUrl() + " and service " + serviceName);
          }
          OAuthConsumer consumer = null;
          if (cks.getKeyType() == KeyType.RSA_PRIVATE) {
            consumer = new OAuthConsumer(null, cks.getConsumerKey(), null, provider);
            // The oauth.net java code has lots of magic.  By setting this property here, code thousands
            // of lines away knows that the consumerSecret value in the consumer should be treated as
            // an RSA private key and not an HMAC key.
            consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.RSA_SHA1);
            consumer.setProperty(RSA_SHA1.PRIVATE_KEY, cks.getConsumerSecret());
          } else {
            consumer = new OAuthConsumer(null, cks.getConsumerKey(), cks.getConsumerSecret(), provider);
            consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);
          }
          String callback = (cks.getCallbackUrl() != null ? cks.getCallbackUrl() : defaultCallbackUrl);
          return new ConsumerInfo(consumer, cks.getKeyName(), callback);*/


        }

/*    @Override
    public void setConsumerKeyAndSecret(
            BasicOAuthStoreConsumerIndex providerKey,
            BasicOAuthStoreConsumerKeyAndSecret keyAndSecret) {

        String consumerKey = keyAndSecret.getConsumerKey();
        if (consumerKey == null) {
            consumerKey = keyAndSecret.getKeyName();
        }
        BasicOAuthStoreConsumerKeyAndSecret kas = new BasicOAuthStoreConsumerKeyAndSecret(
                consumerKey, keyAndSecret.getConsumerSecret(),
                keyAndSecret.getKeyType(), null, keyAndSecret.getCallbackUrl());

        super.setConsumerKeyAndSecret(providerKey, kas);
    }*/
}
