/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.services;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;
import org.apache.shindig.gadgets.oauth.OAuthModule;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.apache.shindig.gadgets.oauth.OAuthModule.OAuthCrypterProvider;
import org.apache.shindig.gadgets.oauth.OAuthModule.OAuthStoreProvider;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;
import org.nuxeo.opensocial.shindig.crypto.NXBlobCrypterSecurityTokenDecoder;
import org.nuxeo.opensocial.shindig.oauth.NuxeoOAuthRequest;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Names;

public class NuxeoCryptoModule extends AbstractModule {

    private static final Log LOG = LogFactory.getLog(NuxeoCryptoModule.class);

    @Override
    protected final void configure() {
        try {
            bind(SecurityTokenDecoder.class).to(
                    NXBlobCrypterSecurityTokenDecoder.class);
            bind(OAuthDataStore.class).to(FakeNuxeoDataStore.class);

            // this is kinda horrible but would require some nasty changes in
            // the injector creation to fix... these two bindings are copied
            // from the OAuthModule...
            // Used for encrypting client-side OAuth state.
            bind(BlobCrypter.class).annotatedWith(
                    Names.named(OAuthFetcherConfig.OAUTH_STATE_CRYPTER)).toProvider(
                    OAuthCrypterProvider.class);

            // Used for persistent storage of OAuth access tokens.
            bind(OAuthStore.class).toProvider(OAuthStoreProvider.class);

            // this is the one we are trying to override
            bind(OAuthRequest.class).toProvider(NuxeoOAuthRequestProvider.class);
        } catch (Exception e) {
            LOG.error("Unable to bind Shindig services to Nuxeo components");
            LOG.error(e.getMessage());
        }
    }
}

class NuxeoOAuthRequestProvider extends OAuthModule.OAuthRequestProvider {
    private final HttpFetcher fetcher;

    private final OAuthFetcherConfig config;

    @Inject
    public NuxeoOAuthRequestProvider(HttpFetcher fetcher,
            OAuthFetcherConfig config) {
        super(null, null);
        this.fetcher = fetcher;
        this.config = config;
    }

    @Override
    public OAuthRequest get() {
        return new NuxeoOAuthRequest(config, fetcher);
    }
}

// for the provider side, which we dont deal with right now

class FakeNuxeoDataStore implements OAuthDataStore {

    public void authorizeToken(OAuthEntry arg0, String arg1)
            throws OAuthProblemException {
        // TODO Auto-generated method stub

    }

    public OAuthEntry convertToAccessToken(OAuthEntry arg0)
            throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public void disableToken(OAuthEntry arg0) {
        // TODO Auto-generated method stub

    }

    public OAuthEntry generateRequestToken(String arg0, String arg1, String arg2)
            throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public OAuthConsumer getConsumer(String arg0) throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public OAuthEntry getEntry(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public SecurityToken getSecurityTokenForConsumerRequest(String arg0,
            String arg1) throws OAuthProblemException {
        // TODO Auto-generated method stub
        return null;
    }

    public void removeToken(OAuthEntry arg0) {
        // TODO Auto-generated method stub

    }

}