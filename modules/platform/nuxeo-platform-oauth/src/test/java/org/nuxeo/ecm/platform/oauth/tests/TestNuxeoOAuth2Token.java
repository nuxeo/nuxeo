/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;

/**
 * Tests {@link NuxeoOAuth2Token}.
 *
 * @since 11.1
 */
public class TestNuxeoOAuth2Token {

    @Test
    public void testToJsonObject() {
        NuxeoOAuth2Token token = new NuxeoOAuth2Token("myAccessToken", "myRefreshToken", 2000L);
        Map<String, Object> json = token.toJsonObject();
        assertEquals("myAccessToken", json.get("access_token"));
        assertEquals("myRefreshToken", json.get("refresh_token"));
        assertEquals("bearer", json.get("token_type"));
        Object expiresIn = json.get("expires_in");
        assertTrue(expiresIn instanceof Long);
        assertTrue((long) expiresIn <= 2);
    }

}
