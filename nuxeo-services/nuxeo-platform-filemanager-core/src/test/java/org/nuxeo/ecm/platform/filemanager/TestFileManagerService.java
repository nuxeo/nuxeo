/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.Normalizer;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.types.api", "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.core" })
@LocalDeploy({ FileManagerUTConstants.FILEMANAGER_BUNDLE + ":ecm-types-test-contrib.xml",
        FileManagerUTConstants.FILEMANAGER_BUNDLE + ":nxfilemanager-test-contribs.xml" })
public class TestFileManagerService {

    protected FileManager service;

    protected DocumentModel root;

    protected DocumentModel workspace;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected RuntimeHarness harness;

    @Before
    public void setUp() throws Exception {
        service = Framework.getLocalService(FileManager.class);
        root = coreSession.getRootDocument();
        createWorkspaces();
    }

    private void createWorkspaces() {
        DocumentModel workspace = coreSession.createDocumentModel(root.getPathAsString(), "workspace", "Workspace");
        coreSession.createDocument(workspace);
        this.workspace = workspace;
    }

    protected File getTestFile(String relativePath) {
        return new File(FileUtils.getResourcePathFromContext(relativePath));
    }

    @Test
    public void testDefaultCreateFromBlob() throws Exception {
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/msword");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.doc");
        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertNotNull(doc.getProperty("file", "content"));
        BinaryBlob blob = (BinaryBlob) doc.getProperty("file", "content");
        assertEquals("application/msword", blob.getMimeType());
        assertEquals("hello.doc", blob.getFilename());

