/*
 * (C) Copyright 2006-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */

package org.nuxeo.usermapper.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.usermapper.service.UserMapperService;
import org.nuxeo.usermapper.test.dummy.DummyUser;

import com.google.inject.Inject;

@Deploy({ "org.nuxeo.usermapper", "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.types", "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.user.center.profile", "org.nuxeo.ecm.platform.login", "org.nuxeo.ecm.platform.login.default" })
@RunWith(FeaturesRunner.class)
@LocalDeploy("org.nuxeo.usermapper:usermapper-contribs.xml")
@Features(PlatformFeature.class)
/**
 *
 * @author tiry
 *
 */
public class TestUserMapperService {

    @Inject
    CoreSession session;

    @Test
    public void shouldDeclareService() throws Exception {
        UserMapperService ums = Framework.getLocalService(UserMapperService.class);
        Assert.assertNotNull(ums);
        Assert.assertEquals(3, ums.getAvailableMappings().size());
    }

    @Test
    public void testJavaContrib() throws Exception {

        // test create
        DummyUser dm = new DummyUser("jchan", "Jacky", "Chan");
        UserMapperService ums = Framework.getLocalService(UserMapperService.class);
        NuxeoPrincipal principal = ums.getCreateOrUpdateNuxeoPrincipal("javaDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("jchan", principal.getName());
        Assert.assertEquals("Jacky", principal.getFirstName());
        Assert.assertEquals("Chan", principal.getLastName());

        // test update
        dm = new DummyUser("jchan", null, "Chan2");
        principal = ums.getCreateOrUpdateNuxeoPrincipal("javaDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("jchan", principal.getName());
        Assert.assertEquals("Jacky", principal.getFirstName());
        Assert.assertEquals("Chan2", principal.getLastName());

    }

    @Test
    public void testGroovyContrib() throws Exception {

        // test create
        DummyUser dm = new DummyUser("bharper", "Ben", "Harper");
        UserMapperService ums = Framework.getLocalService(UserMapperService.class);
        NuxeoPrincipal principal = ums.getCreateOrUpdateNuxeoPrincipal("groovyDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("bharper", principal.getName());
        Assert.assertEquals("Ben", principal.getFirstName());
        Assert.assertEquals("Harper", principal.getLastName());

        dm = new DummyUser("bharper", "Bill", "Harper");
        principal = ums.getCreateOrUpdateNuxeoPrincipal("groovyDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("bharper", principal.getName());
        Assert.assertEquals("Bill", principal.getFirstName());
        Assert.assertEquals("Harper", principal.getLastName());

        UserProfileService ups = Framework.getService(UserProfileService.class);
        DocumentModel profile = ups.getUserProfileDocument("bharper", session);
        Assert.assertEquals("555.666.7777", profile.getPropertyValue("userprofile:phonenumber"));

    }

    @Test
    public void testNashornContrib() throws Exception {

        // test create
        DummyUser dm = new DummyUser("bharper", "Ben", "Harper");
        UserMapperService ums = Framework.getLocalService(UserMapperService.class);
        NuxeoPrincipal principal = ums.getCreateOrUpdateNuxeoPrincipal("jsDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("bharper", principal.getName());
        Assert.assertEquals("Ben", principal.getFirstName());
        Assert.assertEquals("Harper", principal.getLastName());

        dm = new DummyUser("bharper", "Bill", "Harper");
        principal = ums.getCreateOrUpdateNuxeoPrincipal("jsDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("bharper", principal.getName());
        Assert.assertEquals("Bill", principal.getFirstName());
        Assert.assertEquals("Harper", principal.getLastName());

        UserProfileService ups = Framework.getService(UserProfileService.class);
        DocumentModel profile = ups.getUserProfileDocument("bharper", session);
        Assert.assertEquals("555.666.7777", profile.getPropertyValue("userprofile:phonenumber"));

    }

}
