/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.oauth.providers;

import java.util.List;

/**
 * This service is used to manage OAuth Service Providers: ie REST Services that can be used by Nuxeo via OAuth.
 * <p>
 * Typically, this service is used by Shindig to determine what what shared secret should be used by gadgets to fetch
 * their data.
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
    NuxeoOAuthServiceProvider getProvider(String gadgetUri, String serviceName);

    /**
     * This method is here for compatibility reasons. Providers that are directly contributed to the OpenSocialService
     * are forwarded to the new centralized service.
     */
    NuxeoOAuthServiceProvider addReadOnlyProvider(String gadgetUri, String serviceName, String consumerKey,
            String consumerSecret, String publicKey);

    /**
     * Deletes a provider.
     */
    void deleteProvider(String gadgetUri, String serviceName);

    /**
     * Deletes a provider.
     */
    void deleteProvider(String providerId);

    /**
     * Return the list of all know providers (both readonly and editable ones).
     */
    List<NuxeoOAuthServiceProvider> listProviders();

}
