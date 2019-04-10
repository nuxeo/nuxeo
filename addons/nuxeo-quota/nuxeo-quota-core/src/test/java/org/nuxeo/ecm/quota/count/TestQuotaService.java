/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 */
package org.nuxeo.ecm.quota.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 *
 * @since 5.7
 *
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@TransactionalConfig(autoStart = false)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.api",
        "org.nuxeo.ecm.platform.userworkspace.core",
        "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.core.event", "org.nuxeo.ecm.quota.core" })
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
        CoreSession userSession;
        DocumentRef uwRef1;
        DocumentRef uwRef2;

        assertNotNull(workManager);
        TransactionHelper.startTransaction();
        try {
            userSession = openSessionAs("jdoe");
            assertNotNull(uwm);

            DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession,
                    null);
            assertNotNull(uw);
            uwRef1 = uw.getRef();

            // check creator
            String creator = (String) uw.getProperty("dublincore", "creator");
            assertEquals(creator, "jdoe");
            CoreInstance.getInstance().close(userSession);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        TransactionHelper.startTransaction();
        try {
            userSession = openSessionAs("jack");
            uwm = Framework.getLocalService(UserWorkspaceService.class);
            assertNotNull(uwm);

            DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession,
                    null);
            assertNotNull(uw);
            uwRef2 = uw.getRef();

            // check creator
            String creator = (String) uw.getProperty("dublincore", "creator");
            assertEquals(creator, "jack");
            CoreInstance.getInstance().close(userSession);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        try {
            quotaStatsService.launchSetMaxQuotaOnUserWorkspaces(100L,
                    session.getRootDocument(), session);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        eventService.waitForAsyncCompletion();
        DocumentModel uw1 = session.getDocument(uwRef1);
        DocumentModel uw2 = session.getDocument(uwRef2);
        workManager.awaitCompletion("quota", 3, TimeUnit.SECONDS);
        assertEquals(0, workManager.getQueueSize("quota", null));
        assertTrue((Long) uw1.getPropertyValue("dss:maxSize") == 100L);
        assertTrue((Long) uw2.getPropertyValue("dss:maxSize") == 100L);
    }

    public CoreSession openSessionAs(String username) throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", username);
        return CoreInstance.getInstance().open(session.getRepositoryName(),
                context);
    }

}
