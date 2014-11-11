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

package org.nuxeo.ecm.platform.oauth.providers;

import java.util.List;

/**
 * This service is used to manage OAuth Service Providers:
 * ie REST Services that can be used by Nuxeo via OAuth.
 * <p>
 * Typically, this service is used by Shindig to determine what
 * what shared secret should be used by gadgets to fetch their data.
 *
 * @author tiry
 */
public interface OAuthServiceProviderRegistry {

    /**
     * Select the best provider given.
     *
     * @param gadgetUri the gadget url (or AppId)
     * @param serviceName the service name as defined in MakeRequest
     */
    NuxeoOAuthServiceProvider getProvider(String gadgetUri,
            String serviceName);

    /**
     * This method is here for compatibility reasons.
     * Providers that are directly contributed to the OpenSocialService
     * are forwarded to the new centralized service.
     */
    NuxeoOAuthServiceProvider addReadOnlyProvider(String gadgetUri,
            String serviceName, String consumerKey, String consumerSecret,
            String publicKey);

    /**
     * Deletes a provider.
     */
    void deleteProvider(String gadgetUri, String serviceName);

    /**
     * Deletes a provider.
     */
    void deleteProvider(String providerId);

    /**
     * Return the list of all know providers
     * (both readonly and editable ones).
     */
    List<NuxeoOAuthServiceProvider> listProviders();

}