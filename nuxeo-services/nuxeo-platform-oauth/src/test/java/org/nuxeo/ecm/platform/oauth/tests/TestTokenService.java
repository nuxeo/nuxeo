
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
package org.nuxeo.ecm.platform.oauth.tests;

import org.nuxeo.ecm.platform.oauth.tokens.OAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestTokenService extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.directory.api");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        deployBundle("org.nuxeo.ecm.platform.oauth");
        deployContrib("org.nuxeo.ecm.platform.oauth.test", "OSGI-INF/directory-test-config.xml");
    }

    public void testServiceLookup() throws Exception {
        OAuthTokenStore tokenStore = Framework.getLocalService(OAuthTokenStore.class);
        assertNotNull(tokenStore);
    }

    public void testServiceRW() throws Exception {

        OAuthTokenStore tokenStore = Framework.getLocalService(OAuthTokenStore.class);

        OAuthToken rToken = tokenStore.createRequestToken("toto", null);

        assertNotNull(tokenStore.getRequestToken(rToken.getToken()));

        rToken = tokenStore.addVerifierToRequestToken(rToken.getToken(), new Long(0));

        assertNotNull(rToken.getVerifier());

        OAuthToken aToken = tokenStore.createAccessTokenFromRequestToken(rToken);
        assertNotNull(aToken);
        assertNull(tokenStore.getRequestToken(rToken.getToken()));

        assertNotNull(tokenStore.getAccessToken(aToken.getToken()));

        assertFalse(aToken.getToken().equals(rToken.getToken()));

        tokenStore.removeAccessToken(aToken.getToken());

        assertNull(tokenStore.getAccessToken(aToken.getToken()));




    }

}
