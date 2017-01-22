/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.auth.portal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestPortalAuthenticator {

    private static final String SOME_SECRET = "some_secret";

    @Test
    public void testValidateToken() {
        doTestValidateTokenWithMaxAge(Boolean.TRUE, String.valueOf(Long.MAX_VALUE));
        doTestValidateTokenWithMaxAge(Boolean.FALSE, "3600");
    }

    public void doTestValidateTokenWithMaxAge(Boolean expected, String maxAge) {
        PortalAuthenticator portalAuthenticator = new PortalAuthenticator();
        Map<String, String> params = new HashMap<>();
        params.put(PortalAuthenticator.SECRET_KEY_NAME, SOME_SECRET);
        params.put(PortalAuthenticator.MAX_AGE_KEY_NAME, maxAge);
        portalAuthenticator.initPlugin(params);

        String ts = "0"; // in the far past
        String random = "31415abcdef";
        String userName = "bob";
        String token = "A1A27eeSJo8bigVhWB6mMw==";
        assertEquals(expected, portalAuthenticator.validateToken(ts, random, token, userName));
    }

}
