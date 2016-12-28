/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.automation.TraceException;
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

    @Test(expected = TraceException.class)
    public void shouldThroughTraceExceptionForNonExistingRendition() throws OperationException {
        DocumentModel file = createDummyFile();

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(file);
        Map<String, Object> params = new HashMap<>();
        params.put("renditionName", "nonExistingRendition");
        automationService.run(ctx, GetRendition.ID, params);
    }
}
