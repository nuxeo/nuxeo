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

import java.io.File;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml" })
public class TestAclProcessingWork extends AbstractAclLayoutTest {
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
        eventService.waitForAsyncCompletion(60 * 1000);
        log.debug("done building test data");

        // --------------------
        IResultPublisher publisher = new IResultPublisher() {
            private static final long serialVersionUID = 1L;

            @Override
            public void publish(Blob blob) throws ClientException {
                log.debug("audit done");
            }
        };
        Work work = new AclAuditWork("test-work", session.getRepositoryName(), root.getId(), testFile, publisher);

        // Go!
        workManager.schedule(work, true);

        eventService.waitForAsyncCompletion(60 * 1000);
        TransactionHelper.startTransaction();
    }

}
