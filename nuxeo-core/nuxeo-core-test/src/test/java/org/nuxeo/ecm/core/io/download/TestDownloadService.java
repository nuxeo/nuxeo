/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.io.download.DownloadService.EXTENDED_INFO_RENDITION;
import static org.nuxeo.ecm.core.io.download.DownloadService.REQUEST_ATTR_DOWNLOAD_REASON;
import static org.nuxeo.ecm.core.io.download.DownloadService.REQUEST_ATTR_DOWNLOAD_RENDITION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
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
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.core.blob.binary.DefaultBinaryManager;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.io.NginxConstants;
import org.nuxeo.ecm.core.io.download.DownloadService.DownloadContext;
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

    private static final String CONTENT = "this is a file au caf\u00e9";

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
    public void testBasicDownloadGet() throws Exception {
        doTestBasicDownload(false);
    }

    @Test
    public void testBasicDownloadHead() throws Exception {
        doTestBasicDownload(true);
    }

    protected void doTestBasicDownload(boolean head) throws Exception {
        // ascii filename is used directly
        doTestBasicDownload(head, "cafe.txt", "filename=cafe.txt");
        // non-ascii filename gets RFC2231 encoding
        doTestBasicDownload(head, "caf\u00e9.txt", "filename*=UTF-8''caf%C3%A9.txt");
    }

    protected void doTestBasicDownload(boolean head, String filename, String filenameInHeader) throws Exception {
        // regular download
        doTestBasicDownload(head, filename, filenameInHeader, false);
        // download with empty=true in Content-Type
        doTestBasicDownload(head, filename, filenameInHeader, true);
    }

    protected void doTestBasicDownload(boolean head, String filename, String filenameInHeader, boolean empty) throws Exception {
        // blob to download
        String blobValue = "Hello World Caf\u00e9";
        String mimeType = "text/plain";
        if (empty) {
            mimeType += "; empty=true";
        }
        String encoding = "ISO-8859-1";
        String digest = "12345";
        Blob blob = Blobs.createBlob(blobValue, mimeType, encoding);
        blob.setFilename(filename);
        blob.setDigest(digest);
        Calendar lastModified = GregorianCalendar.from(ZonedDateTime.parse("2001-02-03T04:05:06Z"));

        // prepare mocks
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(head ? "HEAD" : "GET");

        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);

        // send download request
        DownloadContext context = DownloadContext.builder(request, response)
                                                 .blob(blob)
                                                 .lastModified(lastModified)
                                                 .build();
        downloadService.downloadBlob(context);

        // assert headers (mockito wants us to assert all header in same order they were set)
        if (empty) {
            verify(response, never()).setHeader(eq("ETag"), any());
        } else {
            verify(response).setHeader(eq("ETag"), eq('"' + digest + '"'));
        }
        verify(response).setHeader(eq("Content-Disposition"), eq("attachment; " + filenameInHeader));
        verify(response).setHeader(eq("Accept-Ranges"), eq("bytes"));
        // assert others interactions
        verify(response).setContentType(eq(mimeType));
        verify(response).setCharacterEncoding(encoding);
        verify(response).setContentLengthLong(eq(blob.getLength()));
        if (empty) {
            verify(response, never()).setDateHeader(eq("Last-Modified"), anyLong());
        } else {
            verify(response).setDateHeader(eq("Last-Modified"), eq(lastModified.getTimeInMillis()));
        }
        // check that the blob gets returned (except if HEAD)
        assertEquals(head ? "" : blobValue, out.toString(encoding));
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
            public void write(int b) {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);

        DownloadContext context = DownloadContext.builder(req, resp)
                                                 .blob(blob)
                                                 .build();
        downloadService.downloadBlob(context);

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
            public void write(int b) {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);

        DownloadContext context = DownloadContext.builder(req, resp)
                                                 .blob(blob)
                                                 .reason("test")
                                                 .build();
        downloadService.downloadBlob(context);

        verify(req, atLeastOnce()).getHeader("If-None-Match");
        assertEquals(0, out.toByteArray().length);
        verify(resp).sendError(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    public void testWantDigestMD5() throws Exception {
        doTestWantDigest(Arrays.asList("MD5"), "MD5=sQqNsWTgdUEFt6mb5y4/5Q==", null);
    }

    @Test
    public void testWantDigestContentMD5() throws Exception {
        doTestWantDigest(Arrays.asList("contentMD5"), null, "sQqNsWTgdUEFt6mb5y4/5Q==");
    }

    @Test
    public void testWantDigestMD5AndContentMD5() throws Exception {
        doTestWantDigest(Arrays.asList("MD5", "contentMD5"), "MD5=sQqNsWTgdUEFt6mb5y4/5Q==",
                "sQqNsWTgdUEFt6mb5y4/5Q==");
    }

    @Test
    public void testWantDigestUnknown() throws Exception {
        doTestWantDigest(Arrays.asList("nosuchalgo"), null, null);
    }

    protected void doTestWantDigest(List<String> wanted, String expectedDigest, String expectedContentMD5)
            throws Exception {
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myFile.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeaders("Want-Digest")).thenReturn(Collections.enumeration(wanted));
        when(req.getMethod()).thenReturn("GET");

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) {
                out.write(b);
            }
        };
        @SuppressWarnings("resource")
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);

        DownloadContext context = DownloadContext.builder(req, resp)
                                                 .blob(blob)
                                                 .build();
        downloadService.downloadBlob(context);

        verify(resp).setHeader(eq("ETag"), eq("\"b10a8db164e0754105b7a99be72e3fe5\""));
        if (expectedDigest != null) {
            verify(resp).setHeader(eq("Digest"), eq(expectedDigest));
        }
        if (expectedContentMD5 != null) {
            verify(resp).setHeader(eq("Content-MD5"), eq(expectedContentMD5));
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-download-service-permission.xml")
    public void testDownloadPermission() throws Exception {
        doTestDownloadPermission(false);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-download-service-permission.xml")
    public void testDownloadPermissionWithReasonInRequestAttribute() throws Exception {
        doTestDownloadPermission(true);
    }

    protected void doTestDownloadPermission(boolean useRequestAttribute) throws Exception {
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
            public void write(int b) {
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
        String reason;
        Map<String, Serializable> extendedInfos;
        if (!useRequestAttribute) {
            // reason/rendition passed explicitly to downloadBlob method
            reason = "rendition";
            extendedInfos = Collections.singletonMap(EXTENDED_INFO_RENDITION, "myrendition");
        } else {
            // reason/rendition passed in request attribute
            reason = null;
            extendedInfos = null;
            when(request.getAttribute(REQUEST_ATTR_DOWNLOAD_REASON)).thenReturn("rendition");
            when(request.getAttribute(REQUEST_ATTR_DOWNLOAD_RENDITION)).thenReturn("myrendition");
        }

        // principal
        NuxeoPrincipal principal = new UserPrincipal("bob", Collections.singletonList("members"), false, false);

        // do tests while logged in
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(principal, null, null);
        try {
            // send download request for file:content, should be denied
            DownloadContext.Builder builder = DownloadContext.builder(request, response)
                                                             .doc(doc)
                                                             .xpath("file:content")
                                                             .blob(blob)
                                                             .reason(reason)
                                                             .extendedInfos(extendedInfos);
            downloadService.downloadBlob(builder.build());
            assertEquals("", out.toString());
            verify(response, atLeastOnce()).sendError(403, "Permission denied");

            // but another xpath is allowed, per the javascript rule
            builder.xpath("other:blob");
            downloadService.downloadBlob(builder.build());
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
            public void write(int b) {
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
        DownloadContext context = DownloadContext.builder(request, response)
                                                 .doc(doc)
                                                 .build();
        downloadService.downloadBlob(context);
        assertEquals("", out.toString());
        verify(response, atLeastOnce()).sendError(403, "Permission denied");

        // send download request, should return main content
        when(doc.getPropertyValue("dc:format")).thenReturn("txt");
        downloadService.downloadBlob(context);
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
            public void write(int b) {
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
            public void write(int b) {
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
    public void testDownloadUrl() {
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
            public void write(int b) {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);
        // throw exception when setting encoding
        doThrow(new IllegalArgumentException()).when(response).setCharacterEncoding(encoding);

        // send download request
        DownloadContext context = DownloadContext.builder(request, response)
                                                 .blob(blob)
                                                 .build();
        downloadService.downloadBlob(context);

        // check that the blob gets returned even though the encoding was illegal
        assertEquals(blobValue, out.toString()); // decode with system default
    }

    @Test
    public void testDownloadUrlWithURLSanitization() {
        String filename = "/home/john/foo.txt;jsessionid=FooBarBaz;otherparam=foobarbaz";
        String url = downloadService.getDownloadUrl("default", "1234", "file:content", filename);
        assertEquals("nxfile/default/1234/file:content/foo.txt", url);

        filename = "/home/john/foo.txt;jsessionid=FooBarBaz";
        url = downloadService.getDownloadUrl("default", "1234", "file:content", filename);
        assertEquals("nxfile/default/1234/file:content/foo.txt", url);
    }

    @Test
    public void testDownloadUrlWithSemiColonInTheFilename() {
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
    public void testDownloadLoggedIfNoByteRange() throws IOException {
        doTestDownloadLoggedOrNot(null, false, "Hello World", 1); // logged
    }

    @Test
    public void testDownloadNotLoggedIfHead() throws IOException {
        doTestDownloadLoggedOrNot(null, true, "", 0); // not logged
    }

    @Test
    public void testDownloadLoggedIfByteRangeFromStart() throws IOException {
        doTestDownloadLoggedOrNot("0-4", false, "Hello", 1); // logged
    }

    @Test
    public void testDownloadNotLoggedIfByteRangeOther() throws IOException {
        doTestDownloadLoggedOrNot("6-10", false, "World", 0); // not logged
    }

    protected void doTestDownloadLoggedOrNot(String range, boolean head, String expectedResult, int expectedSize)
            throws IOException {
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myFile.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(head ? "HEAD" : "GET");
        if (range != null) {
            when(req.getHeader("Range")).thenReturn("bytes=" + range);
        }

        HttpServletResponse resp = mock(HttpServletResponse.class);
        ServletOutputStream sos = new DummyServletOutputStream() {
            @Override
            public void write(int b) {
                out.write(b);
            }
        };
        PrintWriter printWriter = new PrintWriter(sos);
        when(resp.getOutputStream()).thenReturn(sos);
        when(resp.getWriter()).thenReturn(printWriter);

        try (CapturingEventListener listener = new CapturingEventListener(DownloadService.EVENT_NAME)) {
            DownloadContext context = DownloadContext.builder(req, resp)
                                                     .blob(blob)
                                                     .reason("test")
                                                     .build();
            downloadService.downloadBlob(context);

            assertEquals(expectedResult, out.toString("UTF-8"));
            assertEquals(expectedSize, listener.getCapturedEventCount(DownloadService.EVENT_NAME));
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-default-blob-provider.xml")
    public void testDownloadWithNginxAccel() throws IOException {
        Framework.getProperties().put(NginxConstants.X_ACCEL_ENABLED, "true");
        try {
            // create a temporary FileBlob
            DefaultBinaryManager binaryManager = new DefaultBinaryManager();
            binaryManager.initialize("repo", Collections.emptyMap());
            Blob source = new FileBlob(new ByteArrayInputStream(CONTENT.getBytes("UTF-8")));
            Binary binary = binaryManager.getBinary(source);
            String digest = binary.getDigest();
            String filename = "cafe.txt";
            long length = binary.getFile().length();
            Blob blob = new BinaryBlob(binary, digest, filename, "text/plain", "utf-8", digest, length);

            // mock request response
            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getHeader(NginxConstants.X_ACCEL_LOCATION_HEADER)).thenReturn("/protected_files");
            HttpServletResponse resp = mock(HttpServletResponse.class);
            ServletOutputStream sos = new DummyServletOutputStream() {
                @Override
                public void write(int b) {
                    throw new NuxeoException("Not supposed to write to response");
                }
            };
            PrintWriter printWriter = new PrintWriter(sos);
            when(resp.getOutputStream()).thenReturn(sos);
            when(resp.getWriter()).thenReturn(printWriter);

            // download it
            DownloadContext context = DownloadContext.builder(req, resp)
                                                     .blob(blob)
                                                     .reason("test")
                                                     .build();
            downloadService.downloadBlob(context);

            // assert headers (mockito wants us to assert all header in same order they were set)
            verify(resp).setHeader(eq("ETag"), eq('"' + digest + '"'));
            verify(resp).setHeader(eq("Content-Disposition"), eq("attachment; filename=cafe.txt"));
            verify(resp).setHeader(eq("Accept-Ranges"), eq("bytes"));
            verify(resp).setHeader(eq(NginxConstants.X_ACCEL_REDIRECT_HEADER),
                    eq("/protected_files/d2/5e/d25ea4f4642073b7f218024d397dbaef"));
            // assert others interactions
            verify(resp).setContentType(eq("text/plain"));
            verify(resp).setCharacterEncoding("utf-8");
            verify(resp).setContentLengthLong(eq(blob.getLength()));
            // assert we end the download
            verifyNoMoreInteractions(resp);
        } finally {
            Framework.getProperties().remove(NginxConstants.X_ACCEL_ENABLED);
        }
    }

}
