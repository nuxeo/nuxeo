/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.consumers;

import java.util.List;

/**
 * Service interface for managing OAuth Service Consumers
 *
 * @author tiry
 */
public interface OAuthConsumerRegistry {

    /**
     * Get a Consumer from its consumerKey.
     */
    NuxeoOAuthConsumer getConsumer(String consumerKey);

    /**
     * Get a Consumer from its consumerKey.
     * <p>
     * The keyType param indicates if we need HMAC or RSA secret. This is needed because the default OAuthValidator
     * implementation only uses 1 field for both Keys. If keyType is OAUth.RSA_SHA1, the consumerSecret field will be
     * polupated with the RSA public key rather than the HMAC secret.
     */
    NuxeoOAuthConsumer getConsumer(String consumerKey, String keyType);

    /**
     * remove a Consumer
     */
    void deleteConsumer(String consumerKey);

    /**
     * List all registered Consumers
     */
    List<NuxeoOAuthConsumer> listConsumers();

    /**
     * Store a new Consumer
     */
    NuxeoOAuthConsumer storeConsumer(NuxeoOAuthConsumer consumer);
}
