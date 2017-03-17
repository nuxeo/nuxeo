/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.Normalizer;
import java.util.List;

import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
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

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.types.api", "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.filemanager.core", "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core" })
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

    private void createWorkspaces() throws ClientException {
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

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/msword");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.doc");
        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertEquals("hello.doc", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));
    }

    @Test
    public void testDefaultCreateTwiceFromSameBlob() throws Exception {
        // create doc
        File file = getTestFile("test-data/hello.doc");

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/msword");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.doc");
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertEquals("hello.doc", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));

        List<DocumentModel> versions = coreSession.getVersions(docRef);
        assertEquals(0, versions.size());

        // create again with same file
        doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.doc");
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertEquals("hello.doc", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));

        versions = coreSession.getVersions(docRef);
        assertEquals(1, versions.size());
    }

    @Test
    public void testDefaultUpdateFromBlob() throws Exception {
        // create doc
        File file = getTestFile("test-data/hello.doc");

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "application/msword");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.doc");
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertEquals("hello.doc", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));

        // update it with another file with same name
        doc = service.updateDocumentFromBlob(coreSession, input, workspace.getPathAsString(),
                "test-data/update/hello.doc");
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertEquals("hello.doc", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));
    }

    protected static final String SEPARATOR = "\n";

    protected static final String NOTE_HTML_CONTENT = "<html>" + SEPARATOR + "<body>" + SEPARATOR
            + "  <p>Hello from HTML document</p>" + SEPARATOR + "</body>" + SEPARATOR + "</html>";

    @Test
    public void testCreateNote() throws Exception {
        File file = getTestFile("test-data/hello.html");

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "text/html");

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

        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "text/html");

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
    public void testCreateBlobWithNormalizedMimeType() throws Exception {
        File file = getTestFile("test-data/hello.doc");
        Blob blob = new FileBlob(file);
        // should fore Note creation using 'pluginToUseNormalizedMimeType'
        // plugin
        blob.setMimeType("application/csv");

        DocumentModel doc = service.createDocumentFromBlob(coreSession, blob, workspace.getPathAsString(), true,
                "test-data/hello.csv");
        assertNotNull(doc);
        assertEquals("Note", doc.getType());
    }

    @Test
    public void testCreateExistingBlobWithNonNFCNormalizedFilename() throws Exception {
        // Create doc from NFC normalized filename
        String fileName = "ÜÜÜ ÓÓÓ.rtf";
        String nfcNormalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFC);
        Blob blob = StreamingBlob.createFromString("Test content", "text/rtf");
        blob.setFilename(nfcNormalizedFileName);
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
        byte[] content = FileManagerUtils.getBytesFromFile(file);
        ByteArrayBlob input = new ByteArrayBlob(content, "text/rtf");
        input.setFilename("hello.html");

        DocumentModel doc = coreSession.createDocumentModel(workspace.getPathAsString(), "hello.html", "File");
        doc.setPropertyValue("dc:title", "hello.html");
        doc.setPropertyValue("file:content", input);
        doc.setPropertyValue("file:filename", "hello.html");

        // create doc
        doc = coreSession.createDocument(doc);
        coreSession.save();
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.html", doc.getProperty("dublincore", "title"));
        assertEquals("hello.html", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));
        assertTrue(extractText(doc).contains("RTF"));
        assertEquals("text/rtf", getMimeType(doc));

        List<DocumentModel> versions = coreSession.getVersions(docRef);
        assertEquals(0, versions.size());

        // update the with a file that matches the same importer
        file = getTestFile("test-data/hello.html");
        content = FileManagerUtils.getBytesFromFile(file);
        input = new ByteArrayBlob(content, "text/html");
        doc = service.createDocumentFromBlob(coreSession, input, workspace.getPathAsString(), true,
                "test-data/hello.html");
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.html", doc.getProperty("file", "filename"));
        assertNotNull(doc.getProperty("file", "content"));
        assertTrue(extractText(doc).contains("HTML"));
        assertEquals("text/html", getMimeType(doc));

        versions = coreSession.getVersions(docRef);
        assertEquals(1, versions.size());
    }

    @Test
    public void testCreateFolder() throws Exception {
        DocumentModel testFolder = service.createFolder(coreSession, "testFolder", workspace.getPathAsString(), true);
        // Overwrite existing folder
        DocumentModel testFolder2 = service.createFolder(coreSession, "testFolder", workspace.getPathAsString(), true);
        assertEquals(testFolder.getId(), testFolder2.getId());
        // Create a new folder
        DocumentModel testFolder3 = service.createFolder(coreSession, "testFolder", workspace.getPathAsString(), false);
        assertNotEquals(testFolder.getId(), testFolder3.getId());
    }

    private Object getMimeType(DocumentModel doc) {
        return ((Blob) doc.getProperty("file", "content")).getMimeType();
    }

    private String extractText(DocumentModel doc) throws IOException {
        return ((Blob) doc.getProperty("file", "content")).getString();
    }

}
