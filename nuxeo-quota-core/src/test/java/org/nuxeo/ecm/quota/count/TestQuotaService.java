/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 */
package org.nuxeo.ecm.quota.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.api", "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.userworkspace.types", "org.nuxeo.ecm.core.event", "org.nuxeo.ecm.quota.core" })
public class TestQuotaService {

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    WorkManager workManager;

    @Inject
    UserWorkspaceService uwm;

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Test
    public void testSetQuotaOnUserWorkspaces() throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
        workManager.awaitCompletion(3, TimeUnit.SECONDS);
        DocumentRef uwRef1;
        DocumentRef uwRef2;

        TransactionHelper.startTransaction();
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), "jdoe")) {
            assertNotNull(uwm);
            DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession, null);
            assertNotNull(uw);
            uwRef1 = uw.getRef();

            // check creator
            String creator = (String) uw.getProperty("dublincore", "creator");
            assertEquals(creator, "jdoe");
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            workManager.awaitCompletion(3, TimeUnit.SECONDS);
        }
        TransactionHelper.startTransaction();
        try (CloseableCoreSession userSession = CoreInstance.openCoreSession(session.getRepositoryName(), "jack")) {
            uwm = Framework.getService(UserWorkspaceService.class);
            assertNotNull(uwm);

            DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession, null);
            assertNotNull(uw);
            uwRef2 = uw.getRef();

            // check creator
            String creator = (String) uw.getProperty("dublincore", "creator");
            assertEquals(creator, "jack");
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            workManager.awaitCompletion(3, TimeUnit.SECONDS);
        }
        TransactionHelper.startTransaction();
        try {
            quotaStatsService.launchSetMaxQuotaOnUserWorkspaces(100L, session.getRootDocument(), session);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            workManager.awaitCompletion(3, TimeUnit.SECONDS);
        }
        TransactionHelper.startTransaction();
        try {
            DocumentModel uw1 = session.getDocument(uwRef1);
            DocumentModel uw2 = session.getDocument(uwRef2);
            assertEquals(uw1.getProperty("dss:maxSize").getValue(Long.class), Long.valueOf(100L));
            assertEquals(uw2.getProperty("dss:maxSize").getValue(Long.class), Long.valueOf(100L));
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
            workManager.awaitCompletion(3, TimeUnit.SECONDS);
        }
    }

}
