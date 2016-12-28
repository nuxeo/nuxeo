/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.download;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.ecm.core.io.download.DownloadServiceImpl.Action;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.io", "org.nuxeo.ecm.core.cache" })
public class TestDownloadService {

    @Inject
    protected DownloadService downloadService;

    @Test
    public void testBasicDownload() throws Exception {
        // blob to download
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myFile.txt");
        blob.setDigest("12345");

        // prepare mocks
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream sos = new ServletOutputStream() {
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
        assertEquals(blobValue, out.toString());
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
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        @SuppressWarnings("resource")
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
    @LocalDeploy("org.nuxeo.ecm.core.io.test:OSGI-INF/test-download-service-permission.xml")
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
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        @SuppressWarnings("resource")
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

    @Test
    public void testTransientCleanup() throws IOException {
        // transfert temporary file into a blob
        Path path = Files.createTempFile("pfouh","pfouh");
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
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        };
        @SuppressWarnings("resource")
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
        CoreSession session = CoreInstance.openCoreSession(repositoryName);
        Framework.getProperties().setProperty("nuxeo.url", "http://localhost:8080/nuxeo");
        DocumentModel doc = session.createDocumentModel("/", "James-Bond", "File");
        doc.setProperty("dublincore", "title", "Diamonds are forever");

        FileBlob blob = new FileBlob("Synopsis");
        String blobFilename = "synopsis.txt";
        blob.setFilename(blobFilename);

        Map<String, Object> fileMap = new HashMap<String, Object>();
        fileMap.put("file", blob);
        fileMap.put("filename", blob.getFilename());
        List<Map<String, Object>> docFiles = new ArrayList<Map<String, Object>>();
        docFiles.add(fileMap);

        doc.setProperty("files", "files", docFiles);
        doc = session.createDocument(doc);
        session.save();

        String url = "http://localhost:8080/nuxeo/nxfile/" + repositoryName + "/" + doc.getId() + "/files:files/0/file/"
                + blobFilename;
        Blob resolvedBlob = downloadService.resolveBlobFromDownloadUrl(url);
        assertEquals(blob, resolvedBlob);
        session.close();
    }

}
