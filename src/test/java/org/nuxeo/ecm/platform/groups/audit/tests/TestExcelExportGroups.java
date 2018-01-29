/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.groups.audit.tests;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.groups.audit.service.ExcelExportService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
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
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/test-chain-export-operation.xml" })
public class TestExcelExportGroups {

    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    @Inject
    AutomationService automationService;

    @Test
    public void testExcelExportService() throws Exception {
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");
        List<String> g2Groups = Arrays.asList("test_g1");
        g2.setProperty("group", "subGroups", g2Groups);
        DocumentModel u1 = getUser("test_u1");
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
        ExcelExportService exportService = Framework.getService(ExcelExportService.class);
        Assert.assertTrue(exportService.getExcelReport("exportAllGroupsAudit").length() > 0);
    }

    @Test
    public void testExcelExportOperation() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Blob export = (Blob) automationService.run(ctx, "testExportExcel");
        Assert.assertTrue(export.getLength() > 0);
    }

    private DocumentModel getGroup(String groupId) throws Exception {
        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setProperty("group", "groupname", groupId);
        return newGroup;
    }

    private DocumentModel getUser(String userId) throws Exception {
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userId);
        return newUser;
    }
}
