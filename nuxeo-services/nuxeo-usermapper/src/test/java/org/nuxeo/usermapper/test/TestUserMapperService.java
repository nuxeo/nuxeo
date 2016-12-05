/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.usermapper.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.usermapper.service.UserMapperService;
import org.nuxeo.usermapper.test.dummy.DummyUser;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(UserMapperFeature.class)
@LocalDeploy("org.nuxeo.usermapper:usermapper-contribs.xml")
@Deploy({"org.nuxeo.ecm.platform.userworkspace.api",
    "org.nuxeo.ecm.platform.userworkspace.types", "org.nuxeo.ecm.platform.userworkspace.core",
"org.nuxeo.ecm.user.center.profile"})
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
        NuxeoPrincipal principal = ums.getOrCreateAndUpdateNuxeoPrincipal("javaDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("jchan", principal.getName());
        Assert.assertEquals("Jacky", principal.getFirstName());
        Assert.assertEquals("Chan", principal.getLastName());

        // test update
        dm = new DummyUser("jchan", null, "Chan2");
        principal = ums.getOrCreateAndUpdateNuxeoPrincipal("javaDummy", dm);
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
        NuxeoPrincipal principal = ums.getOrCreateAndUpdateNuxeoPrincipal("groovyDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("bharper", principal.getName());
        Assert.assertEquals("Ben", principal.getFirstName());
        Assert.assertEquals("Harper", principal.getLastName());

        dm = new DummyUser("bharper", "Bill", "Harper");
        principal = ums.getOrCreateAndUpdateNuxeoPrincipal("groovyDummy", dm);
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
        NuxeoPrincipal principal = ums.getOrCreateAndUpdateNuxeoPrincipal("jsDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("bharper", principal.getName());
        Assert.assertEquals("Ben", principal.getFirstName());
        Assert.assertEquals("Harper", principal.getLastName());

        dm = new DummyUser("bharper", "Bill", "Harper");
        principal = ums.getOrCreateAndUpdateNuxeoPrincipal("jsDummy", dm);
        Assert.assertNotNull(principal);
        Assert.assertEquals("bharper", principal.getName());
        Assert.assertEquals("Bill", principal.getFirstName());
        Assert.assertEquals("Harper", principal.getLastName());

        UserProfileService ups = Framework.getService(UserProfileService.class);
        DocumentModel profile = ups.getUserProfileDocument("bharper", session);
        Assert.assertEquals("555.666.7777", profile.getPropertyValue("userprofile:phonenumber"));

    }

}
