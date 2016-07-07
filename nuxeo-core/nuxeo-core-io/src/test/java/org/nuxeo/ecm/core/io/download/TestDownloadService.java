/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.io.download;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogCaptureFeature.Filter;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.ecm.core.io")
@LocalDeploy("org.nuxeo.ecm.core.io.test:OSGI-INF/test-download-service.xml")
public class TestDownloadService {

    @Inject
    protected DownloadService downloadService;

    @Inject
    private LogCaptureFeature.Result logCaptureResults;

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
    @LogCaptureFeature.FilterWith(DownloadServiceEtagNoDigestFilter.class)
    public void testETagHeaderNoDigest() throws Exception {
        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        blob.setFilename("myFile.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("If-None-Match")).thenReturn("\"1234\"");
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

        downloadService.downloadBlob(req, resp, null, null, blob, null, "test");

        verify(req, never()).getHeader("If-None-Match");
        logCaptureResults.assertHasEvent();
        LoggingEvent log = logCaptureResults.getCaughtEvents().get(0);
        assertEquals(
                "ETag and cache has been disabled because blob doesn't have digest for reason=test, docId=null, filename=myFile.txt, xpath=null",
                log.getMessage());
    }

    public static class DownloadServiceEtagNoDigestFilter implements Filter {

        @Override
        public boolean accept(LoggingEvent event) {
            return event.getLevel().equals(Level.WARN)
                    && event.getLoggerName().equals("org.nuxeo.ecm.core.io.download.DownloadServiceImpl");
        }

    }

    @Test
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

}
