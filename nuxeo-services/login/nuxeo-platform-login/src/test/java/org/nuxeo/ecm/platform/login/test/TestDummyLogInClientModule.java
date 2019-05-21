/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     mhilaire
 */
package org.nuxeo.ecm.platform.login.test;

import java.security.Principal;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test on the {@link DummyNuxeoLoginModule}
 *
 * @since 6.0
 * @deprecated since 11.1, no replacement
 */
@RunWith(FeaturesRunner.class)
@Features(ClientLoginFeature.class)
@Deprecated(since = "11.1")
public class TestDummyLogInClientModule {

    @Inject
    ClientLoginFeature login;

    @Test
    public void canBuildDummyPrincipal() throws LoginException {
        Principal dummyPrincipal = login.login("dummyName");
        Assert.assertNotNull(dummyPrincipal);
    }

    @Test
    public void canGetCurrentDummyPrincipal() throws LoginException {
        Principal dummyPrincipal = login.login("dummyName");
        NuxeoPrincipal currentDummy = ClientLoginModule.getCurrentPrincipal();
        Assert.assertEquals(dummyPrincipal.getName(), currentDummy.getName());
    }

}
