/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashMap;
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
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.operation.GetRendition;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

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
        assertEquals("dummy.txt.pdf", renditionBlob.getFilename());

        Rendition pdfRendition = renditionService.getRendition(file, "pdf");
        assertEquals(renditionBlob.getLength(), pdfRendition.getBlob().getLength());
    }

    protected DocumentModel createDummyFile() {
        Blob blob = Blobs.createBlob("dummy content");
        blob.setFilename("dummy.txt");
        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        bh.setBlob(blob);
        return session.createDocument(file);
    }

    @Test(expected = OperationException.class)
    public void shouldThroughTraceExceptionForNonExistingRendition() throws OperationException {
        DocumentModel file = createDummyFile();

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(file);
        Map<String, Object> params = new HashMap<>();
        params.put("renditionName", "nonExistingRendition");
        automationService.run(ctx, GetRendition.ID, params);
    }
}