        // let's make the same test but this time without mime-type checking
        // because the blob already carries a mime-type that matches the file name
        // using mime-type check on or off should yield the same result
        doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
            "test-data/hello2.doc", true);
        assertNotNull(doc);
        assertEquals("hello2.doc", doc.getProperty("dublincore", "title"));
        assertNotNull(doc.getProperty("file", "content"));
        blob = (BinaryBlob) doc.getProperty("file", "content");
        assertEquals("application/msword", blob.getMimeType());
        assertEquals("hello2.doc", blob.getFilename());
    }

    @Test
    public void testCreateFromBlobWithMimeTypeCheck() throws IOException {
        // if we use a mime-type that does not match the file's name,
        // we should still get a mime-type matching the file's name
        // but only if we use mime-type check
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/sometype");
        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
            "test-data/hello3.doc");
        assertNotNull(doc);
        assertEquals("hello3.doc", doc.getProperty("dublincore", "title"));
        assertNotNull(doc.getProperty("file", "content"));
        BinaryBlob blob = (BinaryBlob) doc.getProperty("file", "content");
        assertEquals("application/msword", blob.getMimeType());
        assertEquals("hello3.doc", blob.getFilename());

        input = Blobs.createBlob(file, "application/sometype");
        doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
            "test-data/hello3.doc", true);
        assertNotNull(doc);
        assertEquals("hello3.doc", doc.getProperty("dublincore", "title"));
        assertNotNull(doc.getProperty("file", "content"));
        blob = (BinaryBlob) doc.getProperty("file", "content");
        assertEquals("application/sometype", blob.getMimeType());
        assertEquals("hello3.doc", blob.getFilename());
    }

    @Test
    public void testDefaultCreateTwiceFromSameBlob() throws Exception {
        // create doc
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/msword");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.doc");
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.doc", blob.getFilename());

        List<DocumentModel> versions = coreSession.getVersions(docRef);
        assertEquals(0, versions.size());

        // create again with same file
        doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.doc");
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.doc", blob.getFilename());

        versions = coreSession.getVersions(docRef);
        assertEquals(1, versions.size());
    }

    @Test
    public void testDefaultUpdateFromBlob() throws Exception {
        // create doc
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/msword");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.doc");
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.doc", blob.getFilename());

        // update it with another file with same name
        doc = service.updateDocumentFromBlob(coreSession, input, workspace.getPathAsString(),
                "test-data/update/hello.doc");
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.doc", blob.getFilename());
    }

    protected static final String SEPARATOR = "\n";

    protected static final String NOTE_HTML_CONTENT = "<html>" + SEPARATOR + "<body>" + SEPARATOR
            + "  <p>Hello from HTML document</p>" + SEPARATOR + "</body>" + SEPARATOR + "</html>";

    @Test
    public void testCreateNote() throws Exception {
        File file = getTestFile("test-data/hello.html");
        Blob input = Blobs.createBlob(file, "text/html");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.html");
        assertNotNull(doc);
        assertEquals("hello.html", doc.getProperty("dublincore", "title"));
        String expectedNoteTest = NOTE_HTML_CONTENT;
        String noteText = ((String) doc.getProperty("note", "note"));
        if (SystemUtils.IS_OS_WINDOWS) {
            expectedNoteTest = expectedNoteTest.trim();
            expectedNoteTest = expectedNoteTest.replace("\n", "");
            expectedNoteTest = expectedNoteTest.replace("\r", "");
            noteText = expectedNoteTest.trim();
            noteText = expectedNoteTest.replace("\n", "");
            noteText = expectedNoteTest.replace("\r", "");
        }
        assertEquals(expectedNoteTest, noteText);
    }

    @Test
    public void testCreateNoteTwiceFromSameBlob() throws Exception {
        // create doc
        File file = getTestFile("test-data/hello.html");
        Blob input = Blobs.createBlob(file, "text/html");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.html");
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.html", doc.getProperty("dublincore", "title"));
        String expectedNoteTest = NOTE_HTML_CONTENT;
        String noteText = ((String) doc.getProperty("note", "note"));
        if (SystemUtils.IS_OS_WINDOWS) {
            expectedNoteTest = expectedNoteTest.trim();
            expectedNoteTest = expectedNoteTest.replace("\n", "");
            expectedNoteTest = expectedNoteTest.replace("\r", "");
            noteText = expectedNoteTest.trim();
            noteText = expectedNoteTest.replace("\n", "");
            noteText = expectedNoteTest.replace("\r", "");
        }
        assertEquals(expectedNoteTest, noteText);

        List<DocumentModel> versions = coreSession.getVersions(docRef);
        assertEquals(0, versions.size());

        // create again with same file
        doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.html");
        assertNotNull(doc);
        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.html", doc.getProperty("dublincore", "title"));
        noteText = ((String) doc.getProperty("note", "note"));
        if (SystemUtils.IS_OS_WINDOWS) {
            noteText = expectedNoteTest.trim();
            noteText = expectedNoteTest.replace("\n", "");
            noteText = expectedNoteTest.replace("\r", "");
        }
        assertEquals(expectedNoteTest, noteText);

        versions = coreSession.getVersions(docRef);
        assertEquals(1, versions.size());
    }

    @Test
    public void testFileImporterDocType() {
        FileManagerService fileManagerService = (FileManagerService) service;
        FileImporter plugin = fileManagerService.getPluginByName("plug");
        assertNotNull(plugin);
        assertNull(plugin.getDocType());

        plugin = fileManagerService.getPluginByName("pluginWithDocType");
        assertNotNull(plugin.getDocType());
        assertEquals("File", plugin.getDocType());
    }

    @Test
    public void testFileImportersMerge() throws Exception {
        assertNotNull(harness);
        URL url = getClass().getClassLoader().getResource("nxfilemanager-test-override.xml");
        assertNotNull(url);
        harness.deployTestContrib(FileManagerUTConstants.FILEMANAGER_BUNDLE, url);

        FileManagerService fileManagerService = (FileManagerService) service;

        FileImporter plugin = fileManagerService.getPluginByName("pluginWithDocType4merge");
        assertNotNull(plugin);
        assertNotNull(plugin.getDocType());
        assertEquals("Picture", plugin.getDocType());
        assertEquals(2, plugin.getFilters().size());
        List<String> filters = plugin.getFilters();
        assertTrue(filters.contains("image/jpeg"));
        assertTrue(filters.contains("image/png"));

        plugin = fileManagerService.getPluginByName("plug4merge");
        assertNotNull(plugin.getDocType());
        assertEquals("Note", plugin.getDocType());
        assertEquals(3, plugin.getFilters().size());
        filters = plugin.getFilters();
        assertTrue(filters.contains("text/plain"));
        assertTrue(filters.contains("text/rtf"));
        assertTrue(filters.contains("text/xml"));
    }

    @Test
    public void testCreateBlobWithNormalizedMimetype() throws Exception {
        File file = getTestFile("test-data/hello.xls");
        Blob blob = Blobs.createBlob(file);
        blob.setMimeType("text/plain");
        DocumentModel doc = service.createDocumentFromBlob(coreSession, blob, workspace.getPathAsString(), true,
                "test-data/hello.xls");
        assertNotNull(doc);
        assertEquals("application/vnd.ms-excel", blob.getMimeType());
        assertEquals("File", doc.getType());
    }

    @Test
    public void testCreateBlobWithAmbiguousMimetype() throws Exception {
        File file = getTestFile("test-data/hello.xml");
        Blob blob = Blobs.createBlob(file);
        blob.setMimeType("text/plain");
        DocumentModel doc = service.createDocumentFromBlob(coreSession, blob, workspace.getPathAsString(), true,
                "test-data/hello.xml");
        assertNotNull(doc);
        assertEquals("text/plain", blob.getMimeType());
        assertEquals("Note", doc.getType());
    }

    @Test
    public void testCreateBlobWithBlobMimetypeFallback() throws Exception {
        // don't use a binary file, we store it in a text field and PostgreSQL doesn't accept 0x00 bytes
        File file = getTestFile("test-data/hello.xml");
        Blob blob = Blobs.createBlob(file);
        blob.setFilename("hello.plouf");
        blob.setMimeType("text/plain");
        DocumentModel doc = service.createDocumentFromBlob(coreSession, blob, workspace.getPathAsString(), true,
                "test-data/hello.plouf");
        assertNotNull(doc);
        assertEquals("text/plain", blob.getMimeType());
        assertEquals("Note", doc.getType());
    }

    @Test
    public void testCreateBlobWithCalculatedBlobMimetype() throws Exception {
        File file = getTestFile("test-data/hello.doc");
        Blob blob = Blobs.createBlob(file);
        blob.setFilename("hello.plouf");
        blob.setMimeType("pif/paf");
        DocumentModel doc = service.createDocumentFromBlob(coreSession, blob, workspace.getPathAsString(), true,
                "test-data/hello.plouf");
        assertNotNull(doc);
        assertEquals("File", doc.getType());
    }

    @Test
    public void testCreateExistingBlobWithNonNFCNormalizedFilename() throws Exception {
        // Create doc from NFC normalized filename
        String fileName = "ÜÜÜ ÓÓÓ.rtf";
        String nfcNormalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFC);
        Blob blob = Blobs.createBlob("Test content", "text/rtf", null, nfcNormalizedFileName);
        service.createDocumentFromBlob(coreSession, blob, workspace.getPathAsString(), true, nfcNormalizedFileName);
        assertNotNull(FileManagerUtils.getExistingDocByFileName(coreSession, workspace.getPathAsString(),
                nfcNormalizedFileName));
        // Check existing doc with non NFC (NFD) normalized filename
        String nfdNormalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFD);
        assertNotNull(FileManagerUtils.getExistingDocByFileName(coreSession, workspace.getPathAsString(),
                nfdNormalizedFileName));
    }

    @Test
    public void testUpdateFileDocWithPlainTextFile() throws Exception {

        // create a File whose title is "hello.html" and content is "hello.rtf"
        File file = getTestFile("test-data/hello.rtf");
        Blob input = Blobs.createBlob(file, "text/rtf", null, "hello.html");

        DocumentModel doc = coreSession.createDocumentModel(workspace.getPathAsString(), "hello.html", "File");
        doc.setPropertyValue("dc:title", "hello.html");
        doc.setPropertyValue("file:content", (Serializable) input);

        // create doc
        doc = coreSession.createDocument(doc);
        coreSession.save();
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.html", doc.getProperty("dublincore", "title"));
        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.html", blob.getFilename());
        assertTrue(extractText(doc).contains("RTF"));
        assertEquals("text/rtf", getMimeType(doc));

        List<DocumentModel> versions = coreSession.getVersions(docRef);
        assertEquals(0, versions.size());

        // update the with a file that matches the same importer
        file = getTestFile("test-data/hello.html");
        input = Blobs.createBlob(file, "text/html");
        doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.html");
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.html", blob.getFilename());
        assertTrue(extractText(doc).contains("HTML"));
        assertEquals("text/html", getMimeType(doc));

        versions = coreSession.getVersions(docRef);
        assertEquals(1, versions.size());
    }

    private Object getMimeType(DocumentModel doc) {
        return ((Blob) doc.getProperty("file", "content")).getMimeType();
    }

    private String extractText(DocumentModel doc) throws IOException {
        return ((Blob) doc.getProperty("file", "content")).getString();
    }

}
