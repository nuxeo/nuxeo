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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.groups.audit.service.acl.AclExcelLayoutBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessor.ProcessorStatus;
import org.nuxeo.ecm.platform.groups.audit.service.acl.data.DataProcessorPaginated;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.IExcelBuilder;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.AclAuditWork;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish.IResultPublisher;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * This test asserts that an audit exceeding its dedicated transaction time will be able to cleanly exit and indicate an
 * error status in the output excel file.
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
@ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreLongRunning.class)
public class TestAclProcessingExceedingTimeout extends AbstractAclLayoutTest {

    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    @Inject
    EventService eventService;

    @Inject
    WorkManager workManager;

    private final static Log log = LogFactory.getLog(TestAclProcessingExceedingTimeout.class);

    protected static File testFile = new File(folder + TestAclProcessingExceedingTimeout.class.getSimpleName() + ".xls");

    @Test
    public void testTimeout() throws Exception {
        // --------------------
        // Doc tree generation
        // Many docs to have a long process
        int depth = 5;
        int width = 10;
        int groups = 1;

        log.debug("Build a test repository: depth=" + depth + ", width:" + width + ", groups:" + groups);
        DocumentModel root = makeDocumentTree(session, depth, width, groups);
        session.save();
        log.debug("done building test data");
        TransactionHelper.commitOrRollbackTransaction();
        // cancel lots of fulltext work
        eventService.waitForAsyncCompletion(60 * 1000); // 1min
        log.debug("done initial async work");

        // --------------------
        // worker wrapping
        String wname = "test-process-too-long";
        int testTimeout = DataProcessorPaginated.EXCEL_RENDERING_RESERVED_TIME + 1;// s

        IResultPublisher publisher = new IResultPublisher() {
            private static final long serialVersionUID = 1L;

            @Override
            public void publish(Blob blob) throws ClientException {
                // verify
                try {
                    assertProcessInterruptStatusInOutputFile();
                } catch (InvalidFormatException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Work work = new AclAuditWork(wname, session.getRepositoryName(), root.getId(), testFile, publisher, testTimeout);

        // Go!
        workManager.schedule(work, true);

        eventService.waitForAsyncCompletion(2 * 60 * 1000);
        TransactionHelper.startTransaction();
    }

    protected void assertProcessInterruptStatusInOutputFile() throws InvalidFormatException, IOException {
        // reload and assert we have the expected error message
        IExcelBuilder v = new ExcelBuilder();
        Workbook workbook = v.load(testFile);
        String txt = get(workbook, 1, AclExcelLayoutBuilder.STATUS_ROW, AclExcelLayoutBuilder.STATUS_COL);
        if (txt != null) {
            assertTrue("assert we found an error message",
                    txt.contains(ProcessorStatus.ERROR_TOO_LONG_PROCESS.toString()));
        } else {
            fail("no text string available at expected cell");
        }
    }
}