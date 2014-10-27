/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mhilaire
 */
package org.nuxeo.ecm.platform.login.test;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature.User;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test on the {@link TrustLoginModule}
 *
 * @since 5.9.6
 */
@RunWith(FeaturesRunner.class)
@Features(ClientLoginFeature.class)
@User(name = "dummyName")
public class TestTrustedLogInClientModule {

    @Inject
    ClientLoginFeature login;

    @Test
    @User(name = "pfouh", administrator = true, anonymous = false, groups = { "administrators" })
    public void canGetCurrentDummyPrincipal() throws LoginException {
        NuxeoPrincipal identity = ClientLoginModule.getCurrentPrincipal();
        Assert.assertEquals("pfouh", identity.getName());
        Assert.assertThat(identity.isAdministrator(), Matchers.is(true));
        Assert.assertThat(identity.getGroups().get(0),
                Matchers.is("administrators"));
    }

}