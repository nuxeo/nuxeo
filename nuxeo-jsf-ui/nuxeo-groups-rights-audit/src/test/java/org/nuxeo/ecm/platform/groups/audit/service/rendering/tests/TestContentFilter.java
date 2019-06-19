/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.AcceptsAllContent;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.AcceptsGroupOnly;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.test.UserManagerFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test excel export of groups
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class, UserManagerFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("nuxeo-groups-rights-audit")
@Deploy("org.nuxeo.ecm.automation.core")
public class TestContentFilter extends AbstractAclLayoutTest {

    @Inject
    protected UserManager userManager;

    @Test
    public void testExcelExportService() throws Exception {
        // groups
        DocumentModel g1 = makeGroup(userManager, "test_g1");
        DocumentModel g2 = makeGroup(userManager, "test_g2");
        g2.setProperty("group", "subGroups", List.of("test_g1"));
        DocumentModel u1 = makeUser(userManager, "test_u1");

        // Set user properties
        u1.setProperty("user", "username", "test_u1");
        u1.setProperty("user", "firstName", "test");
        u1.setProperty("user", "lastName", "_u1");
        u1.setProperty("user", "email", "test@u1");
        // Set user/subgroup/group bindings
        u1.setProperty("user", "groups", List.of("test_g1"));
        userManager.createGroup(g1);
        userManager.createUser(u1);
        userManager.createGroup(g2);

        IContentFilter filter = new AcceptsAllContent();
        assertTrue("accepts a group", filter.acceptsUserOrGroup("test_g1"));
        assertTrue("accepts a group", filter.acceptsUserOrGroup("test_g2"));
        assertTrue("accepts a user", filter.acceptsUserOrGroup("test_u1"));

        IContentFilter filter2 = new AcceptsGroupOnly();
        assertTrue("accepts a group", filter2.acceptsUserOrGroup("test_g1"));
        assertTrue("accepts a group", filter2.acceptsUserOrGroup("test_g2"));
        assertFalse("do NOT accept a user", filter2.acceptsUserOrGroup("test_u1"));

    }
}
