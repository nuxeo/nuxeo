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
package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/exception-handling-service.xml")
public class TestNuxeoExceptionFilter {

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    protected NuxeoExceptionFilter filter;

    protected DummyFilterChain chain;

    protected Exception chainException;

    public class DummyFilterChain implements FilterChain {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            try {
                throw chainException;
            } catch (IOException | ServletException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                fail(e.toString());
            }
        }
    }

    @Before
    public void setUp() {
        filter = new NuxeoExceptionFilter();
        chain = new DummyFilterChain();
    }

    protected Map<String, Object> mockRequestAttributes(HttpServletRequest request) {
        Map<String, Object> attributes = new HashMap<>();
        // getAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            return attributes.get(key);
        }).when(request).getAttribute(anyString());
        // setAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            Object value = i.getArguments()[1];
            attributes.put(key, value);
            return null;
        }).when(request).setAttribute(anyString(), any());
        // removeAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            attributes.remove(key);
            return null;
        }).when(request).removeAttribute(anyString());
        // getAttributeNames
        doAnswer(i -> attributes.keySet()).when(request).getAttributeNames();
        return attributes;
    }

    @Test
    public void testNuxeoException() throws IOException, ServletException {
        doTestException(new NuxeoException("oops", 456), 456, "oops", "oops", null);
    }

    @Test
    public void testNuxeoExceptionAsCause() throws IOException, ServletException {
        doTestException(new ServletException(new NuxeoException("oops", 456)), 456, "oops", "oops", null);
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "ERROR")
    public void testRuntimeException() throws IOException, ServletException {
        doTestException(new RuntimeException("oops"), SC_INTERNAL_SERVER_ERROR, "Internal Server Error",
                "oops", "java.lang.RuntimeException: oops");
    }

    @SuppressWarnings("resource")
    protected void doTestException(Exception exc, int expectedStatus, String expectedJsonMessage,
            String expectedMessage, String expectedLog) throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, Object> requestAttributes = mockRequestAttributes(request);
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF_8), true); // NOSONAR
        when(response.getWriter()).thenReturn(writer);

        logCaptureResult.clear();

        chainException = exc;
        filter.doFilter(request, response, chain);

        assertEquals("{\"entity-type\":\"exception\",\"status\":" + expectedStatus + ",\"message\":\""
                + expectedJsonMessage + "\"}", out.toString(UTF_8));

        List<String> expectedEvents = expectedLog == null ? Collections.emptyList()
                : Collections.singletonList(expectedLog);
        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertEquals(expectedEvents, caughtEvents);

        Map<String, Object> expectedRequestAttributes = new HashMap<>();
        expectedRequestAttributes.put("NuxeoExceptionHandlerMarker", true);
        expectedRequestAttributes.put("nuxeo.disable.redirect.wrapper", true);
        expectedRequestAttributes.put("user_message", "Error.Unknown");
        expectedRequestAttributes.put("exception_message", expectedMessage);
        expectedRequestAttributes.put("securityError", false);
        expectedRequestAttributes.put("isDevModeSet", false);
        assertEquals(expectedRequestAttributes, requestAttributes);

        verify(request, atLeastOnce()).getAttribute(anyString());
        verify(request, atLeastOnce()).setAttribute(anyString(), any());
        verify(request, atLeastOnce()).getHeader(anyString());
        verifyNoMoreInteractions(request);

        verify(response).isCommitted();
        verify(response).reset();
        verify(response).setStatus(expectedStatus);
        verify(response).setContentType(anyString());
        verify(response).getWriter();
        verifyNoMoreInteractions(response);
    }

}
