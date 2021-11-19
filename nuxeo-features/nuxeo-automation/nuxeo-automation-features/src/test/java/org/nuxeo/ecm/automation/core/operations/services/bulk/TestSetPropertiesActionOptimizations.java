/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.automation.core.operations.services.bulk;

import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.VersioningOption.NONE;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.ACTION_NAME;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_DISABLE_AUDIT;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.PARAM_VERSIONING_OPTION;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test audit and/or auto versioning disablement.
 * <p>
 * It's done at this level to have those features loaded.
 *
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, AuditFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
public class TestSetPropertiesActionOptimizations {

    @Inject
    public CoreSession session;

    @Inject
    public BulkService bulkService;

    @Inject
    public NXAuditEventsService auditService;

    @Inject
    public TransactionalFeature transactions;

    @Inject
    public VersioningService versioningService;

    @Inject
    public EventService eventService;

    @Inject
    public Logs logs;

    protected DocumentModel note;

    @Before
    public void before() throws InterruptedException {
        DocumentModel workspace = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        DocumentModel note = session.createDocumentModel(workspace.getPathAsString(), "note", "Note");
        this.note = session.createDocument(note);
        transactions.nextTransaction();
    }

    @Test
    public void testDisableAudit() throws InterruptedException {

        // make sure all previous logs have been bulked and processed
        logs.await(60, TimeUnit.SECONDS);

        int initialLogEntries = logs.getLogEntriesFor(note.getId(), session.getRepositoryName()).size();

        bulkService.submit(createBuilder().build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        assertTrue("Logs didn't finish", logs.await(60, TimeUnit.SECONDS));
        // documentCheckin and documentModified
        assertEquals(initialLogEntries + 2, logs.getLogEntriesFor(note.getId(), session.getRepositoryName()).size());

        bulkService.submit(createBuilder().param(PARAM_DISABLE_AUDIT, TRUE).build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        assertTrue("Logs didn't finish", logs.await(60, TimeUnit.SECONDS));
        // only documentCheckin
        assertEquals(initialLogEntries + 3, logs.getLogEntriesFor(note.getId(), session.getRepositoryName()).size());

        bulkService.submit(createBuilder().param(PARAM_VERSIONING_OPTION, NONE.toString()).build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        assertTrue("Logs didn't finish", logs.await(60, TimeUnit.SECONDS));
        // only documentModified
        assertEquals(initialLogEntries + 4, logs.getLogEntriesFor(note.getId(), session.getRepositoryName()).size());

        bulkService.submit(createBuilder().param(PARAM_DISABLE_AUDIT, TRUE)
                                          .param(PARAM_VERSIONING_OPTION, NONE.toString())
                                          .build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        assertTrue("Logs didn't finish", logs.await(60, TimeUnit.SECONDS));
        // none
        assertEquals(initialLogEntries + 4, logs.getLogEntriesFor(note.getId(), session.getRepositoryName()).size());
    }

    @Test
    public void testDisableVersioning() throws InterruptedException {
        assertEquals(1, session.getVersions(note.getRef()).size());
        // should not create a new version
        bulkService.submit(createBuilder().param(PARAM_VERSIONING_OPTION, NONE.toString()).build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        transactions.nextTransaction();
        assertEquals(1, session.getVersions(note.getRef()).size());
        // should create a new version
        bulkService.submit(createBuilder().build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        transactions.nextTransaction();
        assertEquals(2, session.getVersions(note.getRef()).size());
    }

    // NXP-30700
    @Test
    @Deploy("org.nuxeo.ecm.automation.features.tests:test-bulk-disable-automatic-versioning-contrib.xml")
    public void testDisableAutomaticVersioning() throws InterruptedException {
        // the note is automatically checked in after update due to the default policy
        assertEquals(1, session.getVersions(note.getRef()).size());
        // check out the note in order to trigger the automatic versioning engine during the before to update stage
        note.checkOut();
        transactions.nextTransaction();
        // should not create a new version
        bulkService.submit(createBuilder().param(PARAM_VERSIONING_OPTION, NONE.toString()).build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        transactions.nextTransaction();
        assertEquals(1, session.getVersions(note.getRef()).size());
        // should create a new version
        bulkService.submit(createBuilder().build());
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        transactions.nextTransaction();
        assertEquals(3, session.getVersions(note.getRef()).size());
    }

    protected BulkCommand.Builder createBuilder() {
        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * FROM Note where ecm:parentId='%s'", model.getId());
        return new BulkCommand.Builder(ACTION_NAME, nxql).repository(session.getRepositoryName())
                                                         .user(session.getPrincipal().getName())
                                                         .param("dc:title", UUID.randomUUID().toString());
    }

}
