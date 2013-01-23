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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jxls.exception.ParsePropertyException;
import net.sf.jxls.transformer.XLSTransformer;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
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

    @Test
    public void testExport() throws Exception {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        File template = getFileFromPath("templates/audit-groups-template.xls");
        List<NuxeoGroup> groups = new ArrayList<NuxeoGroup>();
        DocumentModel g1 = getGroup("test_g1");
        DocumentModel g2 = getGroup("test_g2");
        userManager.createGroup(g1);
        userManager.createGroup(g2);
        groups = userManager.getAvailableGroups();
        Map beans = new HashMap();
        beans.put("groups", groups);
        XLSTransformer transformer = new XLSTransformer();
        File resultReport = new File("audit-groups.xls");
        resultReport.createNewFile();
        try {
            transformer.transformXLS(template.getAbsolutePath(), beans,
                    resultReport.getAbsolutePath());
        } catch (ParsePropertyException e) {
        } catch (InvalidFormatException e) {
        } catch (IOException e) {
        }
    }

    private DocumentModel getGroup(String groupId) throws Exception {
        UserManager userManager = Framework.getLocalService(UserManager.class);
        DocumentModel newGroup = userManager.getBareGroupModel();
        newGroup.setProperty("group", "groupname", groupId);
        return newGroup;
    }

    private static File getFileFromPath(String path) {
        return FileUtils.getResourceFileFromContext(path);
    }
}