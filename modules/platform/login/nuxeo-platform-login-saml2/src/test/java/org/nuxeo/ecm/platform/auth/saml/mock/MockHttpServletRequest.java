/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.auth.saml.mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @since 2023.0
 */
public class MockHttpServletRequest {

    protected final HttpServletRequest mock;

    protected Map<String, Object> attributes;

    protected Map<String, Object> sessionAttributes;

    protected List<Cookie> cookies;

    protected MockHttpServletRequest(HttpServletRequest request) {
        mock = request;
        when(mock.getLocale()).thenReturn(Locale.ENGLISH);
    }

    public static MockHttpServletRequest init() {
        var request = Mockito.mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        return new MockHttpServletRequest(request);
    }

    public static MockHttpServletRequest init(String method, String requestURLString) {
        try {
            var request = Mockito.mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
            when(request.getMethod()).thenReturn(method);
            when(request.getRequestURL()).thenReturn(new StringBuffer(requestURLString));
            var requestURL = new URL(requestURLString);
            when(request.getServerName()).thenReturn(requestURL.getHost());
            when(request.getServerPort()).thenReturn(requestURL.getPort());
            when(request.getScheme()).thenReturn(requestURL.getProtocol());
            return new MockHttpServletRequest(request);
        } catch (MalformedURLException e) {
            throw new AssertionError("Failed to build MockHttpServletRequest", e);
        }
    }

    public MockHttpServletRequest withAttributes() {
        attributes = new HashMap<>();
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            attributes.put(key, value);
            return null;
        }).when(mock).setAttribute(anyString(), any());
        when(mock.getAttribute(anyString())).thenAnswer(
                invocation -> attributes.get(invocation.<String> getArgument(0)));
        return this;
    }

    public MockHttpServletRequest withGetCookieThenReturn(String name, String value) {
        if (cookies == null) {
            cookies = new ArrayList<>();
            when(mock.getCookies()).thenAnswer(invocation -> cookies.toArray(Cookie[]::new));
        }
        cookies.add(new Cookie(name, value));
        return this;
    }

    public MockHttpServletRequest whenGetParameterThenReturn(String name, String value) {
        when(mock.getParameter(name)).thenReturn(value);
        return this;
    }

    public HttpServletRequest mock() {
        return mock;
    }

    @SuppressWarnings("unchecked")
    public <R> R getSessionAttributeValue(String name) {
        if (sessionAttributes == null) {
            var sessionAttributeNamesCaptor = ArgumentCaptor.forClass(String.class);
            var sessionAttributeValuesCaptor = ArgumentCaptor.forClass(Object.class);
            verify(mock.getSession(anyBoolean())).setAttribute(sessionAttributeNamesCaptor.capture(),
                    sessionAttributeValuesCaptor.capture());
            sessionAttributes = IntStream.range(0, sessionAttributeNamesCaptor.getAllValues().size())
                                         .boxed()
                                         .collect(Collectors.toMap(sessionAttributeNamesCaptor.getAllValues()::get,
                                                 sessionAttributeValuesCaptor.getAllValues()::get));
        }
        return (R) sessionAttributes.get(name);
    }
}
