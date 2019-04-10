/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.wss.fprpc.tests.fake;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.NotImplementedException;

/**
 * Fake WebDAV Response for tests.
 *
 * @author tiry
 */
public class FakeResponse implements HttpServletResponse {

    protected final Map<String, String> headers = new HashMap<>();

    protected final OutputStream out = new ByteArrayOutputStream();

    protected final FakeServletOutputStream fout = new FakeServletOutputStream(
            out);

    protected final PrintWriter printer = new PrintWriter(out);

    protected int status;

    protected String output;

    @Override
    public int getStatus() {
        return status;
    }

    public String getOutput() throws UnsupportedEncodingException {
        if (output == null) {
            printer.flush();
            output = ((ByteArrayOutputStream) out).toString("UTF-8");
        }
        return output;
    }

    @Override
    public void addCookie(Cookie cookie) {
    }

    @Override
    public void addDateHeader(String name, long date) {
    }

    @Override
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
    }

    @Override
    public boolean containsHeader(String name) {
        return false;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Deprecated
    @Override
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Deprecated
    @Override
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    public void sendError(int sc) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        throw new NotImplementedException();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
    }

    @Override
    public void setDateHeader(String name, long date) {
        throw new NotImplementedException();
    }

    @Override
    public void setHeader(String name, String value) {
        addHeader(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
    }

    @Override
    public void setStatus(int sc) {
        status = sc;
    }

    @Deprecated
    @Override
    public void setStatus(int sc, String sm) {
        status = sc;
    }

    @Override
    public void flushBuffer() throws IOException {
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        return "utf-8";
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return fout;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return printer;
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
        throw new NotImplementedException();
    }

    @Override
    public void resetBuffer() {
        throw new NotImplementedException();
    }

    @Override
    public void setBufferSize(int size) {
        // NOP
    }

    @Override
    public void setCharacterEncoding(String charset) {
    }

    @Override
    public void setContentLength(int len) {
    }

    @Override
    public void setContentType(String type) {
    }

    @Override
    public void setLocale(Locale loc) {
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

}
