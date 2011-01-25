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

import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.OAuthModule.OAuthCrypterProvider;
import org.apache.shindig.gadgets.oauth.OAuthModule.OAuthRequestProvider;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Used to register {@link NXOAuthStoreProvider} in Shindig Stack
 *
 * @author tiry
 *
 */
public class NXOAuthModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BlobCrypter.class).annotatedWith(Names.named(OAuthFetcherConfig.OAUTH_STATE_CRYPTER)).toProvider(OAuthCrypterProvider.class);

        // Use Nuxeo Store
        bind(OAuthStore.class).toProvider(NXOAuthStoreProvider.class);

        bind(OAuthRequest.class).toProvider(OAuthRequestProvider.class);

    }

}
