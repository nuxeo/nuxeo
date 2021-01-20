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
package org.nuxeo.ecm.platform.web.idempotency;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.platform.web.common.idempotency.NuxeoIdempotentResponse.SKIPPED_HEADERS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nuxeo.ecm.platform.web.common.idempotency.NuxeoIdempotentFilter;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Checks idempotent requests management.
 *
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/idempotency-configuration.xml")
public class TestNuxeoIdempotentFilter {

    protected static final String KEY = "mykey";

    protected static final String CONTENT = "test content";

    protected static final Map<String, Collection<String>> RESPONSE_HEADERS = new LinkedHashMap<>();

    static {
        RESPONSE_HEADERS.put("Accept", Arrays.asList("text/html", "application/xhtml+xml", "*/*;q=0.8"));
        RESPONSE_HEADERS.put("Connection", Arrays.asList("Keep-Alive"));
        RESPONSE_HEADERS.put("Content-Encoding", Arrays.asList("gzip"));
        RESPONSE_HEADERS.put("Content-Type", Arrays.asList("text/html; charset=utf-8"));
        RESPONSE_HEADERS.put("Set-Cookie", Arrays.asList("sessionId=38afes7a8", "id=a3fWa; Max-Age=2592000"));
        RESPONSE_HEADERS.put("Transfer-Encoding", Arrays.asList("chunked")); // should be filtered
    }

    protected static final Map<String, Collection<String>> KEY_RESPONSE_HEADERS = new LinkedHashMap<>();

    static {
        KEY_RESPONSE_HEADERS.put(NuxeoIdempotentFilter.HEADER_KEY, Arrays.asList(KEY));
    }

    protected static final Map<String, Collection<String>> FINAL_RESPONSE_HEADERS = new LinkedHashMap<>();

    static {
        FINAL_RESPONSE_HEADERS.putAll(RESPONSE_HEADERS);
        FINAL_RESPONSE_HEADERS.putAll(KEY_RESPONSE_HEADERS);
    }

    protected static final Map<String, Collection<String>> FINAL_COPY_RESPONSE_HEADERS = new LinkedHashMap<>();

    static {
        FINAL_COPY_RESPONSE_HEADERS.putAll(FINAL_RESPONSE_HEADERS);
        FINAL_COPY_RESPONSE_HEADERS.keySet().removeAll(SKIPPED_HEADERS);
    }

    protected NuxeoIdempotentFilter filter;

    protected FilterChain chain;

    protected HttpServletRequest request;

    protected MockResponse mockResponse;

    @Inject
    protected KeyValueService kvs;

    protected KeyValueStoreProvider store;

    protected static class MockResponse {

        protected HttpServletResponse response;

        protected int status;

        protected Map<String, Collection<String>> headers = new LinkedHashMap<>();

        protected OutputStream output;

        public MockResponse() throws IOException {
            super();
            response = mock(HttpServletResponse.class);
            // output mock
            output = new ByteArrayOutputStream();
            ServletOutputStream servletOutput = mock(ServletOutputStream.class);
            doAnswer(invocation -> {
                output.write((Integer) invocation.getArguments()[0]);
                return null;
            }).when(servletOutput).write(anyInt());
            doAnswer(invocation -> {
                output.write((byte[]) invocation.getArguments()[0]);
                return null;
            }).when(servletOutput).write(any(byte[].class));
            doAnswer(invocation -> {
                output.write((byte[]) invocation.getArguments()[0], (Integer) invocation.getArguments()[1],
                        (Integer) invocation.getArguments()[2]);
                return null;
            }).when(servletOutput).write(any(byte[].class), anyInt(), anyInt());
            when(response.getOutputStream()).thenReturn(servletOutput);
            PrintWriter writer = mock(PrintWriter.class);
            doAnswer(invocation -> {
                output.write(((String) invocation.getArguments()[0]).getBytes());
                return null;
            }).when(writer).write(anyString());
            doAnswer(invocation -> writer).when(response).getWriter();
            when(response.getCharacterEncoding()).thenReturn(UTF_8.name());
            // status mock
            doAnswer(invocation -> status).when(response).getStatus();
            doAnswer(invocation -> {
                status = (Integer) invocation.getArguments()[0];
                return null;
            }).when(response).setStatus(anyInt());
            // headers mock
            when(response.getHeaderNames()).thenReturn(headers.keySet());
            doAnswer(invocation -> headers.get(invocation.getArguments()[0]).stream().findFirst().get()).when(
                    response).getHeader(anyString());
            doAnswer(invocation -> headers.get(invocation.getArguments()[0])).when(response).getHeaders(anyString());
            doAnswer(invocation -> {
                headers.put((String) invocation.getArguments()[0],
                        new ArrayList<>(Arrays.asList((String) invocation.getArguments()[1])));
                return null;
            }).when(response).setHeader(anyString(), anyString());
            doAnswer(invocation -> {
                headers.computeIfAbsent((String) invocation.getArguments()[0], k -> new ArrayList<>())
                       .add((String) invocation.getArguments()[1]);
                return null;
            }).when(response).addHeader(anyString(), anyString());
        }

