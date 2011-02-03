/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.consumers;

import java.util.List;


/**
 * Service interface for managing OAuth Service Consumers
 *
 * @author tiry
 *
 */
public interface OAuthConsumerRegistry {

    /**
     * Get a Consumer from it's consumerKey
     * @param consumerKey
     * @return
     */
    NuxeoOAuthConsumer getConsumer(String consumerKey);


    /**
     * Get a Consumer from it's consumerKey
     * The keyType param indicate if we need HMAC or RSA secret
     * This is needed because the default OAuthValidator implementation only uses 1 field for both Keys
     * If keyType is OAUth.RSA_SHA1, the consumerSecret field will be polupated with the RSA public key rather than the HMAC secret.
     *
     * @param consumerKey
     * @return
     */
    NuxeoOAuthConsumer getConsumer(String consumerKey, String keyType);

    /**
     * remove a Consumer
     * @param consumerKey
     */
    void deleteConsumer(String consumerKey);

    /**
     * List all registred Consumers
     *
     * @return
     */
    List<NuxeoOAuthConsumer> listConsumers();

    /**
     * Store a new Consumer
     *
     * @param consumer
     * @return
     * @throws Exception
     */
    NuxeoOAuthConsumer storeConsumer(NuxeoOAuthConsumer consumer) throws Exception;
}
