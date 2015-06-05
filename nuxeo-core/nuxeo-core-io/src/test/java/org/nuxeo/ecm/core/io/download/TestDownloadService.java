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
 */
package org.nuxeo.ecm.core.io.download;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/download-service.xml")
public class TestDownloadService {

    @Inject
    protected DownloadService downloadService;

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

}
