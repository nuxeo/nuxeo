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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.NotImplementedException;

/**
 * Fake WebDAV Request for tests.
 *
 * @author tiry
 */
public class FakeRequest implements HttpServletRequest {

    protected final Map<String, Object> attributes = new HashMap<String, Object>();

    protected final Map<String, String> parameters = new HashMap<String, String>();

    protected final Map<String, String> headers = new HashMap<String, String>();

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
            if (tuple.length>1) {
                parameters.put(tuple[0], tuple[1]);
            }
            else {
                parameters.put(tuple[0], "");
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
            if (tuple.length>1) {
                if (tuple[1].endsWith("\n")) {
                    tuple[1]= tuple[1].substring(0,tuple[1].length()-2);
                }
                parameters.put(tuple[0], tuple[1]);
            } else {
                parameters.put(tuple[0], "");
            }
        }
        this.in = new FakeServletInputStream(data);
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
        return path;
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
        throw new NotImplementedException();
    }

    public HttpSession getSession(boolean create) {
        return null;
    }

    public Principal getUserPrincipal() {
        return null;
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
        String cl = headers.get("Content-Length");
        if (cl!=null) {
            return Integer.parseInt(cl);
        }
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
        if (parameters==null) {
            return null;
        }
        return parameters.get(name);
    }

    public Map<String, String> getParameterMap() {
        return parameters;
    }

    public Enumeration<String> getParameterNames() {
        return new SetEnumeration(parameters.keySet());
    }

    public String[] getParameterValues(String name) {
        Collection<String> values = parameters.values();
        return values.toArray(new String[values.size()]);
    }

    public String getProtocol() {
        return "http";
    }

    public BufferedReader getReader() throws IOException {
        return null;
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
        return "localhost";
    }

    public int getServerPort() {
        return 80;
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
        //throw new NotImplementedException();
    }

}