        public int getStatus() {
            return status;
        }

        public HttpServletResponse getResponse() {
            return response;
        }

        public OutputStream getOutput() {
            return output;
        }

    }

    @Before
    public void setUp() throws IOException {
        filter = new NuxeoIdempotentFilter();
        chain = mock(FilterChain.class);
        request = mock(HttpServletRequest.class);
        mockResponse = new MockResponse();
        // handle store
        store = (KeyValueStoreProvider) kvs.getKeyValueStore(NuxeoIdempotentFilter.DEFAULT_STORE);
        store.clear();
    }

    @After
    public void tearDown() {
        if (filter != null) {
            filter.destroy();
        }
        if (store != null) {
            store.clear();
        }
    }

    protected void checkResponse(MockResponse mockResponse, Integer status, String content,
            Map<String, Collection<String>> headers) {
        assertEquals(status, (Integer) mockResponse.getStatus());
        assertEquals(content, mockResponse.getOutput().toString());
        HttpServletResponse response = mockResponse.getResponse();
        assertEquals(headers.keySet(), response.getHeaderNames());
        headers.forEach((k, v) -> assertEquals(v, response.getHeaders(k)));
    }

    protected void checkStore(String status, String content) {
        assertEquals(content, store.getString(KEY));
        String ikey = KEY + NuxeoIdempotentFilter.INFO_SUFFIX;
        if (status == null) {
            assertNull(store.getString(ikey));
        } else {
            if (NuxeoIdempotentFilter.INPROGRESS_MARKER.equals(status)) {
                assertEquals(status, store.getString(ikey));
            } else {
                String info = "{\"headers\":" //
                        + "{\"Accept\":[\"text/html\",\"application/xhtml+xml\",\"*/*;q=0.8\"]," //
                        + "\"Connection\":[\"Keep-Alive\"]," //
                        + "\"Content-Encoding\":[\"gzip\"]," //
                        + "\"Content-Type\":[\"text/html; charset=utf-8\"]," //
                        + "\"Idempotency-Key\":[\"mykey\"],"
                        + "\"Set-Cookie\":[\"sessionId=38afes7a8\",\"id=a3fWa; Max-Age=2592000\"]" //
                        + "},\"status\":%s}";
                assertEquals(String.format(info, status), store.getString(ikey));
            }
        }
    }

