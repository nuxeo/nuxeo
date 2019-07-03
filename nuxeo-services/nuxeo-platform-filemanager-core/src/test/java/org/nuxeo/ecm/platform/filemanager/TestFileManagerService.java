/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.filemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
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
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.filemanager.service.FileManagerService;
import org.nuxeo.ecm.platform.filemanager.service.extension.FileImporter;
import org.nuxeo.ecm.platform.filemanager.utils.FileManagerUtils;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.tests:ecm-types-test-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.filemanager.core.tests:nxfilemanager-test-contribs.xml")
public class TestFileManagerService {

    protected DocumentModel workspace;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected FileManager fileManager;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void setUp() throws Exception {
        workspace = coreSession.createDocumentModel("/", "workspace", "Workspace");
        workspace = coreSession.createDocument(workspace);
    }

    protected File getTestFile(String relativePath) {
        return FileUtils.getResourceFileFromContext(relativePath);
    }

    @Test
    public void testDefaultCreateFromBlob() throws Exception {
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/msword");

        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        assertNotNull(doc.getProperty("file", "content"));
        BinaryBlob blob = (BinaryBlob) doc.getProperty("file", "content");
        assertEquals("application/msword", blob.getMimeType());
        assertEquals("hello.doc", blob.getFilename());

        // let's make the same test but this time without mime-type checking
        // because the blob already carries a mime-type that matches the file name
        // using mime-type check on or off should yield the same result
        context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                     .overwrite(true)
                                     .fileName("test-data/hello2.doc")
                                     .mimeTypeCheck(false)
                                     .build();
        doc = fileManager.createOrUpdateDocument(context);
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
        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .fileName("test-data/hello3.doc")
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals("hello3.doc", doc.getProperty("dublincore", "title"));
        assertNotNull(doc.getProperty("file", "content"));
        BinaryBlob blob = (BinaryBlob) doc.getProperty("file", "content");
        assertEquals("application/msword", blob.getMimeType());
        assertEquals("hello3.doc", blob.getFilename());

        input = Blobs.createBlob(file, "application/sometype");
        context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                     .overwrite(true)
                                     .fileName("test-data/hello3.doc")
                                     .mimeTypeCheck(false)
                                     .build();
        doc = fileManager.createOrUpdateDocument(context);
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

        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .fileName("test-data/hello.doc")
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.doc", blob.getFilename());

        // create again with same file
        doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.doc", blob.getFilename());
    }

    @Test
    public void testDefaultUpdateFromBlob() throws Exception {
        // create doc
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/msword");

        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.doc", doc.getProperty("dublincore", "title"));
        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.doc", blob.getFilename());

        // update it with another file with same name
        context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                     .overwrite(true)
                                     .fileName("test-data/update/hello.doc")
                                     .build();
        doc = fileManager.createOrUpdateDocument(context);
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

        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals("hello.html", doc.getProperty("dublincore", "title"));
        String expectedNoteTest = NOTE_HTML_CONTENT;
        String noteText = (String) doc.getProperty("note", "note");
        if (SystemUtils.IS_OS_WINDOWS) {
            expectedNoteTest = expectedNoteTest.trim();
            expectedNoteTest = expectedNoteTest.replace("\n", "");
            expectedNoteTest = expectedNoteTest.replace("\r", "");
            noteText = noteText.trim();
            noteText = noteText.replace("\n", "");
            noteText = noteText.replace("\r", "");
        }
        assertEquals(expectedNoteTest, noteText);
    }

    @Test
    public void testCreateNoteTwiceFromSameBlob() throws Exception {
        // create doc
        File file = getTestFile("test-data/hello.html");
        Blob input = Blobs.createBlob(file, "text/html");

        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        DocumentRef docRef = doc.getRef();

        assertNotNull(doc);
        assertEquals("hello.html", doc.getProperty("dublincore", "title"));
        String expectedNoteTest = NOTE_HTML_CONTENT;
        String noteText = (String) doc.getProperty("note", "note");
        if (SystemUtils.IS_OS_WINDOWS) {
            expectedNoteTest = expectedNoteTest.trim();
            expectedNoteTest = expectedNoteTest.replace("\n", "");
            expectedNoteTest = expectedNoteTest.replace("\r", "");
            noteText = noteText.trim();
            noteText = noteText.replace("\n", "");
            noteText = noteText.replace("\r", "");
        }
        assertEquals(expectedNoteTest, noteText);

        // create again with same file
        doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        assertEquals("hello.html", doc.getProperty("dublincore", "title"));
        noteText = (String) doc.getProperty("note", "note");
        if (SystemUtils.IS_OS_WINDOWS) {
            noteText = noteText.trim();
            noteText = noteText.replace("\n", "");
            noteText = noteText.replace("\r", "");
        }
        assertEquals(expectedNoteTest, noteText);
    }

