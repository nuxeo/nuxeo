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
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.common.utils.FileUtils;
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

public class TestImportModifiedZipArchive extends SQLRepositoryTestCase {

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

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

    @Test
    public void testImportUnModifiedArchive() throws Exception {

        File archive = FileUtils.getResourceFileFromContext("archive.zip");

        // import
        DocumentReader reader = new NuxeoArchiveReader(archive);
        DocumentWriter writer = new DocumentModelWriter(session, "/");

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        // check result
        DocumentModelList children = session.getChildren(session.getRootDocument().getRef());
        assertEquals(1, children.size());
        DocumentModel importedWS = children.get(0);
        assertEquals("test WS", importedWS.getTitle());

        children = session.getChildren(importedWS.getRef());
        DocumentModel importedDocument = children.get(0);
        Blob blob = (Blob) importedDocument.getProperty("file", "content");
        assertEquals("dummyBlob.txt", blob.getFilename());
        assertEquals("SomeDummyContent", blob.getString());

        byte[] expected = MessageDigest.getInstance("MD5").digest(
                "SomeDummyContent".getBytes());
        String source = Base64.encodeBytes(expected);

        byte[] actual = MessageDigest.getInstance("MD5").digest(
                FileUtils.readBytes(blob.getStream()));
        String result = Base64.encodeBytes(actual);

        assertEquals(source, result);
    }

    @Test
    public void testImportModifiedArchive() throws Exception {

        File archive = FileUtils.getResourceFileFromContext("modified_archive.zip");

        // import
        DocumentReader reader = new NuxeoArchiveReader(archive);
        DocumentWriter writer = new DocumentModelWriter(session, "/");

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        // check result
        DocumentModelList children = session.getChildren(session.getRootDocument().getRef());
        assertEquals(1, children.size());
        DocumentModel importedWS = children.get(0);
        assertEquals("test WS (imported)", importedWS.getTitle());

        children = session.getChildren(importedWS.getRef());
        DocumentModel importedDocument = children.get(0);
        Blob blob = (Blob) importedDocument.getProperty("file", "content");
        assertEquals("dummyBlob.txt", blob.getFilename());
        assertEquals("SomeDummyContentImported\n", blob.getString());

    }

    @Test
    public void testImportModifiedArchiveWithNewItems() throws Exception {

        File archive = FileUtils.getResourceFileFromContext("modified_archive2.zip");

        // import
        DocumentReader reader = new NuxeoArchiveReader(archive);
        DocumentWriter writer = new DocumentModelWriter(session, "/");

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        // check result
        DocumentModelList children = session.getChildren(session.getRootDocument().getRef());
        assertEquals(1, children.size());
        DocumentModel importedWS = children.get(0);
        assertEquals("test WS (imported)", importedWS.getTitle());

        children = session.getChildren(importedWS.getRef());
        assertEquals(2, children.size());

        children = session.getChildren(importedWS.getRef(), "File");
        assertEquals(1, children.size());
        DocumentModel importedDocument = children.get(0);
        Blob blob = (Blob) importedDocument.getProperty("file", "content");
        assertEquals("dummyBlob.txt", blob.getFilename());
        assertEquals("SomeDummyContentImported\n", blob.getString());

        children = session.getChildren(importedWS.getRef(), "Folder");
        assertEquals(1, children.size());
        DocumentModel newFolder = children.get(0);
        assertEquals("subfolder created from import", newFolder.getTitle());

        children = session.getChildren(newFolder.getRef(), "File");
        assertEquals(1, children.size());
        DocumentModel createdDocument = children.get(0);
        assertEquals("Created File", createdDocument.getTitle());
        blob = (Blob) createdDocument.getProperty("file", "content");
        assertEquals("newBlob.txt", blob.getFilename());
        assertEquals("NewContent\n", blob.getString());

    }

    @Test
    public void testImportContentTemplateArchive() throws Exception {

        File archive = FileUtils.getResourceFileFromContext("export.zip");

        // import
        DocumentReader reader = new NuxeoArchiveReader(archive);
        DocumentWriter writer = new DocumentModelWriter(session, "/");

        DocumentPipe pipe = new DocumentPipeImpl(10);
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();

        session.save();
        // check result

        StringBuffer sb = new StringBuffer();
        DocumentModelList docs = session.query("select * from Document order by ecm:path");
        for (DocumentModel doc : docs) {
            sb.append(doc.getPathAsString() + " - " + doc.getType() + " -- "
                    + doc.getTitle());
            sb.append("\n");
        }

        String dump = sb.toString();
        assertTrue(dump.contains("testZipImport - Folder"));
        assertTrue(dump.contains("hello file - File"));
        assertTrue(dump.contains("HelloNote - Note"));
        assertTrue(dump.contains("SubFolder - Folder"));
        assertTrue(dump.contains("SubNote - Note"));

    }

}
