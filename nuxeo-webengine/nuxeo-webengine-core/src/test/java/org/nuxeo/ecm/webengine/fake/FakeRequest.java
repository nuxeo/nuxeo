/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.fake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.NotImplementedException;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;

/**
 * Fake WebDAV Request for tests.
 *
 * @author tiry
 */
public class FakeRequest implements HttpServletRequest {

    protected final Map<String, Object> attributes = new HashMap<String, Object>();

    protected final Map<String, String[]> parameters = new HashMap<String, String[]>();

    protected final Map<String, String> headers = new HashMap<String, String>();

    protected final String method;

    protected final String path;

    protected String queryString;

    protected FakeServletInputStream in;

    protected String ct = "application/octetstream";

    protected final FakeSession session = new FakeSession();

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
            String[] val = parameters.get(tuple[0]);
            String[] ar = null;
            if (val == null) {
                ar = new String[] {tuple[1]};
            } else {
                // FIXME: can't work as ar == null here
                String[] tmp = new String[ar.length+1];
                System.arraycopy(ar, 0, tmp, 0, ar.length);
                tmp[ar.length] = tuple[1];
            }
            parameters.put(tuple[0], ar);
        }
    }

    // specific code

    public void setContentType(String ct) {
        this.ct = ct;
    }

    public void addHeader(String headerName, String value) {
        headers.put(headerName, value);
    }

    public void setStream(FakeServletInputStream in) {
        this.in = in;
    }

    // interface implementation
    public String getAuthType() {
        throw new NotImplementedException();
    }

    public String getContextPath() {
        return null;
    }

    public Cookie[] getCookies() {
        throw new NotImplementedException();
    }

    public long getDateHeader(String name) {
        throw new NotImplementedException();
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Enumeration<String> getHeaderNames() {
        return new SetEnumeration(headers.keySet());
    }

    public Enumeration<String> getHeaders(String name) {
        throw new NotImplementedException();
    }

    public int getIntHeader(String name) {
        throw new NotImplementedException();
    }

    public String getMethod() {
        return method;
    }

    public String getPathInfo() {
        return path;
    }

    public String getPathTranslated() {
        throw new NotImplementedException();
    }

    public String getQueryString() {
        return queryString;
    }

    public String getRemoteUser() {
        throw new NotImplementedException();
    }

    public String getRequestURI() {
        return "/nuxeo/site" + path;
    }

    public StringBuffer getRequestURL() {
        StringBuffer sb = new StringBuffer();
        sb.append("http://127.0.0.1:8080");
        sb.append(getRequestURI());
        return sb;
    }

    public String getRequestedSessionId() {
        throw new NotImplementedException();
    }

    public String getServletPath() {
        return "/dav";
    }

    public HttpSession getSession() {
        return session;
    }

    public HttpSession getSession(boolean create) {
        return session;
    }

    public Principal getUserPrincipal() {
        return new UserPrincipal("Administrator");
    }

    public boolean isRequestedSessionIdFromCookie() {
        throw new NotImplementedException();
    }

    public boolean isRequestedSessionIdFromURL() {
        throw new NotImplementedException();
    }

    public boolean isRequestedSessionIdFromUrl() {
        throw new NotImplementedException();
    }

    public boolean isRequestedSessionIdValid() {
        throw new NotImplementedException();
    }

    public boolean isUserInRole(String role) {
        throw new NotImplementedException();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        return new SetEnumeration(attributes.keySet());
    }

    public String getCharacterEncoding() {
        throw new NotImplementedException();
    }

    public int getContentLength() {
        return 0;
    }

    public String getContentType() {
        return ct;
    }

    public ServletInputStream getInputStream() throws IOException {
        return in;
    }

    public String getLocalAddr() {
        throw new NotImplementedException();
    }

    public String getLocalName() {
        throw new NotImplementedException();
    }

    public int getLocalPort() {
        throw new NotImplementedException();
    }

    public Locale getLocale() {
        throw new NotImplementedException();
    }

    public Enumeration<String> getLocales() {
        throw new NotImplementedException();
    }

    public String getParameter(String name) {
        String[] vals = parameters.get(name);
        if (vals != null && vals.length > 0) {
            return vals[0];
        }
        return null;
    }

    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    public Enumeration<String> getParameterNames() {
        return new SetEnumeration(parameters.keySet());
    }

    public String[] getParameterValues(String name) {
        Collection<String[]> values = parameters.values();
        List<String> result = new ArrayList<String>();
        for (String[] ar : values) {
            result.addAll(Arrays.asList(ar));
        }
        return result.toArray(new String[result.size()]);
    }

    public String getProtocol() {
        return "http";
    }

    public BufferedReader getReader() throws IOException {
        throw new NotImplementedException();
    }

    public String getRealPath(String path) {
        throw new NotImplementedException();
        // return null;
    }

    public String getRemoteAddr() {
        throw new NotImplementedException();
    }

    public String getRemoteHost() {
        throw new NotImplementedException();
    }

    public int getRemotePort() {
        throw new NotImplementedException();
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        throw new NotImplementedException();
    }

    public String getScheme() {
        return "http";
    }

    public String getServerName() {
        throw new NotImplementedException();
    }

    public int getServerPort() {
        throw new NotImplementedException();
    }

    public boolean isSecure() {
        throw new NotImplementedException();
    }

    public void removeAttribute(String name) {
        throw new NotImplementedException();
    }

    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    public void setCharacterEncoding(String env)
            throws UnsupportedEncodingException {
        throw new NotImplementedException();
    }

}
