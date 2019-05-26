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

package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
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

/**
 * This test asserts that an audit exceeding its dedicated transaction time will be able to cleanly exit and indicate an
 * error status in the output excel file.
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
@ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreLongRunning.class)
public class TestAclProcessingExceedingTimeout extends AbstractAclLayoutTest {

    private static class AssertProcessInterruptStatusInOutputFile implements IResultPublisher {
        private static final long serialVersionUID = 1L;

        private static CountDownLatch published = new CountDownLatch(1);

        private static boolean failed;

        AssertProcessInterruptStatusInOutputFile() {
            published = new CountDownLatch(1);
        }

        @Override
        public void publish(Blob blob) {
            // verify
            try {
                assertProcessInterruptStatusInOutputFile();
            } catch (InvalidFormatException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        protected void assertProcessInterruptStatusInOutputFile() throws InvalidFormatException, IOException {
            // reload and assert we have the expected error message
            try {
                IExcelBuilder v = new ExcelBuilder();
                Workbook workbook = v.load(testFile);
                String txt = get(workbook, 1, AclExcelLayoutBuilder.STATUS_ROW, AclExcelLayoutBuilder.STATUS_COL);
                failed = txt == null || txt.contains(ProcessorStatus.ERROR_TOO_LONG_PROCESS.toString());
            } finally {
                published.countDown();
            }
        }

        private static boolean await(int delay) throws InterruptedException {
            return published.await(delay, TimeUnit.MINUTES);
        }
    }

    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    @Inject
    EventService eventService;

    @Inject
    WorkManager workManager;

    private final static Log log = LogFactory.getLog(TestAclProcessingExceedingTimeout.class);

    protected static File testFile = new File(
            folder + TestAclProcessingExceedingTimeout.class.getSimpleName() + ".xls");

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
        TransactionHelper.startTransaction();
        // cancel lots of fulltext work
        eventService.waitForAsyncCompletion(60 * 1000); // 1min
        log.debug("done initial async work");

        // --------------------
        // worker wrapping
        String wname = "test-process-too-long";
        int testTimeout = DataProcessorPaginated.EXCEL_RENDERING_RESERVED_TIME + 1;// s

        AssertProcessInterruptStatusInOutputFile publisher = new AssertProcessInterruptStatusInOutputFile();
        Work work = new AclAuditWork(wname, session.getRepositoryName(), root.getId(), testFile, publisher,
                testTimeout);

        workManager.schedule(work, true);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        AssertProcessInterruptStatusInOutputFile.await(2);
        Assertions.assertThat(AssertProcessInterruptStatusInOutputFile.failed).isFalse();
    }

}
