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

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.work.api.Work.State.SCHEDULED;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.RunnableAclAudit;
import org.nuxeo.ecm.platform.groups.audit.service.acl.job.Work;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
@Deploy({ "org.nuxeo.ecm.platform.query.api", "nuxeo-groups-rights-audit" })
@LocalDeploy({ "nuxeo-groups-rights-audit:OSGI-INF/directory-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/schemas-config.xml",
        "nuxeo-groups-rights-audit:OSGI-INF/test-repo-repository-h2-contrib-nofulltext.xml" })
public class TestAclProcessingWork extends AbstractAclLayoutTest {
    @Inject
    CoreSession session;

    @Inject
    UserManager userManager;

    private final static Log log = LogFactory.getLog(TestAclProcessingWork.class);

    protected static File testFile = new File(folder
            + TestAclProcessingWork.class.getSimpleName() + ".xls");

    @Test
    public void testWork() throws Exception {
        // --------------------
        // Doc tree generation
        // 10k docs to have a long process
        int depth = 2;
        int width = 10;
        int groups = 1;

        log.info("Build a test repository: depth=" + depth + ", width:" + width
                + ", groups:" + groups);
        DocumentModel root = makeDocumentTree(session, depth, width, groups);
        session.save();
        log.info("done building test data");

        // --------------------
        final Work work = new Work("test-work");
        new RunnableAclAudit(session, root, work, testFile){
            @Override
            public void onAuditDone() {
                log.info("audit done");
            }
        };
        assertEquals(SCHEDULED, work.getState());

        // Go!
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        wm.schedule(work);
    }
}