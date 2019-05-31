/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.oauth2.openid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.login.openid.test")
public class TestOpenIDProviders {

    @Inject
    protected OpenIDConnectProviderRegistry registry;

    @Test
    public void verifyServiceRegistration() {

        assertNotNull(registry);

        OpenIDConnectProvider provider = registry.getProvider("TestingGoogleOpenIDConnect");
        assertNotNull(provider);

        assertTrue(OpenIDConnectHelper.getEnabledProviders().size() > 0);

        OpenIDConnectProvider provider2 = registry.getProvider("TestingGoogleOpenIDConnect2");
        assertNotNull(provider2);

        // check provider's authenticationMethod
        provider = registry.getProvider("MY_NAME");
        assertNotNull(provider);
        assertEquals("MY_AUTHENTICATION_METHOD", provider.authenticationMethod);
    }

}
