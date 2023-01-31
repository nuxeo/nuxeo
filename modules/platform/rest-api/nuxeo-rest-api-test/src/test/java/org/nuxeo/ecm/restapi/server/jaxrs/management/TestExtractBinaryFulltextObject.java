/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.CoreSession.BINARY_FULLTEXT_MAIN_KEY;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_HAS_ERROR;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.restapi.test.ManagementBaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 2021.33
 */
@Features(AutomationFeature.class)
public class TestExtractBinaryFulltextObject extends ManagementBaseTest {

    @Inject
    protected CoreSession session;

    @Inject
    protected ThumbnailService thumbnailService;

    protected DocumentRef docRef;

    protected FulltextConfiguration fulltextConfiguration;

    @Before
    public void createDocument() throws IOException {
        // get the fulltext conf
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Repository repository = repositoryService.getRepository(session.getRepositoryName());
        fulltextConfiguration = repository.getFulltextConfiguration();
        // create some docs
        DocumentModel file = session.createDocumentModel("/", "file", "File");
        BlobHolder holder = file.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob("You know for search"));
        file = session.createDocument(file);
        // create a version
        DocumentRef ver = session.checkIn(file.getRef(), VersioningOption.MINOR, null);
        // create a proxy (not re-indexed)
        session.createProxy(ver, session.getRootDocument().getRef());
        DocumentModel file2 = session.createDocumentModel(null, "file2", "File");
        session.createDocument(file2);
        session.save();
        txFeature.nextTransaction();
        docRef = file.getRef();
    }


    @Test
    public void testRunExtractFulltext() throws IOException {
        // default fulltext conf, extraction is done
        var doc = session.getDocument(docRef);
        var ft = doc.getBinaryFulltext();
        assertTrue(ft.toString(), ft.get(BINARY_FULLTEXT_MAIN_KEY).contains("You know for search"));

        // exclude File type, no more binary fulltext, thanks to the force option
        fulltextConfiguration.excludedTypes.add("File");
        runExtractFulltext();
        session.save();
        ft = doc.getBinaryFulltext();
        assertTrue(ft.toString(), ft.get(BINARY_FULLTEXT_MAIN_KEY).isBlank());

        // restore conf and run extraction again
        fulltextConfiguration.excludedTypes.clear();
        runExtractFulltext();
        ft = doc.getBinaryFulltext();
        assertFalse(ft.isEmpty());
        assertTrue(ft.toString(), ft.get(BINARY_FULLTEXT_MAIN_KEY).contains("You know for search"));
    }

    protected void runExtractFulltext() throws IOException {
        String commandId;
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("force", "true");
        try (CloseableClientResponse response = httpClientRule.post("/management/fulltext/extract", formData)) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertBulkStatusScheduled(node);
            commandId = getBulkCommandId(node);
        }
        txFeature.nextTransaction();
        try (CloseableClientResponse response = httpClientRule.get("/management/bulk/" + commandId)) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(SC_OK, response.getStatus());
            assertBulkStatusCompleted(node);
            assertEquals(false, node.get(STATUS_HAS_ERROR).asBoolean());
        }
    }

}