    @Test
    public void testGetRequestWithoutKey() throws IOException, ServletException {
        when(request.getMethod()).thenReturn(HttpGet.METHOD_NAME);
        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, mockResponse.getResponse(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkStore(null, null);
    }

    @Test
    public void testPostRequestWithoutKey() throws IOException, ServletException {
        when(request.getMethod()).thenReturn(HttpPost.METHOD_NAME);
        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, mockResponse.getResponse(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkStore(null, null);
    }

    @Test
    public void testGetRequest() throws IOException, ServletException {
        when(request.getMethod()).thenReturn(HttpGet.METHOD_NAME);
        when(request.getHeader(NuxeoIdempotentFilter.HEADER_KEY)).thenReturn(KEY);
        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, mockResponse.getResponse(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkStore(null, null);
    }

    protected void setResult(HttpServletResponse response, int status, String content) throws IOException {
        response.setStatus(status);
        response.getWriter().write(content);
        // mock headers
        RESPONSE_HEADERS.forEach((k, v) -> v.forEach(vitem -> response.addHeader(k, vitem)));
    }

    @Test
    public void testPostRequest() throws IOException, ServletException {
        when(request.getMethod()).thenReturn(HttpPost.METHOD_NAME);
        when(request.getHeader(NuxeoIdempotentFilter.HEADER_KEY)).thenReturn(KEY);
        // mock final call
        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws IOException {
                setResult((HttpServletResponse) invocation.getArguments()[1], SC_OK, CONTENT);
                return null;
            }
        }).when(chain).doFilter(any(), any());

        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, mockResponse.getResponse(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(mockResponse, SC_OK, CONTENT, FINAL_RESPONSE_HEADERS);
        checkStore(String.valueOf(SC_OK), CONTENT);
        // call filter again: stored value will be sent back again
        MockResponse mockResponse2 = new MockResponse();
        filter.doFilter(request, mockResponse2.getResponse(), chain);
        // chain filter not called again
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(mockResponse2, SC_OK, CONTENT, FINAL_COPY_RESPONSE_HEADERS);
        checkStore(String.valueOf(SC_OK), CONTENT);
    }

    @Test
    public void testPostRequestInProgress() throws IOException, ServletException {
        when(request.getMethod()).thenReturn(HttpPost.METHOD_NAME);
        when(request.getHeader(NuxeoIdempotentFilter.HEADER_KEY)).thenReturn(KEY);

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws IOException, ServletException {
                // during first execution, execute another request
                MockResponse mockResponse2 = new MockResponse();
                verify(chain, times(1)).doFilter(any(), any());
                filter.doFilter(request, mockResponse2.getResponse(), mock(FilterChain.class));
                verify(chain, times(1)).doFilter(any(), any());
                checkResponse(mockResponse2, SC_CONFLICT, "", KEY_RESPONSE_HEADERS);
                checkStore(NuxeoIdempotentFilter.INPROGRESS_MARKER, null);

                // finish first call
                setResult((HttpServletResponse) invocation.getArguments()[1], SC_OK, CONTENT);
                return null;
            }
        }).when(chain).doFilter(any(), any());

        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, mockResponse.getResponse(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(mockResponse, SC_OK, CONTENT, FINAL_RESPONSE_HEADERS);
        checkStore(String.valueOf(SC_OK), CONTENT);
    }

    @Test
    public void testPostRequestException() throws IOException, ServletException {
        when(request.getMethod()).thenReturn(HttpPost.METHOD_NAME);
        when(request.getHeader(NuxeoIdempotentFilter.HEADER_KEY)).thenReturn(KEY);

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws IOException, ServletException {
                throw new ServletException("test error");
            }
        }).when(chain).doFilter(any(), any());

        verify(chain, times(0)).doFilter(any(), any());
        try {
            filter.doFilter(request, mockResponse.getResponse(), chain);
            fail("should have thrown Servlet Exception");
        } catch (ServletException e) {
            // ok
        }
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(mockResponse, SC_INTERNAL_SERVER_ERROR, "", KEY_RESPONSE_HEADERS);
        checkStore(null, null);

        // try again
        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws IOException {
                setResult((HttpServletResponse) invocation.getArguments()[1], SC_OK, CONTENT);
                return null;
            }
        }).when(chain).doFilter(any(), any());
        verify(chain, times(1)).doFilter(any(), any());
        filter.doFilter(request, mockResponse.getResponse(), chain);
        verify(chain, times(2)).doFilter(any(), any());
        checkResponse(mockResponse, SC_OK, CONTENT, FINAL_RESPONSE_HEADERS);
        checkStore(String.valueOf(SC_OK), CONTENT);
    }

    @Test
    public void testPostRequestError() throws IOException, ServletException {
        when(request.getMethod()).thenReturn(HttpPost.METHOD_NAME);
        when(request.getHeader(NuxeoIdempotentFilter.HEADER_KEY)).thenReturn(KEY);

        doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws IOException, ServletException {
                setResult((HttpServletResponse) invocation.getArguments()[1], SC_NOT_FOUND, "not found");
                return null;
            }
        }).when(chain).doFilter(any(), any());

        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, mockResponse.getResponse(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(mockResponse, SC_NOT_FOUND, "not found", FINAL_RESPONSE_HEADERS);
        checkStore(null, null);
    }

}