    @Test
    public void testFileImporterDocType() {
        FileManagerService fileManagerService = (FileManagerService) fileManager;
        FileImporter plugin = fileManagerService.getPluginByName("plug");
        assertNotNull(plugin);
        assertNull(plugin.getDocType());

        plugin = fileManagerService.getPluginByName("pluginWithDocType");
        assertNotNull(plugin.getDocType());
        assertEquals("File", plugin.getDocType());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.filemanager.core.tests:nxfilemanager-test-override.xml")
    public void testFileImportersMerge() {
        FileManagerService fileManagerService = (FileManagerService) fileManager;

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
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals("application/vnd.ms-excel", blob.getMimeType());
        assertEquals("File", doc.getType());
    }

    @Test
    public void testCreateBlobWithAmbiguousMimetype() throws Exception {
        File file = getTestFile("test-data/hello.xml");
        Blob blob = Blobs.createBlob(file);
        blob.setMimeType("text/plain");
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
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
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .fileName("test-data/hello.plouf")
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
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
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .fileName("test-data/hello.plouf")
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals("File", doc.getType());
    }

    @Test
    public void testCreateBlobWithNonNFCNormalizedFilename() throws Exception {
        // Create doc from non NFC (NFD) normalized filename
        String fileName = "ÜÜÜ ÓÓÓ.rtf";
        String nfdNormalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFD);
        Blob blob = Blobs.createBlob("Test content", "text/rtf", null, nfdNormalizedFileName);
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        fileManager.createOrUpdateDocument(context);
        assertNotNull(FileManagerUtils.getExistingDocByFileName(coreSession, workspace.getPathAsString(),
                nfdNormalizedFileName));
        // Check existing doc with NFC normalized filename
        String nfcNormalizedFileName = Normalizer.normalize(fileName, Normalizer.Form.NFC);
        assertNotNull(FileManagerUtils.getExistingDocByFileName(coreSession, workspace.getPathAsString(),
                nfcNormalizedFileName));
    }

    @Test
    public void testGetExistingBlobWithNonNFCNormalizedFilename() throws Exception {
        // Create doc from NFC normalized filename
        String fileName = "ÜÜÜ ÓÓÓ.rtf";
        String nfcNormalizedFileName = Normalizer.normalize(fileName, Form.NFC);
        Blob blob = Blobs.createBlob("Test content", "text/rtf", null, nfcNormalizedFileName);
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        fileManager.createOrUpdateDocument(context);
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

        // update the with a file that matches the same importer
        file = getTestFile("test-data/hello.html");
        input = Blobs.createBlob(file, "text/html");
        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);

