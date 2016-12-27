/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestExportedDocument.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.MessageDigest;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestImportModifiedZipArchive {

    @Inject
    protected CoreSession session;

    DocumentModel rootDocument;

    DocumentModel workspace;

    DocumentModel docToExport;

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

        byte[] expected = MessageDigest.getInstance("MD5").digest("SomeDummyContent".getBytes());
        String source = Base64.encodeBase64String(expected);

        byte[] actual = MessageDigest.getInstance("MD5").digest(IOUtils.toByteArray(blob.getStream()));
        String result = Base64.encodeBase64String(actual);

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
            sb.append(doc.getPathAsString() + " - " + doc.getType() + " -- " + doc.getTitle());
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
