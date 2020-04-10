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
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.ACTION_FULL_NAME;
import static org.nuxeo.ecm.core.bulk.action.SetPropertiesAction.ACTION_NAME;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.ABORTED;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;
import static org.nuxeo.ecm.core.test.DocumentSetRepositoryInit.DOC_BY_LEVEL;
import static org.nuxeo.ecm.core.test.DocumentSetRepositoryInit.USERNAME;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.bulk.computation.BulkScrollerComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DocumentSetRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
@RepositoryConfig(init = DocumentSetRepositoryInit.class)
public class TestSetPropertiesAction {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(BulkScrollerComputation.class);

    @Inject
    public BulkService service;

    @Inject
    public CoreSession session;

    @Inject
    public TransactionalFeature txFeature;

    @Test
    public void testSetPropertiesAsAdmin() throws Exception {
        testSetProperties("Administrator");
    }

    @Test
    public void testSetPropertiesAsNonAdmin() throws Exception {
        testSetProperties(USERNAME);
    }

    protected void testSetProperties(String username) throws Exception {

        DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        String nxql = String.format("SELECT * from Document where ecm:parentId='%s'", model.getId());

        String title = "test title";
        String description = "test description";
        String foo = "test foo";
        String bar = "test bar";

        HashMap<String, Serializable> complex = new HashMap<>();
        complex.put("foo", foo);
        complex.put("bar", bar);

        int oldSize = service.getStatuses(username).size();

        String commandId = service.submit(
                new BulkCommand.Builder(ACTION_NAME, nxql, username).repository(session.getRepositoryName())
                                                                    .param("dc:title", title)
                                                                    .param("dc:description", description)
                                                                    .param("cpx:complex", complex)
                                                                    .build());

        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(60)));

        BulkStatus status = service.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        assertEquals(DOC_BY_LEVEL, status.getProcessed());

        List<BulkStatus> statuses = service.getStatuses(username);
        assertEquals(1, statuses.size() - oldSize);
        assertEquals(status.getId(), statuses.get(statuses.size() - 1).getId());

        List<BulkStatus> emptyStatuses = service.getStatuses("toto");
        assertEquals(0, emptyStatuses.size());

        txFeature.nextTransaction();

        for (DocumentModel child : session.query(nxql)) {
            assertEquals(title, child.getTitle());
            assertEquals(description, child.getPropertyValue("dc:description"));
            if (child.getType().equals("ComplexDoc")) {
                assertEquals(foo, child.getPropertyValue("cpx:complex/foo"));
                assertEquals(bar, child.getPropertyValue("cpx:complex/bar"));
            }
        }
    }

    /**
     * The action must not completely fail even when setting a property fails (property not found or version not
     * writable).
     */
    @Test
    public void testSetPropertiesFailures() throws Exception {
        DocumentModel workspace = session.getDocument(new PathRef("/default-domain/workspaces/test"));
        DocumentModel doc = session.getDocument(new PathRef("/default-domain/workspaces/test/testdoc0"));
        DocumentRef verRef = session.checkIn(doc.getRef(), null, null);
        DocumentModel ver = session.getDocument(verRef);
        session.save();
        txFeature.nextTransaction();

        String nxql = String.format("SELECT * FROM Document WHERE ecm:uuid in ('%s', '%s', '%s')", //
                workspace.getId(), // no such property
                ver.getId(), // not writable
                doc.getId() // ok
        );

        int oldSize = service.getStatuses(session.getPrincipal().getName()).size();

        String commandId = service.submit(
                new BulkCommand.Builder(ACTION_NAME, nxql, session.getPrincipal().getName()).repository(
                        session.getRepositoryName()).param("cpx:complex/foo", "test foo").build());

        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(60)));

        BulkStatus status = service.getStatus(commandId);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        assertEquals(3, status.getProcessed());

        List<BulkStatus> statuses = service.getStatuses(session.getPrincipal().getName());
        assertEquals(1, statuses.size() - oldSize);
        assertEquals(status.getId(), statuses.get(statuses.size() - 1).getId());

        // docs not in error are still written though
        txFeature.nextTransaction();
        doc.refresh();
        assertEquals("test foo", doc.getPropertyValue("cpx:complex/foo"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/bulk-low-scroll-size-contrib.xml")
    public void testAbort() throws Exception {
        StreamService streamService = Framework.getService(StreamService.class);
        @SuppressWarnings("resource")
        LogManager logManager = streamService.getLogManager();
        try (LogTailer<Record> tailer = logManager.createTailer(Name.ofUrn("test"), Name.ofUrn(ACTION_FULL_NAME))) {
            tailer.toLastCommitted();

            DocumentModel model = session.getDocument(new PathRef("/default-domain/workspaces/test"));
            String nxql = String.format("SELECT * from Document where ecm:parentId='%s'", model.getId());
            String commandId = service.submit(
                    new BulkCommand.Builder(ACTION_NAME, nxql, session.getPrincipal().getName()).repository(
                            session.getRepositoryName()).param("dc:description", "foo").bucket(1).batch(1).build());
            BulkStatus abortStatus = service.abort(commandId);
            if (abortStatus.getState().equals(COMPLETED)) {
                log.warn("Bulk command cannot be aborted because already completed");
                return;
            }
            assertEquals(ABORTED, abortStatus.getState());

            BulkStatus status = service.getStatus(commandId);
            assertEquals(ABORTED, status.getState());

            assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(10)));

            status = service.getStatus(commandId);
            assertEquals(ABORTED, status.getState());

            int scrolledDocument = 0;
            while (tailer.read(Duration.ofSeconds(1)) != null) {
                scrolledDocument++;
            }
            // scroller should have aborted and so we shouldn't have scrolled everything as scrollSize = bucketSize = 1
            assertNotEquals(DOC_BY_LEVEL, scrolledDocument);
        }
    }

}
