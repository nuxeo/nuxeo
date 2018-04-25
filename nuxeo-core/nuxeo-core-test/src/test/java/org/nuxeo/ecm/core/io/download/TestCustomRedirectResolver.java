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
 *     Remi Cattiau
 */
package org.nuxeo.ecm.core.io.download;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author loopingz
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.stream")
@Deploy("org.nuxeo.ecm.core.io")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-download-service-with-resolver.xml")
public class TestCustomRedirectResolver {

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
    public void testRedirectResolver() throws IOException {
        Blob blob = Blobs.createBlob("Whatever");
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
        // Verify we are redirect to Nuxeo website
        verify(response).sendRedirect(Matchers.eq("http://www.nuxeo.org"));
        // Verify there is no output
        assertEquals("", out.toString());
    }
}
