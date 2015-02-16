/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.AcceptsAllContent;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.AcceptsGroupOnly;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test excel export of groups
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api", "org.nuxeo.runtime.management",
        "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.ecm.platform.usermanager", "org.nuxeo.ecm.platform.usermanager.api", "nuxeo-groups-rights-audit",
        "org.nuxeo.ecm.automation.core" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
public class TestContentFilter extends AbstractAclLayoutTest {

    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    @Inject
    AutomationService automationService;

    @Test
    public void testExcelExportService() throws Exception {
        // groups
        DocumentModel g1 = makeGroup(userManager, "test_g1");
        DocumentModel g2 = makeGroup(userManager, "test_g2");
        List<String> g2Groups = Arrays.asList("test_g1");
        g2.setProperty("group", "subGroups", g2Groups);
        DocumentModel u1 = makeUser(userManager, "test_u1");

        // Set user properties
        u1.setProperty("user", "username", "test_u1");
        u1.setProperty("user", "firstName", "test");
        u1.setProperty("user", "lastName", "_u1");
        u1.setProperty("user", "email", "test@u1");
        // Set user/subgroup/group bindings
        u1.setProperty("user", "groups", Arrays.asList("test_g1"));
        userManager.createUser(u1);
        userManager.createGroup(g1);
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