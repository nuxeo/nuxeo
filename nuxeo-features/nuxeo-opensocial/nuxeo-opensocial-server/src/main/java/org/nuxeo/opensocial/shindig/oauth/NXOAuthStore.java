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

import org.apache.shindig.gadgets.oauth.BasicOAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerIndex;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret;

import com.google.inject.Singleton;

@Singleton
public class NXOAuthStore extends BasicOAuthStore {

    public NXOAuthStore() {
        super();
    }

    @Override
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
    }
}
