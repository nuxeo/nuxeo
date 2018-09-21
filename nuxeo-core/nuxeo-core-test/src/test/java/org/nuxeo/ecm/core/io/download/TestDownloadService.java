/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.download;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.io.download.DownloadServiceImpl.Action;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.cache")
public class TestDownloadService {

    protected static abstract class DummyServletOutputStream extends ServletOutputStream {
        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }
    }

    @Inject
    protected DownloadService downloadService;

    @Test
    public void testBasicDownload() throws Exception {
        // blob to download
        String blobValue = "Hello World Caf\u00e9";
        String encoding = "ISO-8859-1";
        Blob blob = Blobs.createBlob(blobValue, "text/plain", encoding);
        blob.setFilename("myFile.txt");
        blob.setDigest("12345");

        // prepare mocks
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);

        // send download request
        downloadService.downloadBlob(request, response, null, null, blob, null, null);

        // check that the blob gets returned
        assertEquals(blobValue, out.toString(encoding));
    }

    @Test
    public void testETagHeaderNone() throws Exception {
        doTestETagHeader(null);
    }

    @Test
    public void testETagHeaderNotMatched() throws Exception {
        doTestETagHeader(FALSE);
    }

    @Test
    public void testETagHeaderMatched() throws Exception {
        doTestETagHeader(TRUE);
    }

    protected void doTestETagHeader(Boolean match) throws Exception {
        // Given a blob
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myFile.txt");
        blob.setDigest("12345");

        String digestToTest;
        if (match == null) {
            digestToTest = null;
        } else if (TRUE.equals(match)) {
            digestToTest = "12345";
        } else {
            digestToTest = "78787";
        }

        // When I send a request a given digest
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("If-None-Match")).thenReturn('"' + digestToTest + '"');
        when(req.getMethod()).thenReturn("GET");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);

        downloadService.downloadBlob(req, resp, null, null, blob, null, null);

        verify(req, atLeast(1)).getHeader("If-None-Match");

        // Then the response differs if the digest match
        if (TRUE.equals(match)) {
            assertEquals(0, out.toByteArray().length);
            verify(resp).sendError(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            assertEquals(blobValue, out.toString());
            verify(resp).setHeader("ETag", '"' + blob.getDigest() + '"');
        }
    }

    @Test
    public void testETagHeaderNoDigest() throws Exception {
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myFile.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("If-None-Match")).thenReturn("\"b10a8db164e0754105b7a99be72e3fe5\"");
        when(req.getMethod()).thenReturn("GET");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);

        downloadService.downloadBlob(req, resp, null, null, blob, null, "test");

        verify(req, atLeastOnce()).getHeader("If-None-Match");
        assertEquals(0, out.toByteArray().length);
        verify(resp).sendError(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-download-service-permission.xml")
    public void testDownloadPermission() throws Exception {
        // blob to download
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myfile.txt");
        blob.setDigest("12345");

        // mock request
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        // mock response
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);

        // mock document
        DocumentModel doc = mock(DocumentModel.class);
        when(doc.getPropertyValue("dc:format")).thenReturn("pdf");

        // extended infos with rendition
        String reason = "rendition";
        Map<String, Serializable> extendedInfos = Collections.singletonMap("rendition", "myrendition");

        // principal
        NuxeoPrincipal principal = new UserPrincipal("bob", Collections.singletonList("members"), false, false);

        // do tests while logged in
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(principal, null, null);
        try {
            // send download request for file:content, should be denied
            downloadService.downloadBlob(request, response, doc, "file:content", blob, null, reason, extendedInfos);
            assertEquals("", out.toString());
            verify(response, atLeastOnce()).sendError(403, "Permission denied");

            // but another xpath is allowed, per the javascript rule
            downloadService.downloadBlob(request, response, doc, "other:blob", blob, null, reason, extendedInfos);
            assertEquals(blobValue, out.toString());
        } finally {
            loginStack.pop();
        }
    }

    /**
     * @since 9.3
     */
    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-download-service-default-download.xml")
    public void testDocumentDefaultDownloadAndPermission() throws Exception {
        // blob to download
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myfile.txt");
        blob.setDigest("12345");

        // mock request
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        // mock response
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);

        // mock document
        DocumentModel doc = mock(DocumentModel.class);
        // default Mocked Download Blob Holder returns file:content
        when(doc.getPropertyValue("file:content")).thenReturn((Serializable) blob);

        // send download request permission denied
        downloadService.downloadBlob(request, response, doc, null, null, null, null);
        assertEquals("", out.toString());
        verify(response, atLeastOnce()).sendError(403, "Permission denied");

        // send download request, should return main content
        when(doc.getPropertyValue("dc:format")).thenReturn("txt");
        downloadService.downloadBlob(request, response, doc, null, null, null, null);
        assertEquals(blobValue, out.toString());
    }

    /**
     * @since 9.3
     */
    @Test
    public void testAsyncDownload() throws IOException {
        // blob to download
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myfile.txt");
        blob.setDigest("12345");

        // mock request
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        // mock response
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);

        // principal
        NuxeoPrincipal principal = new UserPrincipal("bob", Collections.singletonList("members"), false, false);

        String key = downloadService.storeBlobs(Collections.singletonList(blob));
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);
        ts.setCompleted(key, false);
        // do tests while logged in
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(principal, null, null);
        try {
            // send status request for not complete stored blob, should be in progress
            downloadService.downloadBlobStatus(request, response, key, "download");
            assertEquals("{\"key\":\"" + key + "\",\"completed\":false,\"progress\":-1}", out.toString());

            // send download request for not completed stored blob, should be accepted
            out.reset();
            downloadService.downloadBlob(request, response, key, "download");
            assertEquals("", out.toString());
            verify(response, atLeastOnce()).setStatus(202);

            ts.setCompleted(key, true);
            ts.putParameter(key, DownloadService.TRANSIENT_STORE_PARAM_PROGRESS, 100);
            out.reset();
            // send status request for complete stored blob, should be complete
            downloadService.downloadBlobStatus(request, response, key, "download");
            assertEquals("{\"key\":\"" + key + "\",\"completed\":true,\"progress\":100}", out.toString());

            out.reset();
            // send download request for complete stored blob
            downloadService.downloadBlob(request, response, key, "download");
            assertEquals(blobValue, out.toString());
        } finally {
            loginStack.pop();
        }
    }

    @Test
    public void testDownloadNonExistingBlob() throws IOException {
        // mock request
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        // mock response
        HttpServletResponse response = mock(HttpServletResponse.class);

        // principal
        NuxeoPrincipal principal = new UserPrincipal("bob", Collections.singletonList("members"), false, false);

        String key = downloadService.storeBlobs(Collections.emptyList());
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);
        ts.setCompleted(key, false);
        // do tests while logged in
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(principal, null, null);
        try {
            // send download request for non existing key, should be not found
            downloadService.downloadBlob(request, response, "undefinedKey", "download");
            verify(response, atLeastOnce()).sendError(404);

            // send download request for non existing blob, should be not found
            out.reset();
            downloadService.downloadBlob(request, response, key, "download");
            verify(response, atLeastOnce()).sendError(404);
        } finally {
            loginStack.pop();
        }
    }

    @Test
    public void testTransientCleanup() throws IOException {
        // transfert temporary file into a blob
        Path path = Files.createTempFile("pfouh", "pfouh");
        FileBlob blob = new FileBlob("pfouh");
        Files.move(path, blob.getFile().toPath(), REPLACE_EXISTING);

        // store the blob for downloading
        String key = downloadService.storeBlobs(Collections.singletonList(blob));

        // mock request
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        // mock response
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);

        NuxeoPrincipal principal = new UserPrincipal("bob", Collections.singletonList("members"), false, false);

        // do tests while logged in
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(principal, null, null);

        try {
            downloadService.downloadBlob(request, response, key, "download");
        } finally {
            loginStack.pop();
        }

        // the file is gone
        assertFalse(blob.getFile().exists());
    }

    @Test
    public void testGetDownloadPathAndAction() {
        DownloadServiceImpl downloadServiceImpl = new DownloadServiceImpl();
        String path = "nxfile/default/3727ef6b-cf8c-4f27-ab2c-79de0171a2c8/files:files/0/file/image.png";
        Pair<String, Action> pair = downloadServiceImpl.getDownloadPathAndAction(path);
        assertEquals("default/3727ef6b-cf8c-4f27-ab2c-79de0171a2c8/files:files/0/file/image.png", pair.getLeft());
        assertEquals(Action.DOWNLOAD_FROM_DOC, pair.getRight());

        path = "plop/default/3727ef6b-cf8c-4f27-ab2c-79de0171a2c8/files:files/0/file/image.png";
        pair = downloadServiceImpl.getDownloadPathAndAction(path);
        assertNull(pair);
    }

    @Test
    public void testResolveBlobFromDownloadUrl() throws IOException {
        String repositoryName = "test";
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName)) {
            DocumentModel doc = session.createDocumentModel("/", "James-Bond", "File");
            doc.setProperty("dublincore", "title", "Diamonds are forever");

            FileBlob blob = new FileBlob("Synopsis");
            String blobFilename = "synopsis.txt";
            blob.setFilename(blobFilename);

            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("file", blob);
            List<Map<String, Object>> docFiles = new ArrayList<>();
            docFiles.add(fileMap);

            doc.setProperty("files", "files", docFiles);
            doc = session.createDocument(doc);
            session.save();

            String path = "nxfile/" + repositoryName + "/" + doc.getId() + "/files:files/0/file/" + blobFilename;
            Blob resolvedBlob = downloadService.resolveBlobFromDownloadUrl(path);
            assertEquals(blob, resolvedBlob);
        }
    }

    @Test
    public void testDownloadUrl() throws IOException {
        // Windows path separators
        String filename = "C:\\My Documents\\foo.txt";
        String url = downloadService.getDownloadUrl("default", "1234", "file:content", filename);
        assertEquals("nxfile/default/1234/file:content/foo.txt", url);
        // Unix path separators
        filename = "/home/john/foo.txt";
        url = downloadService.getDownloadUrl("default", "1234", "file:content", filename);
        assertEquals("nxfile/default/1234/file:content/foo.txt", url);
    }

    @Test
    public void testDownloadBadEncoding() throws Exception {
        // blob to download
        String blobValue = "Hello World Caf\u00e9";
        String encoding = "no-such-charset"; // must not crash DownloadService
        Blob blob = Blobs.createBlob(blobValue.getBytes(), "text/plain", encoding);
        blob.setFilename("myFile.txt");

        // prepare mocks
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);
        // throw exception when setting encoding
        doThrow(new IllegalArgumentException()).when(response).setCharacterEncoding(encoding);

        // send download request
        downloadService.downloadBlob(request, response, null, null, blob, null, null);

        // check that the blob gets returned even though the encoding was illegal
        assertEquals(blobValue, out.toString()); // decode with system default
    }

    @Test
    public void testDownloadUrlWithURLSanitization() throws IOException {
        String filename = "/home/john/foo.txt;jsessionid=FooBarBaz;otherparam=foobarbaz";
        String url = downloadService.getDownloadUrl("default", "1234", "file:content", filename);
        assertEquals("nxfile/default/1234/file:content/foo.txt", url);

        filename = "/home/john/foo.txt;jsessionid=FooBarBaz";
        url = downloadService.getDownloadUrl("default", "1234", "file:content", filename);
        assertEquals("nxfile/default/1234/file:content/foo.txt", url);
    }

    @Test
    public void testDownloadUrlWithSemiColonInTheFilename() throws IOException {
        String filename = "/home/john/foo;bar.txt";
        String url = downloadService.getDownloadUrl("default", "1234", "file:content", filename);
        assertEquals("nxfile/default/1234/file:content/foo%3Bbar.txt", url);
    }

    /**
     * @since 10.3
     */
    @Test
    public void testDownloadUrlWithChangeToken() throws IOException {
        String repositoryName = "test";
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repositoryName)) {
            Framework.getProperties().setProperty("nuxeo.url", "http://localhost:8080/nuxeo");
            DocumentModel doc = session.createDocumentModel("/", "James-Bond", "File");
            doc.setProperty("dublincore", "title", "Spectre");

            FileBlob blob = new FileBlob("Synopsis");
            String blobFilename = "synopsis.txt";
            blob.setFilename(blobFilename);

            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("file", blob);
            List<Map<String, Object>> docFiles = new ArrayList<>();
            docFiles.add(fileMap);

            doc.setProperty("files", "files", docFiles);
            doc = session.createDocument(doc);
            session.save();

            String pattern = "nxfile/test/.*/file:content/synopsis.txt\\?changeToken=[^&]+";
            String url = downloadService.getDownloadUrl(repositoryName, doc.getId(), "file:content", "synopsis.txt",
                    doc.getChangeToken());
            assertTrue(url.matches(pattern));
            url = downloadService.getDownloadUrl(doc, "file:content", "synopsis.txt");
            assertTrue(url.matches(pattern));
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-download-service-listener.xml")
    public void testDownloadLoggedIfNoByteRange() throws IOException {
        doTestDownloadLoggedOrNot(null, "Hello World", 1); // logged
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-download-service-listener.xml")
    public void testDownloadLoggedIfByteRangeFromStart() throws IOException {
        doTestDownloadLoggedOrNot("0-4", "Hello", 1); // logged
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-download-service-listener.xml")
    public void testDownloadNotLoggedIfByteRangeOther() throws IOException {
        doTestDownloadLoggedOrNot("6-10", "World", 0); // not logged
    }

    protected void doTestDownloadLoggedOrNot(String range, String expectedResult, int expectedSize) throws IOException {
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myFile.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest req = mock(HttpServletRequest.class);
        if (range != null) {
            when(req.getHeader("Range")).thenReturn("bytes=" + range);
        }

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);

        DummyDownloadListener.clear();
        downloadService.downloadBlob(req, resp, null, null, blob, null, "test");

        assertEquals(expectedResult, out.toString("UTF-8"));
        assertEquals(expectedSize, DummyDownloadListener.getEvents().size());
    }

}
