/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core.io.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginComponent;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestAnonymousDownload {

    protected class TestHttpServletRequestWrapper extends HttpServletRequestWrapper {

        protected final Principal principal;

        public TestHttpServletRequestWrapper(HttpServletRequest request, Principal principal) {
            super(request);
            this.principal = principal;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

    }

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

    @Inject
    protected CoreSession session;

    @Test
    public void testAnonymousDownload() throws Exception {

        String repositoryName = "test";
        String baseUrl = "http://localhost:8080/nuxeo/";
        Framework.getProperties().setProperty("nuxeo.url", baseUrl);

        DocumentModel doc = session.createDocumentModel("/", "MyDoc", "File");

        String blobValue = "Hello World";
        Blob blob = Blobs.createBlob(blobValue);
        String blobFilename = "blob.txt";
        blob.setFilename(blobFilename);

        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        bh.setBlob(blob);
        doc = session.createDocument(doc);
        session.save();

        String path = "nxfile/" + repositoryName + "/" + doc.getId() + "/blobholder:0/" + blobFilename;

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
        @SuppressWarnings("resource")
        PrintWriter printWriter = new PrintWriter(sos);
        when(response.getOutputStream()).thenReturn(sos);
        when(response.getWriter()).thenReturn(printWriter);

        // anonymous principal
        NuxeoPrincipal anonymous = new UserPrincipal("johndoe", null, true, false);

        // do tests while logged in
        LoginComponent.pushPrincipal(anonymous);
        try {
            downloadService.handleDownload(new TestHttpServletRequestWrapper(request, anonymous), response, baseUrl,
                    path);
            fail("The user has to authenticate before downloading the blob");
        } catch (IOException e) {
            assertEquals("Authentication is needed for downloading the blob", e.getCause().getMessage());
            NuxeoPrincipal principal = new UserPrincipal("johnnotdoe", Collections.singletonList("members"), false,
                    false);
            LoginComponent.pushPrincipal(principal);
            try {
                downloadService.handleDownload(new TestHttpServletRequestWrapper(request, principal), response, baseUrl,
                        path);
                assertEquals(blobValue, out.toString());
            } catch (IOException ioe) {
                fail("The user should be able to download the blob");
            } finally {
                LoginComponent.popPrincipal();
            }
        } finally {
            LoginComponent.popPrincipal();
        }
    }

}
