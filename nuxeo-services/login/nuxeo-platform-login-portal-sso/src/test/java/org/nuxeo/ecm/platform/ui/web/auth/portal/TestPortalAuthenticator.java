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
 *     Florent Munch
 */
package org.nuxeo.ecm.platform.ui.web.auth.portal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.login.portal")
@Deploy("org.nuxeo.ecm.platform.login.portal:fake-pluggableauthenticationservice.xml")
public class TestPortalAuthenticator {

    private static final String SOME_TS = "1538167025822";

    private static final String SOME_RANDOM = "31415abcdef";

    private static final String SOME_USER = "bob";

    private static final String SOME_SECRET = "some_secret";

    @Test
    public void testValidateTokenWithDefaultDigestAlgorithm() {
        doTestValidateToken(Boolean.FALSE, SOME_TS, SOME_RANDOM, SOME_USER, "q0kweBvLv2/fPAuCrJkBmQ==");
        doTestValidateToken(Boolean.TRUE, SOME_TS, SOME_RANDOM, SOME_USER, "13/PMDal0Bzq3LnyICLcfQ==");
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.login.portal.test:properties-digest-md5-contrib.xml")
    public void testValidateTokenWithMd5() {
        doTestValidateToken(Boolean.FALSE, SOME_TS, SOME_RANDOM, SOME_USER, "q0kweBvLv2/fPAuCrJkBmQ==");
        doTestValidateToken(Boolean.TRUE, SOME_TS, SOME_RANDOM, SOME_USER, "13/PMDal0Bzq3LnyICLcfQ==");
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.login.portal.test:properties-digest-sha-contrib.xml")
    public void testValidateTokenWithSha() {
        doTestValidateToken(Boolean.FALSE, SOME_TS, SOME_RANDOM, SOME_USER, "w7/p23JaEIg1aUWPl/3UUnlJLio=");
        doTestValidateToken(Boolean.TRUE, SOME_TS, SOME_RANDOM, SOME_USER, "0d2699yAx8Oz8jKq3m/ntI+Nhgs=");
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.login.portal.test:properties-digest-sha256-contrib.xml")
    public void testValidateTokenWithSha256() {
        doTestValidateToken(Boolean.FALSE, SOME_TS, SOME_RANDOM, SOME_USER,
                "8SZI0nZmEThFUifkr5X0VFixaMbQIIZQuMUAFPwqYbw=");
        doTestValidateToken(Boolean.TRUE, SOME_TS, SOME_RANDOM, SOME_USER,
                "aQWWi7TPe9HOT/Hws7WfPvNuiDzgsiluVbloZWvvl2E=");
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.login.portal.test:properties-digest-sha512-contrib.xml")
    public void testValidateTokenWithSha512() {
        doTestValidateToken(Boolean.FALSE, SOME_TS, SOME_RANDOM, SOME_USER,
                "AxpXhL2+mG7DmfdE1xQ1b2LIH1HGpDqTLMGaX9B8zBhGfHHbm80u7NbhliQne4UU/Z1A2L6iuOUn+NtfTcxg7w==");
        doTestValidateToken(Boolean.TRUE, SOME_TS, SOME_RANDOM, SOME_USER,
                "yS+e7Gb/FK9cxFjPyrxXLHfm6BLtXESO0xk+lTGWbcReqXvQZq4/bKtvBoGKR8VIw4KDlehnNzD/zICnw5JCKg==");
    }

    @Test
    public void testValidateTokenWithMaxAge() {
        doTestValidateTokenWithMaxAge(Boolean.TRUE, String.valueOf(Long.MAX_VALUE));
        doTestValidateTokenWithMaxAge(Boolean.FALSE, "3600");
    }

    public void doTestValidateToken(Boolean expected, String ts, String random, String user, String token) {
        PortalAuthenticator portalAuthenticator = getPortalAuthenticator(String.valueOf(Long.MAX_VALUE));

        assertEquals(expected, portalAuthenticator.validateToken(ts, random, token, user));
    }

    public void doTestValidateTokenWithMaxAge(Boolean expected, String maxAge) {
        PortalAuthenticator portalAuthenticator = getPortalAuthenticator(maxAge);

        String ts = "0"; // in the far past
        String token = "A1A27eeSJo8bigVhWB6mMw==";
        assertEquals(expected, portalAuthenticator.validateToken(ts, SOME_RANDOM, token, SOME_USER));
    }

    private PortalAuthenticator getPortalAuthenticator(String maxAge) {
        PortalAuthenticator portalAuthenticator = new PortalAuthenticator();
        Map<String, String> params = new HashMap<>();
        params.put(PortalAuthenticator.SECRET_KEY_NAME, SOME_SECRET);
        params.put(PortalAuthenticator.MAX_AGE_KEY_NAME, maxAge);
        portalAuthenticator.initPlugin(params);

        return portalAuthenticator;
    }

}
