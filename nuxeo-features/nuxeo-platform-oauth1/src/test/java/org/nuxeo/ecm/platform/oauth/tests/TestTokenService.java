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
@Features(OAuth1Feature.class)
public class TestTokenService {

    @Inject
    OAuthTokenStore tokenStore;

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
