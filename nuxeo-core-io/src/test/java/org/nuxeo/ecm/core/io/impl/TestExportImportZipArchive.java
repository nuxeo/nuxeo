/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestExportedDocument.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestExportImportZipArchive extends SQLRepositoryTestCase {

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

    protected static final String XML_DATA = "\n    <nxdt:templateParams xmlns:nxdt=\"http://www.nuxeo.org/DocumentTemplate\">\n<nxdt:field name=\"htmlContent\" type=\"content\" source=\"htmlPreview\"></nxdt:field>\n   </nxdt:templateParams>\n    ";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.api");

        openSession();
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    private void createDocs() throws Exception {
        rootDocument = session.getRootDocument();
        workspace = session.createDocumentModel(rootDocument.getPathAsString(),
                "ws1", "Workspace");
        workspace.setProperty("dublincore", "title", "test WS");
        workspace = session.createDocument(workspace);

        docToExport = session.createDocumentModel(workspace.getPathAsString(),
                "file", "File");
        docToExport.setProperty("dublincore", "title", "MyDoc");

        docToExport.setProperty("dublincore", "description", XML_DATA);

        Blob blob = new StringBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        blob.setMimeType("text/plain");
        docToExport.setProperty("file", "content", blob);

        docToExport = session.createDocument(docToExport);

        docToExport.addFacet("HiddenInNavigation");
        docToExport = session.saveDocument(docToExport);

        session.save();
    }

    @Test
    public void testExportAsZipAndReimport() throws Exception {
        createDocs();

        File zip = File.createTempFile("core-io-archive", "zip");

        DocumentReader reader = new DocumentTreeReader(session, workspace);
        DocumentWriter writer = new NuxeoArchiveWriter(zip);

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        File archive = new File(zip.getAbsolutePath());
        assertTrue(archive.exists());
        assertTrue(archive.length() > 0);

        writer.close();
        reader.close();

        // check the zip contents
        ZipInputStream zin = new ZipInputStream(new FileInputStream(archive));
        ZipEntry entry = zin.getNextEntry();
        int nbDocs = 0;
        int nbBlobs = 0;
        while (entry != null) {
            if (entry.getName().endsWith(ExportConstants.DOCUMENT_FILE)) {
                nbDocs++;
            } else if (entry.getName().endsWith(".blob")) {
                nbBlobs++;
            }
            entry = zin.getNextEntry();
        }
        assertEquals(2, nbDocs);
        assertEquals(1, nbBlobs);

        // now wipe DB
        session.removeDocument(workspace.getRef());
        session.save();
        assertEquals(0,
                session.getChildren(session.getRootDocument().getRef()).size());

        // reimport
        reader = new NuxeoArchiveReader(archive);
        writer = new DocumentModelWriter(session, "/");

        pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        // check result
        DocumentModelList children = session.getChildren(session.getRootDocument().getRef());
        assertEquals(1, children.size());
        DocumentModel importedWS = children.get(0);
        assertEquals(workspace.getTitle(), importedWS.getTitle());

        children = session.getChildren(importedWS.getRef());
        DocumentModel importedDocument = children.get(0);
        Blob blob = (Blob) importedDocument.getProperty("file", "content");
        assertEquals("dummyBlob.txt", blob.getFilename());
        // check attributes
        assertEquals("MyDoc", importedDocument.getPropertyValue("dc:title"));
        assertEquals(XML_DATA,
                importedDocument.getPropertyValue("dc:description"));

        // check that facets have been reimported
        assertTrue(importedDocument.hasFacet("HiddenInNavigation"));
    }

}