        DocumentRef newDocRef = doc.getRef();
        assertEquals(docRef, newDocRef);
        blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);
        assertEquals("hello.html", blob.getFilename());
        assertTrue(extractText(doc).contains("HTML"));
        assertEquals("text/html", getMimeType(doc));
    }

    @Test
    public void testCreateFolder() throws IOException {
        DocumentModel testFolder = fileManager.createFolder(coreSession, "testFolder", workspace.getPathAsString(),
                true);
        // Overwrite existing folder
        DocumentModel testFolder2 = fileManager.createFolder(coreSession, "testFolder", workspace.getPathAsString(),
                true);
        assertEquals(testFolder.getId(), testFolder2.getId());
        // Create a new folder
        DocumentModel testFolder3 = fileManager.createFolder(coreSession, "testFolder", workspace.getPathAsString(),
                false);
        assertNotEquals(testFolder.getId(), testFolder3.getId());
    }

    /*
     * NXP-24830
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.filemanager.core.tests:test-nxfilemanager-mandatory-metadata-contrib.xml")
    public void testCreateBlobWithDocTypeHoldingMandatoryMetadataWithDefault() throws Exception {
        File file = getTestFile("test-data/hello.doc");
        Blob blob = Blobs.createBlob(file);
        blob.setMimeType("application/msword");
        FileImporterContext context = FileImporterContext.builder(coreSession, blob, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals("application/msword", blob.getMimeType());
        assertEquals("SpecialFile", doc.getType());
        // check mandatory metadata has fallback on its default value
        assertEquals("france", doc.getPropertyValue("sf:country"));
    }

    @Test
    public void testExcludeOneToManyFileImporters() throws Exception {
        // .doc input, don't exclude oneToMany importers, expecting a new document holding the file
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/msword");
        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .mimeTypeCheck(false)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals(workspace.getRef(), doc.getParentRef());
        assertEquals("File", doc.getType());
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("hello.doc", blob.getFilename());

        // .doc input, exclude oneToMany importers, still expecting a new document holding the file
        context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                     .overwrite(true)
                                     .mimeTypeCheck(false)
                                     .excludeOneToMany(true)
                                     .build();
        doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals(workspace.getRef(), doc.getParentRef());
        assertEquals("File", doc.getType());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("hello.doc", blob.getFilename());

        // .zip input, don't exclude oneToMany importers, expecting the target folder
        file = getTestFile("test-data/testCSVArchive.zip");
        input = Blobs.createBlob(file, "application/zip");
        context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                     .overwrite(true)
                                     .mimeTypeCheck(false)
                                     .build();
        doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals(workspace.getRef(), doc.getRef());
        assertEquals("Workspace", doc.getType());

        // .zip input, exclude oneToMany importers, expecting a new document holding the ZIP file (DefaultFileImporter
        // selected instead of CSVZipImporter)
        context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                     .overwrite(true)
                                     .mimeTypeCheck(false)
                                     .excludeOneToMany(true)
                                     .build();
        doc = fileManager.createOrUpdateDocument(context);
        assertNotNull(doc);
        assertEquals(workspace.getRef(), doc.getParentRef());
        assertEquals("File", doc.getType());
        blob = (Blob) doc.getPropertyValue("file:content");
        assertEquals("testCSVArchive.zip", blob.getFilename());
    }

    @Test
    public void testPersistDocument() throws IOException {
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/msword");

        // do not persis the document, expecting just a dirty document model w/o an uuid
        FileImporterContext context = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                         .overwrite(true)
                                                         .persistDocument(false)
                                                         .build();
        DocumentModel doc = fileManager.createOrUpdateDocument(context);
        assertNull(doc.getId());
        assertTrue(doc.isDirty());

        // persist the document
        doc = coreSession.createDocument(doc);
        txFeature.nextTransaction();

        String docId = doc.getId();
        assertNotNull(docId);
        assertFalse(doc.isDirty());

        // update with same file w/o persisting the document
        doc = fileManager.createOrUpdateDocument(context);
        String newDocId = doc.getId();
        assertTrue(doc.isDirty());
        assertEquals(docId, newDocId);
    }

    @Test
    public void fileNameShouldNotBeEmpty() throws IOException {
        File file = getTestFile("test-data/hello.doc");
        Blob input = Blobs.createBlob(file, "application/msword");

        // We take the file name if it is provided, otherwise the blob name.
        FileImporterContext fileImporter = FileImporterContext.builder(coreSession, input, workspace.getPathAsString())
                                                              .fileName("myOwnFileName")
                                                              .build();
        assertEquals("myOwnFileName", fileImporter.getFileName());

        fileImporter = FileImporterContext.builder(coreSession, input, workspace.getPathAsString()).build();
        assertEquals("hello.doc", fileImporter.getFileName());

    }

    private Object getMimeType(DocumentModel doc) {
        return ((Blob) doc.getProperty("file", "content")).getMimeType();
    }

    private String extractText(DocumentModel doc) throws IOException {
        return ((Blob) doc.getProperty("file", "content")).getString();
    }

}
