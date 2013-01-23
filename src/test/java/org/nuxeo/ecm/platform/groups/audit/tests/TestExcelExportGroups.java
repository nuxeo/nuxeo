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

package org.nuxeo.ecm.platform.groups.audit.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import net.sf.jxls.exception.ParsePropertyException;
import net.sf.jxls.transformer.XLSTransformer;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Test excel export of groups
 * 
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core", "org.nuxeo.ecm.core.api",
        "org.nuxeo.runtime.management", "org.nuxeo.ecm.directory.api",
        "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql",
        "org.nuxeo.ecm.platform.usermanager",
        "org.nuxeo.ecm.platform.usermanager.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
public class TestExcelExportGroups {

    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    @Test
    public void testExport() throws Exception {
        File template = getFileFromPath("templates/audit-groups-template.xls");
        List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>();
        List<String> groupsId = new ArrayList<String>();
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");
        List<String> g2Groups = Arrays.asList("test_g1");
        g2.setProperty("group", "subGroups", g2Groups);
        DocumentModel u1 = getUser("test_u1");
        DocumentModel u2 = getUser("test_u2");
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
        groupsId = userManager.getGroupIds();
        for (String groupId : groupsId) {
            NuxeoGroup group = userManager.getGroup(groupId);
            groups.add(group);
            for (String userId : group.getMemberUsers()) {
                NuxeoPrincipal user = userManager.getPrincipal(userId);
            }
        }
        Map beans = new HashMap();
        beans.put("groups", groups);
        beans.put("userManager", userManager);
        XLSTransformer transformer = new XLSTransformer();
        File resultReport = new File("audit-groups.xls");
        resultReport.createNewFile();
        Assert.assertEquals(resultReport.length(), 0);
        try {
            transformer.transformXLS(template.getAbsolutePath(), beans,
                    resultReport.getAbsolutePath());
            Assert.assertTrue(resultReport.length() > 0);
            resultReport.delete();
        } catch (ParsePropertyException e) {
        } catch (InvalidFormatException e) {
        } catch (IOException e) {
        }
    }

    private DocumentModel getGroup(String groupId) throws Exception {
        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setProperty("group", "groupname", groupId);
        return newGroup;
    }

    private static File getFileFromPath(String path) {
        return FileUtils.getResourceFileFromContext(path);
    }

    private DocumentModel getUser(String userId) throws Exception {
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", userId);
        return newUser;
    }
}