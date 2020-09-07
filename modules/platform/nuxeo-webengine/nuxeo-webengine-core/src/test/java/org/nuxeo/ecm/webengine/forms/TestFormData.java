/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.webengine.forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Matchers;

/**
 * @since 11.3
 */
public class TestFormData {

    protected static Map<String, String[]> REQUEST_MAP = Map.of(//
            "dc:title", new String[] { "My title" }, //
            "dc:description", new String[] { "My description" } //
    );

    protected HttpServletRequest getMockGetRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameterMap()).thenReturn(REQUEST_MAP);
        when(request.getParameterValues(Matchers.<String> any())).thenAnswer(
                invocation -> REQUEST_MAP.get(invocation.getArguments()[0]));
        return request;
    }

    @Test
    public void testGetString() {
        FormData form = new FormData(getMockGetRequest());
        assertEquals("My title", form.getString("dc:title"));
        assertEquals("My description", form.getString("dc:description"));
    }

    protected void checkFormFields(FormData formData) {
        Map<String, String[]> fields = formData.getFormFields();
        assertEquals(REQUEST_MAP.size(), fields.size());
        for (String key : REQUEST_MAP.keySet()) {
            assertTrue(fields.containsKey(key));
            assertEquals(List.of(REQUEST_MAP.get(key)), List.of(fields.get(key)));
        }
    }

    @Test
    public void testGetFormFields() {
        checkFormFields(new FormData(getMockGetRequest()));
    }

    @Test
    public void testGetFormFieldsMultipart() throws IOException {
        // prepare mocks
        String boundary = "-----------------------------8865888362744999763737040263";
        String contentBoundary = "--" + boundary + "\r\n";
        String endBoundary = "--" + boundary + "--\r\n";
        String requestContent = contentBoundary //
                + "Content-Disposition: form-data; name=\"dc:title\"\r\n" //
                + "\r\n" //
                + "My title\r\n" //
                + contentBoundary //
                + "Content-Disposition: form-data; name=\"dc:description\"\r\n" //
                + "\r\n" //
                + "My description\r\n" //
                + contentBoundary //
                + "Content-Disposition: form-data; name=\"archive\"; filename=\"test.txt\"\r\n" //
                + "Content-Type: text/plain\r\n" //
                + "\r\n" //
                + "My awesome blob\r\n" //
                + "\r\n" //
                + endBoundary;
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getContentLength()).thenReturn(requestContent.length());
        when(request.getContentType()).thenReturn("multipart/form-data; boundary=" + boundary);

        try (InputStream stream = new ByteArrayInputStream(requestContent.getBytes());
                StringServletInputStream sstream = new StringServletInputStream(stream);) {
            when(request.getInputStream()).thenReturn(sstream);

            checkFormFields(new FormData(request));
        }
    }

    private class StringServletInputStream extends ServletInputStream {

        private final InputStream sourceStream;

        private boolean finished = false;

        public StringServletInputStream(InputStream sourceStream) {
            this.sourceStream = sourceStream;
        }

        @Override
        public int read() throws IOException {
            int data = this.sourceStream.read();
            if (data == -1) {
                this.finished = true;
            }
            return data;
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.sourceStream.close();
        }

        @Override
        public boolean isFinished() {
            return this.finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }
    }

}
