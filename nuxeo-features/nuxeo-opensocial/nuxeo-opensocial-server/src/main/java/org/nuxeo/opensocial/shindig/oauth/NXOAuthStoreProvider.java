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

import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.BasicOAuthStoreConsumerKeyAndSecret.KeyType;
import org.nuxeo.ecm.platform.oauth.keys.OAuthServerKeyManager;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * This class is used to plug Nuxeo OAuth Store into Shindig
 *
 * @author tiry
 *
 */
@Singleton
public class NXOAuthStoreProvider implements Provider<OAuthStore>{

    protected NXOAuthStore store;

    public NXOAuthStoreProvider() {

        store = new NXOAuthStore();
        OAuthServerKeyManager skm = Framework.getLocalService(OAuthServerKeyManager.class);
        OpenSocialService os = Framework.getLocalService(OpenSocialService.class);
        store.setDefaultCallbackUrl(os.getOAuthCallbackUrl());
        String privateKey = skm.getBarePrivateKey();
        String signingKeyName = skm.getKeyName();
        BasicOAuthStoreConsumerKeyAndSecret key = new BasicOAuthStoreConsumerKeyAndSecret(null, privateKey, KeyType.RSA_PRIVATE,signingKeyName, null);
        store.setDefaultKey(key);

        // XXX load entries from OpenSocial Service config

    }

    @Override
    public OAuthStore get() {
        return store;
    }

}