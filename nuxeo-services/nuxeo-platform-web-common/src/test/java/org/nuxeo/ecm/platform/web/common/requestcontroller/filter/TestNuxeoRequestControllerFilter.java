/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfigImpl;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, TransactionalFeature.class, MockitoFeature.class })
@TransactionalConfig(autoStart = false)
public class TestNuxeoRequestControllerFilter {

    @Mock
    @RuntimeService
    protected RequestControllerManager manager;

    protected NuxeoRequestControllerFilter filter;

    protected DummyFilterChain chain;

    protected Exception chainException;

    protected static abstract class DummyServletOutputStream extends ServletOutputStream {
        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }
    }

    public class DummyFilterChain implements FilterChain {

        public boolean hasTransaction;

        @SuppressWarnings("resource")
        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            hasTransaction = TransactionHelper.isTransactionActive();
            response.getOutputStream().write("ABC".getBytes());
            try {
                if (chainException != null) {
                    throw chainException;
                }
            } catch (IOException | ServletException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }

    @Before
    public void setUp() {
        filter = new NuxeoRequestControllerFilter();
        chain = new DummyFilterChain();
    }

    protected Map<String, List<String>> mockResponseHeaders(HttpServletResponse response) {
        Map<String, List<String>> headers = new HashMap<>();
        // containsHeader
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            return headers.get(key);
        }).when(response).containsHeader(anyString());
        // setHeader
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            String value = (String) i.getArguments()[1];
            headers.put(key, new ArrayList<>(Arrays.asList(value)));
            return null;
        }).when(response).setHeader(anyString(), any());
        // addHeader
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            String value = (String) i.getArguments()[1];
            headers.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            return null;
        }).when(response).addHeader(anyString(), any());
        // getHeader
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            List<String> values = headers.get(key);
            return values == null || values.isEmpty() ? null : values.get(0);
        }).when(response).getHeader(anyString());
        // getHeaders
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            List<String> values = headers.get(key);
            return values == null ? Collections.emptyList() : values;
        }).when(response).getHeaders(anyString());
        // getHeaderNames
        doAnswer(i -> headers.keySet()).when(response).getHeaderNames();
        return headers;
    }

    @SuppressWarnings("resource")
    @Test
    public void testBasics() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ServletOutputStream out = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                bout.write(b);
            }
        };
        when(response.getOutputStream()).thenReturn(out);
        Map<String, List<String>> responseHeaders = mockResponseHeaders(response);

        RequestFilterConfig filterConfig = new RequestFilterConfigImpl(false, true, true, true, true, "123");
        when(manager.getConfigForRequest(any())).thenReturn(filterConfig);
        Map<String, String> managerHeaders = new HashMap<>();
        managerHeaders.put("MyHeader", "my-header-value");
        when(manager.getResponseHeaders()).thenReturn(managerHeaders);

        filter.doFilter(request, response, chain);

        verify(response, never()).setStatus(anyInt());
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());

        Map<String, List<String>> expectedResponseHeaders = new HashMap<>();
        expectedResponseHeaders.put("MyHeader", Arrays.asList("my-header-value"));
        expectedResponseHeaders.put("Cache-Control", Arrays.asList("private, max-age=123"));
        assertNotNull(responseHeaders.remove("Expires"));
        assertEquals(expectedResponseHeaders, responseHeaders);

        assertTrue(chain.hasTransaction);
        assertEquals("ABC", bout.toString());
    }

    @Test
    public void testNuxeoException() throws IOException, ServletException {
        doTestException(new NuxeoException(456), 456);
    }

    @Test
    public void testRuntimeException() throws IOException, ServletException {
        doTestException(new RuntimeException(), SC_INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("resource")
    protected void doTestException(Exception exc, int expectedStatus) throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ServletOutputStream out = new DummyServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                bout.write(b);
            }
        };
        when(response.getOutputStream()).thenReturn(out);

        RequestFilterConfig filterConfig = new RequestFilterConfigImpl(false, true, true, false, false, "");
        when(manager.getConfigForRequest(any())).thenReturn(filterConfig);

        chainException = exc;
        try {
            filter.doFilter(request, response, chain);
            fail();
        } catch (ServletException e) {
            assertEquals(exc, e.getCause());
        }

        verify(response).setStatus(expectedStatus);
        verify(response, never()).sendError(anyInt());
        verify(response, never()).sendError(anyInt(), anyString());

        assertEquals("", bout.toString()); // output was suppressed
    }

}
