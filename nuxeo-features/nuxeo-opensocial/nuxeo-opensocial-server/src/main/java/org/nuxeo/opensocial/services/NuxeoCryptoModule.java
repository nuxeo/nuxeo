/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 */

package org.nuxeo.opensocial.services;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthProblemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.auth.SecurityTokenDecoder;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.apache.shindig.social.opensocial.oauth.OAuthEntry;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.opensocial.shindig.crypto.NXBlobCrypterSecurityTokenDecoder;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class NuxeoCryptoModule extends AbstractModule {

    private static final Log LOG = LogFactory.getLog(NuxeoCryptoModule.class);

    @Override
    protected final void configure() {
        try {
            bind(SecurityTokenDecoder.class).to(
                    NXBlobCrypterSecurityTokenDecoder.class);
            bind(OAuthDataStore.class).to(FakeNuxeoDataStore.class);

            // These are here because they are no longer supplied in the
            // open social properties file but rather are computed from
            // a contribution to an extension point.
            String signingKeyPath = Framework.getService(
                    OpenSocialService.class).getSigningStateKeyFile().getPath();
            String privateKeyPath = Framework.getService(
                    OpenSocialService.class).getOAuthPrivateKeyFile().getPath();
            String privateKeyName = Framework.getService(
                    OpenSocialService.class).getOAuthPrivateKeyName();
            String callbackUrl = Framework.getService(OpenSocialService.class).getOAuthCallbackUrl();

            bind(String.class).annotatedWith(
                    Names.named("shindig.signing.state-key")).toInstance(
                    signingKeyPath);
            bind(String.class).annotatedWith(
                    Names.named("shindig.signing.key-file")).toInstance(
                    privateKeyPath);
            bind(String.class).annotatedWith(
                    Names.named("shindig.signing.key-name")).toInstance(
                    privateKeyName);
            bind(String.class).annotatedWith(
                    Names.named("shindig.signing.global-callback-url")).toInstance(
                    callbackUrl);
        } catch (Exception e) {
            LOG.error("Unable to bind Shindig services to Nuxeo components", e);
        }
    }
}

// for the provider side, which we do not deal with right now

class FakeNuxeoDataStore implements OAuthDataStore {

    public void authorizeToken(OAuthEntry arg0, String arg1)
            throws OAuthProblemException {
    }

    public OAuthEntry convertToAccessToken(OAuthEntry arg0)
            throws OAuthProblemException {
        return null;
    }

    public void disableToken(OAuthEntry arg0) {
    }

    public OAuthEntry generateRequestToken(String arg0, String arg1, String arg2)
            throws OAuthProblemException {
        return null;
    }

    public OAuthConsumer getConsumer(String arg0) throws OAuthProblemException {
        return null;
    }

    public OAuthEntry getEntry(String arg0) {
        return null;
    }

    public SecurityToken getSecurityTokenForConsumerRequest(String arg0,
            String arg1) throws OAuthProblemException {
        return null;
    }

    public void removeToken(OAuthEntry arg0) {
    }

}
