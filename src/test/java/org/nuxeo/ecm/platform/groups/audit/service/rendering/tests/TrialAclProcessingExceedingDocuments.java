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

import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.groups.audit.service.acl.AclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.IAclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.ReportLayoutSettings;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessor.ProcessorStatus;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test excel export of groups
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
public class TrialAclProcessingExceedingDocuments extends AbstractAclLayoutTest {
    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    private final static Log log = LogFactory.getLog(TrialAclProcessingExceedingDocuments.class);

    protected static File testFile = new File(folder + TrialAclProcessingExceedingDocuments.class.getSimpleName()
            + ".xls");

    @Test
    @Ignore
    public void testTooManyDocuments() throws Exception {
        // edit max number of rows for the purpose of our test
        int realMaxRow = ExcelBuilder.MAX_ROW;
        ExcelBuilder.MAX_ROW = 100;

        // doc tree
        int depth = 2;
        int width = ExcelBuilder.MAX_ROW + 1;
        int groups = 1;

        log.info("Build a test repository: depth=" + depth + ", width:" + width + ", groups:" + groups);
        DocumentModel root = makeDocumentTree(session, depth, width, groups);
        session.save();
        log.info("done building test data");

        // --------------------
        // settings and filters
        ReportLayoutSettings s = AclExcelLayoutBuilder.defaultLayout();
        s.setPageSize(1000);
        IContentFilter filter = null;

        // generate XLS report
        log.info("Start audit");
        IAclExcelLayoutBuilder v = new AclExcelLayoutBuilder(s, filter);
        v.renderAudit(session, root);
        log.info("End audit");

        // save
        v.getExcel().save(testFile);
        log.info("End save");

        // reload and assert we have the expected error message
        Workbook w = v.getExcel().load(testFile);
        String txt = get(w, 1, AclExcelLayoutBuilder.STATUS_ROW, AclExcelLayoutBuilder.STATUS_COL);
        assertTrue("", txt.contains(ProcessorStatus.ERROR_TOO_MANY_DOCUMENTS.toString()));

        ExcelBuilder.MAX_ROW = realMaxRow;
    }
}
