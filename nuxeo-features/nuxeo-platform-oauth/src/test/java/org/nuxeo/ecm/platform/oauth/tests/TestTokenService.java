
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(OAuthFeature.class)
public class TestTokenService {

    @Inject OAuthTokenStore tokenStore;

    @Test
    public void testServiceRW() throws Exception {

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
