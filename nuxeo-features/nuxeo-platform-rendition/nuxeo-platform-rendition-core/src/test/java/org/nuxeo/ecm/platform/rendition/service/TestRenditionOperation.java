/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.platform.rendition.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.rendition.Constants;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.operation.GetRendition;
import org.nuxeo.ecm.platform.rendition.operation.PublishRendition;
import org.nuxeo.ecm.platform.rendition.operation.UnpublishAll;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestRenditionOperation {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected RenditionService renditionService;

    @Test
    public void shouldGetPDFRendition() throws OperationException {
        DocumentModel file = createDummyFile();

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(file);
        Map<String, Object> params = new HashMap<>();
        params.put("renditionName", "pdf");

        Blob renditionBlob = (Blob) automationService.run(ctx, GetRendition.ID, params);
        assertNotNull(renditionBlob);
        assertEquals("application/pdf", renditionBlob.getMimeType());
        assertEquals("dummy.pdf", renditionBlob.getFilename());

        // do it again in order to check the cached blob
        ctx = new OperationContext(session);
        ctx.setInput(file);
        renditionBlob = (Blob) automationService.run(ctx, GetRendition.ID, params);
        assertNotNull(renditionBlob);
        assertEquals("application/pdf", renditionBlob.getMimeType());
        assertEquals("dummy.pdf", renditionBlob.getFilename());

        Rendition pdfRendition = renditionService.getRendition(file, "pdf");
        assertEquals(renditionBlob.getLength(), pdfRendition.getBlob().getLength());
    }

    /**
     * @since 10.3
     */
    @Test
    public void shouldPublishPDFRendition() throws OperationException {
        DocumentModel file = createDummyFile();
        DocumentModel section = session.createDocumentModel("/", "section", "Section");
        section = session.createDocument(section);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(file);
        Map<String, Object> params = new HashMap<>();
        params.put("renditionName", "pdf");
        params.put("target", section);
        DocumentModel publishedRendition = (DocumentModel) automationService.run(ctx, PublishRendition.ID, params);
        assertNotNull(publishedRendition);
        assertTrue(publishedRendition.isProxy());
        assertEquals(section.getRef(), publishedRendition.getParentRef());
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        List<DocumentModel> versions = session.getVersions(file.getRef());
        assertNotNull(versions);
        assertEquals(1, versions.size());
        List<DocumentModel> retrievedPublished = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:isProxy = 1 AND rend:sourceVersionableId = '%s'", file.getId()));
        assertEquals(1, retrievedPublished.size());
        assertEquals(publishedRendition.getId(), retrievedPublished.get(0).getId());
    }

    /**
     * @since 10.3
     */
    @Test
    public void shouldPublishDefaultRendition() throws OperationException {
        DocumentModel file = createDummyFile();
        DocumentModel section = session.createDocumentModel("/", "section", "Section");
        section = session.createDocument(section);

        DocumentModel publishedRendition;
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            Map<String, Object> params = new HashMap<>();
            params.put("target", section);
            params.put("defaultRendition", true);
            publishedRendition = (DocumentModel) automationService.run(ctx, PublishRendition.ID, params);
        }
        assertNotNull(publishedRendition);
        assertTrue(publishedRendition.isProxy());
        assertEquals(section.getRef(), publishedRendition.getParentRef());
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        List<DocumentModel> versions = session.getVersions(file.getRef());
        assertNotNull(versions);
        assertEquals(1, versions.size());
        List<DocumentModel> retrievedPublished = session.query(String.format(
                "SELECT * FROM Document WHERE ecm:isProxy = 1 AND rend:sourceVersionableId = '%s'", file.getId()));
        assertEquals(1, retrievedPublished.size());
        assertEquals(publishedRendition.getId(), retrievedPublished.get(0).getId());
    }

    /**
     * @since 10.3
     */
    @Test
    public void shouldPublishMutlipleDocument() throws OperationException {
        DocumentModel file1 = createDummyFile();
        DocumentModel file2 = createDummyFile();
        DocumentModel section = session.createDocumentModel("/", "section", "Section");
        section = session.createDocument(section);

        DocumentModelList publishedRenditions;
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(new String[] { file1.getId(), file2.getId() });
            Map<String, Object> params = new HashMap<>();
            params.put("target", section);
            params.put("defaultRendition", true);
            publishedRenditions = (DocumentModelList) automationService.run(ctx, PublishRendition.ID, params);
        }
        assertNotNull(publishedRenditions);
        assertEquals(2, publishedRenditions.size());
    }

    /**
     * @since 10.3
     */
    @Test
    public void shouldUnpublishAll() throws OperationException {
        DocumentModel file = createDummyFile();
        DocumentModel section = session.createDocumentModel("/", "section", "Section");
        section = session.createDocument(section);

        session.publishDocument(file, section);
        RenditionService rs = Framework.getService(RenditionService.class);
        rs.publishRendition(file, section, "pdf", false);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        String publishedDocQuery = String.format(Constants.ALL_PUBLICATION_QUERY, NXQL.escapeString(file.getId()),
                NXQL.escapeString(file.getId()));
        List<DocumentModel> publishedDocs = session.query(publishedDocQuery);
        assertEquals(2, publishedDocs.size());
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            automationService.run(ctx, UnpublishAll.ID);
        }
        publishedDocs = session.query(publishedDocQuery);
        assertEquals(0, publishedDocs.size());
    }

    protected DocumentModel createDummyFile() {
        Blob blob = Blobs.createBlob("dummy content");
        blob.setFilename("dummy.txt");
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        bh.setBlob(blob);
        return session.createDocument(file);
    }

    @Test(expected = NuxeoException.class)
    public void shouldThroughTraceExceptionForNonExistingRendition() throws OperationException {
        DocumentModel file = createDummyFile();

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(file);
        Map<String, Object> params = new HashMap<>();
        params.put("renditionName", "nonExistingRendition");
        automationService.run(ctx, GetRendition.ID, params);
    }
}
