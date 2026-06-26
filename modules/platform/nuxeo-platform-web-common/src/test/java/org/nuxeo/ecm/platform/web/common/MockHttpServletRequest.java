/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.web.common;

import static org.apache.commons.lang3.ObjectUtils.anyNotNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.mockito.Mockito;

/**
 * @since 2025.0
 */
public class MockHttpServletRequest {

    protected final HttpServletRequest mock;

    protected final Map<String, Object> attributes;

    protected final HttpSession session;

    protected final Map<String, Object> sessionAttributes;

    /**
     * Backing store for request parameters. Drives both {@link HttpServletRequest#getParameter(String)} and
     * {@link HttpServletRequest#getParameterMap()} so callers using the new OpenSAML 5.2.2+ code path (which reads
     * {@code getParameterMap().keySet().stream()}) get a consistent view.
     *
     * @since 2025.22
     */
    protected final Map<String, String[]> parameters;

    protected List<Cookie> cookies;

    protected MockHttpServletRequest(HttpServletRequest request) {
        mock = request;
        when(mock.getLocale()).thenReturn(Locale.ENGLISH);
        // initialize attributes
        attributes = new HashMap<>();
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            attributes.put(key, value);
            return null;
        }).when(mock).setAttribute(anyString(), any());
        when(mock.getAttribute(anyString())).thenAnswer(
                invocation -> attributes.get(invocation.<String> getArgument(0)));
        when(mock.getAttributeNames()).thenAnswer(invocation -> Collections.enumeration(attributes.keySet()));
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            attributes.remove(key);
            return null;
        }).when(mock).removeAttribute(anyString());
        // initialize parameters
        parameters = new HashMap<>();
        when(mock.getParameter(anyString())).thenAnswer(invocation -> {
            String[] values = parameters.get(invocation.<String> getArgument(0));
            return values != null && values.length > 0 ? values[0] : null;
        });
        when(mock.getParameterValues(anyString())).thenAnswer(
                invocation -> parameters.get(invocation.<String> getArgument(0)));
        when(mock.getParameterMap()).thenAnswer(invocation -> Collections.unmodifiableMap(parameters));
        when(mock.getParameterNames()).thenAnswer(invocation -> Collections.enumeration(parameters.keySet()));
        // initialize session
        session = Mockito.mock(HttpSession.class, RETURNS_DEEP_STUBS);
        when(mock.getSession()).thenReturn(session);
        when(mock.getSession(anyBoolean())).thenReturn(session);
        // initialize session attributes
        sessionAttributes = new HashMap<>();
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            return sessionAttributes.get(key);
        }).when(session).getAttribute(anyString());
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            Object value = i.getArguments()[1];
            sessionAttributes.put(key, value);
            return null;
        }).when(session).setAttribute(anyString(), any());
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            sessionAttributes.remove(key);
            return null;
        }).when(session).removeAttribute(anyString());
        doAnswer(i -> {
            sessionAttributes.clear();
            return null;
        }).when(session).invalidate();
    }

    public static MockHttpServletRequest init() {
        return builder().build();
    }

    /**
     * @return a new {@link MockHttpServletRequest} with a method
     */
    public static MockHttpServletRequest init(String method) {
        return builder().method(method).build();
    }

    /**
     * @return a new {@link MockHttpServletRequest} with a method and a complete requestUrl
     */
    public static MockHttpServletRequest init(String method, String requestUrl) {
        return builder().method(method).requestUrl(requestUrl).build();
    }

    /**
     * @return a {@link Builder} to build a more complex {@link MockHttpServletRequest}
     */
    public static Builder builder() {
        return new Builder();
    }

    // Mocking APIs

    public MockHttpServletRequest whenGetAttributeThenReturn(String key, Object value) {
        // as we store attributes to be able to retrieve them, just put it in the map
        attributes.put(key, value);
        return this;
    }

    public MockHttpServletRequest whenGetCookieThenReturn(String name, String value) {
        if (cookies == null) {
            cookies = new ArrayList<>();
            when(mock.getCookies()).thenAnswer(invocation -> cookies.toArray(Cookie[]::new));
        }
        cookies.add(new Cookie(name, value));
        return this;
    }

    public MockHttpServletRequest whenGetHeaderThenReturn(String name, String value) {
        when(mock.getHeader(name)).thenReturn(value);
        return this;
    }

    public MockHttpServletRequest whenGetParameterThenReturn(String name, String value) {
        // store in the backing map so getParameter, getParameterValues, getParameterMap and getParameterNames stay
        // consistent (e.g. OpenSAML 5.2.2+ reads getParameterMap().keySet().stream() in SAMLBindingSupport)
        parameters.put(name, new String[] { value });
        return this;
    }

    public MockHttpServletRequest whenGetSessionAttributeThenReturn(String key, Object value) {
        // as we store session attributes to be able to retrieve them, just put it in the map
        sessionAttributes.put(key, value);
        return this;
    }

    /**
     * @return the mock this handler holds
     */
    public HttpServletRequest mock() {
        return mock;
    }

    // APIs to get data set to the request

    /**
     * @return the request attribute that we get from {@link HttpServletRequest#getAttribute(String)}
     */
    @SuppressWarnings("unchecked")
    public <R> R getAttribute(String key) {
        return (R) attributes.get(key);
    }

    /**
     * @return the request attributes held by the request
     */
    public Map<String, Object> getAttributes() {
        return Map.copyOf(attributes);
    }

    /**
     * @return the session attribute that we get from {@link HttpServletRequest#getSession()} then
     *         {@link HttpSession#getAttribute(String)}
     */
    @SuppressWarnings("unchecked")
    public <R> R getSessionAttributeValue(String name) {
        return (R) sessionAttributes.get(name);
    }

    public static class Builder {

        protected HttpServletRequest request;

        protected String requestUrl;

        protected String scheme;

        protected String host;

        protected Integer port;

        protected String requestUri;

        protected String contextPath;

        protected String servletPath;

        protected String pathInfo;

        protected Builder() {
            this.request = Mockito.mock(HttpServletRequest.class, RETURNS_DEEP_STUBS);
        }

        public Builder method(String method) {
            when(request.getMethod()).thenReturn(method);
            return this;
        }

        public Builder requestUrl(String requestUrl) {
            if (anyNotNull(scheme, host, port, requestUri, contextPath, servletPath, pathInfo)) {
                throw new IllegalStateException(
                        "You can not use requestUrl method and scheme, host, port, requestUri, contextPath, servletPath, pathInfo methods");
            }
            this.requestUrl = requestUrl;
            return this;
        }

        public Builder scheme(String scheme) {
            if (anyNotNull(requestUrl)) {
                throw new IllegalStateException("You can not use scheme method and requestUrl method");
            }
            this.scheme = scheme;
            return this;
        }

        public Builder host(String host) {
            if (anyNotNull(requestUrl)) {
                throw new IllegalStateException("You can not use host method and requestUrl method");
            }
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            if (anyNotNull(requestUrl)) {
                throw new IllegalStateException("You can not use port method and requestUrl method");
            }
            this.port = port;
            return this;
        }

        public Builder requestUri(String requestUri) {
            if (anyNotNull(requestUrl, contextPath, servletPath, pathInfo)) {
                throw new IllegalStateException(
                        "You can not use requestUri method and requestUrl, contextPath, servletPath, pathInfo methods");
            }
            this.requestUri = requestUri;
            return this;
        }

        public Builder contextPath(String contextPath) {
            if (anyNotNull(requestUrl, requestUri)) {
                throw new IllegalStateException(
                        "You can not use contextPath method and requestUrl, requestUri methods");
            }
            this.contextPath = contextPath;
            return this;
        }

        public Builder servletPath(String servletPath) {
            if (anyNotNull(requestUrl, requestUri)) {
                throw new IllegalStateException(
                        "You can not use servletPath method and requestUrl, requestUri methods");
            }
            this.servletPath = servletPath;
            return this;
        }

        public Builder pathInfo(String pathInfo) {
            if (anyNotNull(requestUrl, requestUri)) {
                throw new IllegalStateException("You can not use pathInfo method and requestUrl, requestUri methods");
            }
            this.pathInfo = pathInfo;
            return this;
        }

        public MockHttpServletRequest build() {
            try {
                URI requestURI;
                URL requestURL;
                if (requestUrl != null) {
                    requestURI = new URI(requestUrl);
                    requestURL = requestURI.toURL();
                } else if (requestUri != null) {
                    requestURI = new URI(requestUri);
                    String baseUrl = String.format("%s://%s:%s", defaultIfNull(scheme, "http"),
                            defaultIfNull(host, "localhost"), defaultIfNull(port, "8080"));
                    requestURL = new URI(baseUrl + requestUri).toURL();
                } else {
                    var requestUriBuilder = new StringBuilder();
                    if (contextPath != null) {
                        requestUriBuilder.append(contextPath);
                        when(request.getContextPath()).thenReturn(contextPath);
                    }
                    if (servletPath != null) {
                        requestUriBuilder.append(servletPath);
                        when(request.getServletPath()).thenReturn(servletPath);
                    }
                    if (pathInfo != null) {
                        requestUriBuilder.append(pathInfo);
                        when(request.getPathInfo()).thenReturn(pathInfo);
                    }
                    var requestUriLocal = requestUriBuilder.toString();
                    requestURI = new URI(requestUriLocal);
                    String baseUrl = String.format("%s://%s:%s", defaultIfNull(scheme, "http"),
                            defaultIfNull(host, "localhost"), defaultIfNull(port, "8080"));
                    requestURL = new URI(baseUrl + requestUriLocal).toURL();
                }
                when(request.getScheme()).thenReturn(requestURL.getProtocol());
                when(request.getServerName()).thenReturn(requestURL.getHost());
                when(request.getServerPort()).thenReturn(requestURL.getPort());
                when(request.getRequestURI()).thenReturn(requestURI.getRawPath());
                when(request.getQueryString()).thenReturn(requestURI.getRawQuery());
                when(request.getRequestURL()).thenReturn(new StringBuffer(requestURI.toString()));
                return new MockHttpServletRequest(request);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to build MockHttpServletRequest", e);
            }
        }
    }
}
