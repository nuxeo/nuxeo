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

package org.nuxeo.ecm.platform.io.operation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.io.api", "org.nuxeo.ecm.platform.io.core" })
public class TestExportDocument {

    @Inject
    protected AutomationService as;

    @Inject
    protected CoreSession session;

    @Test
    public void shouldExportDocumentAsXML() throws OperationException {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Blob result = (Blob) as.run(ctx, ExportDocument.ID);
        assertNotNull(result);
        assertEquals("document.xml", result.getFilename());
    }

    @Test
    public void shouldExportDocumentAsZip() throws OperationException, IOException {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(doc);
        Map<String, Object> params = new HashMap<>();
        params.put("exportAsZip", true);
        Blob result = (Blob) as.run(ctx, ExportDocument.ID, params);
        assertNotNull(result);
        assertEquals("export.zip", result.getFilename());

        ZipInputStream zin = new ZipInputStream(result.getStream());
        ZipEntry entry = zin.getNextEntry();
        int nbDocs = 0;
        while (entry != null) {
            if (entry.getName().endsWith(ExportConstants.DOCUMENT_FILE)) {
                nbDocs++;
            }
            entry = zin.getNextEntry();
        }
        assertEquals(1, nbDocs);
    }

    @Test
    public void shouldExportDocumentAsTree() throws OperationException, IOException {
        DocumentModel folder1 = session.createDocumentModel("/", "folder1", "Folder");
        folder1 = session.createDocument(folder1);
        DocumentModel file1 = session.createDocumentModel(folder1.getPathAsString(), "file1", "File");
        file1 = session.createDocument(file1);
        DocumentModel folder2 = session.createDocumentModel(folder1.getPathAsString(), "folder2", "Folder");
        folder2 = session.createDocument(folder2);
        DocumentModel file2 = session.createDocumentModel(folder2.getPathAsString(), "file2", "File");
        file2 = session.createDocument(file2);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(folder1);
        Map<String, Object> params = new HashMap<>();
        params.put("exportAsTree", true);
        params.put("exportAsZip", false);
        Blob result = (Blob) as.run(ctx, ExportDocument.ID, params);
        assertNotNull(result);
        assertEquals("export.zip", result.getFilename());

        ZipInputStream zin = new ZipInputStream(result.getStream());
        ZipEntry entry = zin.getNextEntry();
        int nbDocs = 0;
        while (entry != null) {
            if (entry.getName().endsWith(ExportConstants.DOCUMENT_FILE)) {
                nbDocs++;
            }
            entry = zin.getNextEntry();
        }
        assertEquals(4, nbDocs);
    }
}
