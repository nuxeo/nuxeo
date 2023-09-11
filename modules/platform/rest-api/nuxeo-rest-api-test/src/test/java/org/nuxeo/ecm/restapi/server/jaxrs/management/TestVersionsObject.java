/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ERROR_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_HAS_ERROR;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_SKIP_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_TOTAL;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.model.stream.StreamDocumentGC;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 2023
 */
@Features(AutomationFeature.class)
@WithFrameworkProperty(name = StreamDocumentGC.ENABLED_PROPERTY_NAME, value = "false")
public class TestVersionsObject extends ManagementBaseTest {

    @Inject
    protected CoreSession session;

    @Inject
    CoreFeature coreFeature;

    protected int nbVersions = 10;

    protected DocumentModel folder;

    protected DocumentModelList getVersion() {
        return session.query("select * from Document where ecm:isVersion = 1");
    }

    @Before
    public void createDocuments() throws IOException {
        DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
        ws = session.createDocument(ws);
        folder = session.createDocumentModel("/ws", "folder", "Folder");
        folder = session.createDocument(folder);

        DocumentModel doc = session.createDocumentModel("/ws/folder", "file", "File");
        doc = session.createDocument(doc);
        // create first version
        session.checkIn(doc.getRef(), VersioningOption.MINOR, null);
        // create more versions
        for (int i = 0; i < nbVersions; i++) {
            session.checkOut(doc.getRef());
            session.checkIn(doc.getRef(), VersioningOption.MINOR, null);
        }
        // also create another live doc and two versions that should not be removed
        DocumentModel doc2 = session.createDocumentModel("/ws", "doc2", "File");
        doc2 = session.createDocument(doc2);
        session.checkIn(doc2.getRef(), VersioningOption.MINOR, null);
        session.checkOut(doc2.getRef());
        session.checkIn(doc2.getRef(), VersioningOption.MINOR, null);

        // also create a proxy to a version and another version in the same history, not removed either
        DocumentModel doc3 = session.createDocumentModel("/ws", "doc3", "File");
        doc3 = session.createDocument(doc3);
        session.checkIn(doc3.getRef(), VersioningOption.MINOR, null);
        session.checkOut(doc3.getRef());
        DocumentRef doc3ver2 = session.checkIn(doc3.getRef(), VersioningOption.MINOR, null);
        session.createProxy(doc3ver2, new PathRef("/"));
        session.removeDocument(doc3.getRef()); // remove live doc, keep only proxy

        session.save();
        coreFeature.waitForAsyncCompletion();
    }

    @Test
    public void testGCOrphanVersions() throws IOException {
        DocumentModelList vs = getVersion();
        int nbTotalVersions = (nbVersions + 1) + 2 + 2;

        // all versions found
        assertEquals(nbTotalVersions, vs.size());
        doGCVersions(true, vs.size(), nbTotalVersions, 0, vs.size());

        // delete folder containing the doc
        session.removeDocument(folder.getRef());
        session.save();
        coreFeature.waitForAsyncCompletion();
        vs = getVersion();

        // all versions found
        assertEquals(nbTotalVersions, vs.size());
        doGCVersions(true, vs.size(), 2 + 2, 0, vs.size());
        vs = getVersion();

        // some versions (N+1) have been cleaned up
        assertEquals(2 + 2, vs.size());
    }

    protected void doGCVersions(boolean success, int processed, int skipped, int errorCount, int total)
            throws IOException {
        String commandId;
        try (CloseableClientResponse response = httpClientRule.delete("/management/versions/orphaned")) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());

            assertBulkStatusScheduled(node);
            commandId = getBulkCommandId(node);
        }

        // waiting for the asynchronous gc
        coreFeature.waitForAsyncCompletion();

        try (CloseableClientResponse response = httpClientRule.get("/management/bulk/" + commandId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(SC_OK, response.getStatus());

            assertBulkStatusCompleted(node);
            assertEquals(!success, node.get(STATUS_HAS_ERROR).asBoolean());
            assertEquals(processed, node.get(STATUS_PROCESSED).asInt());
            assertEquals(skipped, node.get(STATUS_SKIP_COUNT).asInt());
            assertEquals(errorCount, node.get(STATUS_ERROR_COUNT).asInt());
            assertEquals(total, node.get(STATUS_TOTAL).asInt());
        }
    }

}
