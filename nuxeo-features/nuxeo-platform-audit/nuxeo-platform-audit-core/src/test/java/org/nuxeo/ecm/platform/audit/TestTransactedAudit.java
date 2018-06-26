/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.platform.audit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestTransactedAudit {

    protected @Inject CoreSession repo;

    @Inject
    TransactionalFeature txFeature;

    public void waitForAsyncCompletion() {
        txFeature.nextTransaction(20,  TimeUnit.SECONDS);
    }

    @Before
    public void isInjected() {
        assertThat(repo, notNullValue());
    }

    @Test
    public void canLogMultipleLifecycleTransitionsInSameTx() {
        // generate events
        DocumentModel doc = repo.createDocumentModel("/", "a-file", "File");
        doc = repo.createDocument(doc);
        String initialLifeCycle = doc.getCurrentLifeCycleState();
        doc.followTransition("approve");
        String approvedLifeCycle = doc.getCurrentLifeCycleState();
        doc.followTransition("backToProject");
        String projectLifeCycle = doc.getCurrentLifeCycleState();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        waitForAsyncCompletion();

        // test audit trail
        AuditReader reader = Framework.getService(AuditReader.class);
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId(), repo.getRepositoryName());

        assertThat(trail, notNullValue());
        assertThat(trail.size(), is(3));

        boolean seenDocCreated = false;
        boolean seenDocApproved = false;
        boolean seenDocBackToProject = false;

        for (LogEntry entry : trail) {
            String lifeCycle = entry.getDocLifeCycle();
            String id = entry.getEventId();
            if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)) {
                if (initialLifeCycle.equals(lifeCycle)) {
                    seenDocCreated = true;
                }
            } else if (LifeCycleConstants.TRANSITION_EVENT.equals(id)) {
                if (projectLifeCycle.equals(lifeCycle)) {
                    seenDocBackToProject = true;
                } else if (approvedLifeCycle.equals(lifeCycle)) {
                    seenDocApproved = true;
                }
            }
        }

        assertThat(seenDocBackToProject, is(true));
        assertThat(seenDocApproved, is(true));
        assertThat(seenDocCreated, is(true));

    }

    @Test
    public void testLogDate() throws InterruptedException {
        // generate doc creation events
        DocumentModel doc = repo.createDocumentModel("/", "a-file", "File");
        doc = repo.createDocument(doc);

        // simulate a long running process in the same transaction: make the
        // delay big enough to make logDate not the same as eventDate even on
        // databases that have a 1s time resolution.
        Thread.sleep(1000);

        // commit the transaction an let the audit service log the events in the
        // log
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        waitForAsyncCompletion();

        // test audit trail
        AuditReader reader = Framework.getService(AuditReader.class);
        List<LogEntry> trail = reader.getLogEntriesFor(doc.getId(), doc.getRepositoryName());

        assertThat(trail, notNullValue());
        assertThat(trail.size(), is(1));

        Date eventDate = null;
        Date logDate = null;
        for (LogEntry entry : trail) {
            String id = entry.getEventId();
            if (DocumentEventTypes.DOCUMENT_CREATED.equals(id)) {
                eventDate = entry.getEventDate();
                logDate = entry.getLogDate();
            }
        }
        assertNotNull(eventDate);
        assertNotNull(logDate);
        assertTrue(logDate.after(eventDate));

    }
}
