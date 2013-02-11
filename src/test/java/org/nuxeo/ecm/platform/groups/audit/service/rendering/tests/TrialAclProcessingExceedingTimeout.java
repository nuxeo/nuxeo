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
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.groups.audit.service.acl.AclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.IAclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.ReportLayoutSettings;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessor.ProcessorStatus;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessorPaginated;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.IExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.filter.IContentFilter;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.Work;
import org.nuxeo.ecm.platform.test.PlatformFeature;
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
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/test-chain-export-operation.xml" })
public class TrialAclProcessingExceedingTimeout extends AbstractAclLayoutTest {
    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    private final static Log log = LogFactory.getLog(TrialAclProcessingExceedingTimeout.class);

    protected static File testFile = new File(folder
            + TrialAclProcessingExceedingTimeout.class.getSimpleName() + ".xls");

    @Test
    public void testTimeout() throws Exception {
        // --------------------
        // Doc tree generation
        // 10k docs to have a long process
        int depth = 5;
        int width = 10;
        int groups = 1;

        log.info("Build a test repository: depth=" + depth + ", width:" + width
                + ", groups:" + groups);
        makeDocumentTree(session, depth, width, groups);
        session.save();
        log.info("done building test data");

        // --------------------
        // worker wrapping
        String wname = "test-process-too-long";
        int testTimeout = DataProcessorPaginated.EXCEL_RENDERING_RESERVED_TIME + 45;// s
        final Work work = new Work(wname, testTimeout) {
            // ACTUAL TEST HERE: VERIFY TIMEOUT WARNING APPEARS IN SPREA
            public void afterRun(boolean ok) {
                super.afterRun(ok);
                System.out.println("done afterrun");
                try {
                    assertProcessInterruptStatusInOutputFile();
                } catch (InvalidFormatException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // --------------------
        // work to do
        Runnable todo = new Runnable() {
            @Override
            public void run() {
                // setup
                ReportLayoutSettings s = AclExcelLayoutBuilder.defaultLayout();
                s.setPageSize(1000);
                IContentFilter filter = null;

                // generate XLS report
                log.info("Start audit");
                IAclExcelLayoutBuilder v = new AclExcelLayoutBuilder(s, filter);
                try {
                    v.renderAudit(session, session.getRootDocument(), true,
                            work);
                    log.info("End audit");
                } catch (ClientException e) {
                    throw new RuntimeException(e);
                }

                // save
                try {
                    v.getExcel().save(testFile);
                    log.info("End save");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // Go!
        work.setRunnable(todo);
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        wm.schedule(work);
    }

    protected void assertProcessInterruptStatusInOutputFile() throws InvalidFormatException, IOException {
        // reload and assert we have the expected error message
        IExcelBuilder v = new ExcelBuilder();
        Workbook workbook = v.load(testFile);
        String txt = get(workbook, 1, AclExcelLayoutBuilder.STATUS_ROW,
                AclExcelLayoutBuilder.STATUS_COL);
        assertTrue("",
                txt.contains(ProcessorStatus.ERROR_TOO_LONG_PROCESS.toString()));
    }
}