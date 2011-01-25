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

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.gadgets.GadgetSpecFactory;
import org.apache.shindig.gadgets.oauth.AccessorInfo;
import org.apache.shindig.gadgets.oauth.AccessorInfoBuilder;
import org.apache.shindig.gadgets.oauth.GadgetOAuthTokenStore;
import org.apache.shindig.gadgets.oauth.OAuthArguments;
import org.apache.shindig.gadgets.oauth.OAuthClientState;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;
import org.apache.shindig.gadgets.oauth.OAuthResponseParams;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.OAuthResponseParams.OAuthRequestException;
import org.apache.shindig.gadgets.oauth.OAuthStore.ConsumerInfo;
import org.nuxeo.ecm.platform.oauth.keys.OAuthServerKeyManager;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;

/**
 * Override the default GadgetOAuthTokenStore to add management for
 * Shindig to Nuxeo local communication.
 *
 * Basically if no OAuth config was found for a local request, we use
 * the shared key between Shindig and Nuxeo to do Sign Fetch
 *
 * @author tiry
 *
 */
public class NXGadgetOAuthTokenStore extends GadgetOAuthTokenStore {

    protected OpenSocialService os;

    protected OAuthServerKeyManager okm;

    @Inject
    public NXGadgetOAuthTokenStore(OAuthStore store,
            GadgetSpecFactory specFactory) {
        super(store, specFactory);
        os = Framework.getLocalService(OpenSocialService.class);
        okm = Framework.getLocalService(OAuthServerKeyManager.class);
    }

    protected boolean isConsumerEmpty(ConsumerInfo consumerInfo) {
        if (consumerInfo==null) {
            return true;
        }
        if (consumerInfo.getConsumer()==null) {
            return true;
        }
        if (consumerInfo.getConsumer().consumerKey==null) {
            return true;
        }
        return false;
    }

    protected boolean isInternalRequest(OAuthArguments arguments) {
        // At this point we don't have the information about the requestURI
        // ==> we have to rely on a flag that is set by the caller
        return arguments.getRequestOption(NuxeoOAuthRequest.NUXEO_INTERNAL_REQUEST, "false").equals("true");
    }

    @Override
    public AccessorInfo getOAuthAccessor(SecurityToken securityToken,
            OAuthArguments arguments, OAuthClientState clientState, OAuthResponseParams responseParams,
            OAuthFetcherConfig fetcherConfig)
            throws OAuthRequestException {

        AccessorInfo accessorInfo = super.getOAuthAccessor(securityToken, arguments, clientState, responseParams, fetcherConfig);

        if (isConsumerEmpty(accessorInfo.getConsumer()) &&
                isInternalRequest(arguments) &&
                !os.propagateJSESSIONIDToTrustedHosts()) {
            AccessorInfoBuilder accessorBuilder = new AccessorInfoBuilder();
            accessorBuilder.setParameterLocation(AccessorInfo.OAuthParamLocation.URI_QUERY);

            String callBack = os.getOAuthCallbackUrl();

            String consumerKey=okm.getInternalKey();
            String secret=okm.getInternalSecret();

            OAuthConsumer consumer = new OAuthConsumer(callBack, consumerKey, secret, null);
            consumer.setProperty(OAuth.OAUTH_SIGNATURE_METHOD, OAuth.HMAC_SHA1);

            ConsumerInfo ci = new ConsumerInfo(consumer, arguments.getServiceName(), callBack);
            accessorBuilder.setConsumer(ci);

            return accessorBuilder.create(responseParams);
        }

        return accessorInfo;
    }
}
