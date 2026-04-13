/*
 * (C) Copyright 2020-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static jakarta.servlet.http.HttpServletResponse.SC_CONFLICT;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.nuxeo.ecm.platform.web.common.idempotency.NuxeoIdempotentResponse.SKIPPED_HEADERS;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.web.common.MockHttpServletRequest;
import org.nuxeo.ecm.platform.web.common.MockHttpServletResponse;
import org.nuxeo.ecm.platform.web.common.idempotency.NuxeoIdempotentFilter;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.RuntimeKeyValueStoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Checks idempotent requests management.
 *
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeKeyValueStoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/idempotency-configuration.xml")
public class TestNuxeoIdempotentFilter {

    protected static final String KEY = "mykey";

    protected static final String CONTENT = "test content";

    protected static final Map<String, Collection<String>> RESPONSE_HEADERS = new LinkedHashMap<>();

    static {
        RESPONSE_HEADERS.put("Accept", List.of("text/html", "application/xhtml+xml", "*/*;q=0.8"));
        RESPONSE_HEADERS.put("Connection", List.of("Keep-Alive"));
        RESPONSE_HEADERS.put("Content-Encoding", List.of("gzip"));
        RESPONSE_HEADERS.put("Content-Type", List.of("text/html; charset=utf-8"));
        RESPONSE_HEADERS.put("Set-Cookie", List.of("sessionId=38afes7a8", "id=a3fWa; Max-Age=2592000"));
        RESPONSE_HEADERS.put("Transfer-Encoding", List.of("chunked")); // should be filtered
    }

    protected static final Map<String, Collection<String>> KEY_RESPONSE_HEADERS = new LinkedHashMap<>();

    static {
        KEY_RESPONSE_HEADERS.put(NuxeoIdempotentFilter.HEADER_KEY, List.of(KEY));
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

    @Inject
    protected KeyValueService kvs;

    @Before
    public void setUp() throws IOException {
        filter = new NuxeoIdempotentFilter();
        chain = mock(FilterChain.class);
    }

    @After
    public void tearDown() {
        if (filter != null) {
            filter.destroy();
        }
    }

    protected void checkResponse(MockHttpServletResponse mockResponse, Integer status, String content,
            Map<String, Collection<String>> headers) {
        assertEquals(status, (Integer) mockResponse.getStatus());
        assertEquals(content, mockResponse.getResponseAsString());
        HttpServletResponse response = mockResponse.mock();
        assertEquals(headers.keySet(), response.getHeaderNames());
        headers.forEach((k, v) -> assertEquals(v, response.getHeaders(k)));
    }

    protected void checkStore(String status, String content) {
        var store = kvs.getKeyValueStore(NuxeoIdempotentFilter.DEFAULT_STORE);
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
        var request = MockHttpServletRequest.init(HttpGet.METHOD_NAME).mock();
        var response = MockHttpServletResponse.init().mock();
        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkStore(null, null);
    }

    @Test
    public void testPostRequestWithoutKey() throws IOException, ServletException {
        var request = MockHttpServletRequest.init(HttpPost.METHOD_NAME).mock();
        var response = MockHttpServletResponse.init().mock();
        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkStore(null, null);
    }

    @Test
    public void testGetRequest() throws IOException, ServletException {
        var request = MockHttpServletRequest.init(HttpGet.METHOD_NAME)
                                            .whenGetHeaderThenReturn(NuxeoIdempotentFilter.HEADER_KEY, KEY)
                                            .mock();
        var response = MockHttpServletResponse.init().mock();
        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, response, chain);
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
        var request = MockHttpServletRequest.init(HttpPost.METHOD_NAME)
                                            .whenGetHeaderThenReturn(NuxeoIdempotentFilter.HEADER_KEY, KEY)
                                            .mock();
        var responseHandler = MockHttpServletResponse.init();
        // mock final call
        doAnswer(invocation -> {
            setResult((HttpServletResponse) invocation.getArguments()[1], SC_OK, CONTENT);
            return null;
        }).when(chain).doFilter(any(), any());

        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, responseHandler.mock(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(responseHandler, SC_OK, CONTENT, FINAL_RESPONSE_HEADERS);
        checkStore(String.valueOf(SC_OK), CONTENT);
        // call filter again: stored value will be sent back again
        responseHandler = MockHttpServletResponse.init();
        filter.doFilter(request, responseHandler.mock(), chain);
        // chain filter not called again
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(responseHandler, SC_OK, CONTENT, FINAL_COPY_RESPONSE_HEADERS);
        checkStore(String.valueOf(SC_OK), CONTENT);
    }

    @Test
    public void testPostRequestInProgress() throws IOException, ServletException {
        var request = MockHttpServletRequest.init(HttpPost.METHOD_NAME)
                                            .whenGetHeaderThenReturn(NuxeoIdempotentFilter.HEADER_KEY, KEY)
                                            .mock();
        var responseHandler = MockHttpServletResponse.init();

        doAnswer(invocation -> {
            // during first execution, execute another request
            var responseHandler2 = MockHttpServletResponse.init();
            verify(chain, times(1)).doFilter(any(), any());
            filter.doFilter(request, responseHandler2.mock(), mock(FilterChain.class));
            verify(chain, times(1)).doFilter(any(), any());
            checkResponse(responseHandler2, SC_CONFLICT, "", KEY_RESPONSE_HEADERS);
            checkStore(NuxeoIdempotentFilter.INPROGRESS_MARKER, null);

            // finish first call
            setResult((HttpServletResponse) invocation.getArguments()[1], SC_OK, CONTENT);
            return null;
        }).when(chain).doFilter(any(), any());

        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, responseHandler.mock(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(responseHandler, SC_OK, CONTENT, FINAL_RESPONSE_HEADERS);
        checkStore(String.valueOf(SC_OK), CONTENT);
    }

    @Test
    public void testPostRequestException() throws IOException, ServletException {
        var request = MockHttpServletRequest.init(HttpPost.METHOD_NAME)
                                            .whenGetHeaderThenReturn(NuxeoIdempotentFilter.HEADER_KEY, KEY)
                                            .mock();
        var responseHandler = MockHttpServletResponse.init();

        doAnswer(invocation -> {
            throw new ServletException("test error");
        }).when(chain).doFilter(any(), any());

        verify(chain, times(0)).doFilter(any(), any());
        assertThrows(ServletException.class, () -> filter.doFilter(request, responseHandler.mock(), chain));
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(responseHandler, SC_INTERNAL_SERVER_ERROR, "", KEY_RESPONSE_HEADERS);
        checkStore(null, null);

        // try again
        doAnswer(invocation -> {
            setResult((HttpServletResponse) invocation.getArguments()[1], SC_OK, CONTENT);
            return null;
        }).when(chain).doFilter(any(), any());
        verify(chain, times(1)).doFilter(any(), any());
        filter.doFilter(request, responseHandler.mock(), chain);
        verify(chain, times(2)).doFilter(any(), any());
        checkResponse(responseHandler, SC_OK, CONTENT, FINAL_RESPONSE_HEADERS);
        checkStore(String.valueOf(SC_OK), CONTENT);
    }

    @Test
    public void testPostRequestError() throws IOException, ServletException {
        var request = MockHttpServletRequest.init(HttpPost.METHOD_NAME)
                                            .whenGetHeaderThenReturn(NuxeoIdempotentFilter.HEADER_KEY, KEY)
                                            .mock();
        var responseHandler = MockHttpServletResponse.init();

        doAnswer(invocation -> {
            setResult((HttpServletResponse) invocation.getArguments()[1], SC_NOT_FOUND, "not found");
            return null;
        }).when(chain).doFilter(any(), any());

        verify(chain, times(0)).doFilter(any(), any());
        filter.doFilter(request, responseHandler.mock(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        checkResponse(responseHandler, SC_NOT_FOUND, "not found", FINAL_RESPONSE_HEADERS);
        checkStore(null, null);
    }

}
