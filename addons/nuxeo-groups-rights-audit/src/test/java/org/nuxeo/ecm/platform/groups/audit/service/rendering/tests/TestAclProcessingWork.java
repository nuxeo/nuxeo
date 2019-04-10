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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
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
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.AclAuditWork;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish.IResultPublisher;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
public class TestAclProcessingWork extends AbstractAclLayoutTest {

    private static class ResultAuditPublisher implements IResultPublisher {
        static CountDownLatch published;

        private static final long serialVersionUID = 1L;

        public ResultAuditPublisher() {
            published = new CountDownLatch(1);
        }

        @Override
        public void publish(Blob blob) {
            published.countDown();
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

    private final static Log log = LogFactory.getLog(TestAclProcessingWork.class);

    protected static File testFile = new File(folder + TestAclProcessingWork.class.getSimpleName() + ".xls");

    @Test
    public void testWork() throws Exception {
        // --------------------
        // Doc tree generation
        // 10k docs to have a long process
        int depth = 2;
        int width = 10;
        int groups = 1;

        log.debug("Build a test repository: depth=" + depth + ", width:" + width + ", groups:" + groups);
        DocumentModel root = makeDocumentTree(session, depth, width, groups);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        eventService.waitForAsyncCompletion(60 * 1000);
        log.debug("done building test data");

        // --------------------
        ResultAuditPublisher publisher = new ResultAuditPublisher();
        Work work = new AclAuditWork("test-work", session.getRepositoryName(), root.getId(), testFile, publisher);
        workManager.schedule(work, true);

        // Go!
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        if (!ResultAuditPublisher.await(1)) {
            Assert.fail("audit was not published");
        }
    }

}
