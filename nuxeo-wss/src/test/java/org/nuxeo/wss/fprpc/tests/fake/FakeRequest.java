/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.wss.fprpc.tests.fake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.commons.lang.NotImplementedException;

/**
 * Fake WebDAV Request for tests.
 *
 * @author tiry
 */
public class FakeRequest implements HttpServletRequest {

    protected final Map<String, Object> attributes = new HashMap<>();

    protected final Map<String, String[]> parameters = new HashMap<>();

    protected final Map<String, String> headers = new HashMap<>();

    protected final String method;

    protected final String path;

    protected String queryString;

    protected FakeServletInputStream in;

    protected String ct = "application/octetstream";

    public FakeRequest(String method, String subUrl, InputStream is) {
        this.method = method;

        String[] urlParts = subUrl.split("\\?");
        path = urlParts[0];
        if (urlParts.length > 1) {
            queryString = urlParts[1];
            initParameters();
        }
        if (is != null) {
            in = new FakeServletInputStream(is);
        }
    }

    public FakeRequest(String method, String subUrl) {
        this(method, subUrl, null);
    }

    private void initParameters() {
        parameters.clear();
        if (queryString == null) {
            return;
        }

        String[] params = queryString.split("&");
        for (String element : params) {
            String[] tuple = element.split("=");
            if (tuple.length > 1) {
                parameters.put(tuple[0], new String[] { tuple[1] });
            } else {
                parameters.put(tuple[0], new String[] { "" });
            }
        }
    }

    // specific code

    public void setContentType(String ct) {
        this.ct = ct;
    }

    public void addHeader(String headerName, String value) {
        headers.put(headerName, value);
        if ("Content-Type".equals(headerName)) {
            ct = value;
        }
    }

    public void setStream(FakeServletInputStream in) {
        this.in = in;
    }

    public void setFormEncodedBody(String data) throws IOException {
        String pData = URLDecoder.decode(data);
        String[] params = pData.split("&");
        for (String element : params) {
            String[] tuple = element.split("=");
            if (tuple.length > 1) {
                if (tuple[1].endsWith("\n")) {
                    tuple[1] = tuple[1].substring(0, tuple[1].length() - 2);
                }
                parameters.put(tuple[0], new String[] { tuple[1] });
            } else {
                parameters.put(tuple[0], new String[] { "" });
            }
        }
        this.in = new FakeServletInputStream(data);
    }

    // interface implementation
    @Override
    public String getAuthType() {
        throw new NotImplementedException();
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        throw new NotImplementedException();
    }

    @Override
    public long getDateHeader(String name) {
        throw new NotImplementedException();
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return new SetEnumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        throw new NotImplementedException();
    }

    @Override
    public int getIntHeader(String name) {
        throw new NotImplementedException();
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getPathInfo() {
        return path;
    }

    @Override
    public String getPathTranslated() {
        throw new NotImplementedException();
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getRemoteUser() {
        throw new NotImplementedException();
    }

    @Override
    public String getRequestURI() {
        return path;
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer sb = new StringBuffer();
        sb.append("http://127.0.0.1:8080");
        sb.append(getRequestURI());
        return sb;
    }

    @Override
    public String getRequestedSessionId() {
        throw new NotImplementedException();
    }

    @Override
    public String getServletPath() {
        return "/dav";
    }

    @Override
    public HttpSession getSession() {
        throw new NotImplementedException();
    }

    @Override
    public HttpSession getSession(boolean create) {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new NotImplementedException();
    }

    @Deprecated
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isUserInRole(String role) {
        throw new NotImplementedException();
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new SetEnumeration(attributes.keySet());
    }

    @Override
    public String getCharacterEncoding() {
        throw new NotImplementedException();
    }

    @Override
    public int getContentLength() {
        String cl = headers.get("Content-Length");
        if (cl != null) {
            return Integer.parseInt(cl);
        }
        return 0;
    }

    @Override
    public String getContentType() {
        return ct;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return in;
    }

    @Override
    public String getLocalAddr() {
        throw new NotImplementedException();
    }

    @Override
    public String getLocalName() {
        throw new NotImplementedException();
    }

    @Override
    public int getLocalPort() {
        throw new NotImplementedException();
    }

    @Override
    public Locale getLocale() {
        throw new NotImplementedException();
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new NotImplementedException();
    }

    @Override
    public String getParameter(String name) {
        if (parameters == null || parameters.get(name) == null
                || parameters.get(name).length == 0) {
            return null;
        }
        return parameters.get(name)[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return new SetEnumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public String getProtocol() {
        return "http"; // FIXME Should be of the form "HTTP/1.0"
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }

    @Deprecated
    @Override
    public String getRealPath(String path) {
        throw new NotImplementedException();
        // return null;
    }

    @Override
    public String getRemoteAddr() {
        throw new NotImplementedException();
    }

    @Override
    public String getRemoteHost() {
        throw new NotImplementedException();
    }

    @Override
    public int getRemotePort() {
        throw new NotImplementedException();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        throw new NotImplementedException();
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return "localhost";
    }

    @Override
    public int getServerPort() {
        return 80;
    }

    @Override
    public boolean isSecure() {
        throw new NotImplementedException();
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    @Override
    public void setCharacterEncoding(String env)
            throws UnsupportedEncodingException {
    }

    @Override
    public ServletContext getServletContext() {
        throw new NotImplementedException();
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new NotImplementedException();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest,
            ServletResponse servletResponse) throws IllegalStateException {
        throw new NotImplementedException();
    }

    @Override
    public boolean isAsyncStarted() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isAsyncSupported() {
        throw new NotImplementedException();
    }

    @Override
    public AsyncContext getAsyncContext() {
        throw new NotImplementedException();
    }

    @Override
    public DispatcherType getDispatcherType() {
        throw new NotImplementedException();
    }

    @Override
    public boolean authenticate(HttpServletResponse response)
            throws IOException, ServletException {
        throw new NotImplementedException();
    }

    @Override
    public void login(String username, String password) throws ServletException {
    }

    @Override
    public void logout() throws ServletException {
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new NotImplementedException();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        throw new NotImplementedException();
    }

}
