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

package org.nuxeo.ecm.webengine.servlet;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class NoBodyResponse implements HttpServletResponse {
    private final HttpServletResponse resp;
    private final NoBodyOutputStream noBody;
    private PrintWriter writer;
    private boolean didSetContentLength;

    public NoBodyResponse(HttpServletResponse r) {
        resp = r;
        noBody = new NoBodyOutputStream();
    }

    public void setContentLength() {
        if (!didSetContentLength) {
            resp.setContentLength(noBody.getContentLength());
        }
    }

    //
    // SERVLET RESPONSE interface methods
    //

    public void setContentLength(int len) {
        resp.setContentLength(len);
        didSetContentLength = true;
    }

    public void setCharacterEncoding(String charset) {
        resp.setCharacterEncoding(charset);
    }

    public void setContentType(String type) {
        resp.setContentType(type);
    }

    public String getContentType() {
        return resp.getContentType();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return noBody;
    }

    public String getCharacterEncoding() {
        return resp.getCharacterEncoding();
    }

    public PrintWriter getWriter() throws UnsupportedEncodingException {
        if (writer == null) {
            OutputStreamWriter w = new OutputStreamWriter(noBody,
                    getCharacterEncoding());
            writer = new PrintWriter(w);
        }
        return writer;
    }

    public void setBufferSize(int size) {
        resp.setBufferSize(size);
    }

    public int getBufferSize() {
        return resp.getBufferSize();
    }

    public void reset() {
        resp.reset();
    }

    public void resetBuffer() {
        resp.resetBuffer();
    }

    public boolean isCommitted() {
        return resp.isCommitted();
    }

    public void flushBuffer() throws IOException {
        resp.flushBuffer();
    }

    public void setLocale(Locale loc) {
        resp.setLocale(loc);
    }

    public Locale getLocale() {
        return resp.getLocale();
    }

    //
    // HTTP SERVLET RESPONSE interface methods
    //

    public void addCookie(Cookie cookie) {
        resp.addCookie(cookie);
    }

    public boolean containsHeader(String name) {
        return resp.containsHeader(name);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setStatus(int sc, String sm) {
        resp.setStatus(sc, sm);
    }

    public void setStatus(int sc) {
        resp.setStatus(sc);
    }

    public void setHeader(String name, String value) {
        resp.setHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
        resp.setIntHeader(name, value);
    }

    public void setDateHeader(String name, long date) {
        resp.setDateHeader(name, date);
    }

    public void sendError(int sc, String msg) throws IOException {
        resp.sendError(sc, msg);
    }

    public void sendError(int sc) throws IOException {
        resp.sendError(sc);
    }

    public void sendRedirect(String location) throws IOException {
        resp.sendRedirect(location);
    }

    public String encodeURL(String url) {
        return resp.encodeURL(url);
    }

    public String encodeRedirectURL(String url) {
        return resp.encodeRedirectURL(url);
    }

    public void addHeader(String name, String value) {
        resp.addHeader(name, value);
    }

    public void addDateHeader(String name, long value) {
        resp.addDateHeader(name, value);
    }

    public void addIntHeader(String name, int value) {
        resp.addIntHeader(name, value);
    }

    /**
     * @deprecated As of Version 2.1, replaced by
     * {@link HttpServletResponse#encodeURL}.
     */
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    /**
     * @deprecated As of Version 2.1, replaced by
     * {@link HttpServletResponse#encodeRedirectURL}.
     */
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
    }

}


/**
 * Servlet output stream that gobbles up all its data.
 */
class NoBodyOutputStream extends ServletOutputStream {
    private static final String LSTRING_FILE =
            "javax.servlet.http.LocalStrings";
    private static final ResourceBundle lStrings =
            ResourceBundle.getBundle(LSTRING_FILE);

    private int contentLength = 0;

    int getContentLength() {
        return contentLength;
    }

    @Override
    public void write(int b) {
        contentLength++;
    }

    @Override
    public void write(byte[] buf, int offset, int len)
            throws IOException {
        if (len >= 0) {
            contentLength += len;
        } else {
            // XXX
            // isn't this really an IllegalArgumentException?

            String msg = lStrings.getString("err.io.negativelength");
            throw new IOException("negative length");
        }
    }

}
