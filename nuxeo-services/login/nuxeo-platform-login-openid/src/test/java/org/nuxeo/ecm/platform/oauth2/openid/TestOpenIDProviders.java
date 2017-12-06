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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.api",
        "org.nuxeo.ecm.platform.login.openid.test" })
public class TestOpenIDProviders {

    @Test
    public void verifyServiceRegistration() {

        OpenIDConnectProviderRegistry registry = Framework.getService(OpenIDConnectProviderRegistry.class);
        Assert.assertNotNull(registry);

        OpenIDConnectProvider provider = registry.getProvider("TestingGoogleOpenIDConnect");
        Assert.assertNotNull(provider);

        Assert.assertTrue(OpenIDConnectHelper.getEnabledProviders().size() > 0);

        OpenIDConnectProvider provider2 = registry.getProvider("TestingGoogleOpenIDConnect2");
        Assert.assertNotNull(provider2);

    }

}
