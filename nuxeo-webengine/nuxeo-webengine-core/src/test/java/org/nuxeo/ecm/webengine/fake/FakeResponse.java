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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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

    protected final Map<String, String> headers = new HashMap<String, String>();

    protected final ByteArrayOutputStream out = new ByteArrayOutputStream();

    protected final FakeServletOutputStream fout = new FakeServletOutputStream(out);

    protected final PrintWriter printer = new PrintWriter(out);

    protected int status;

    protected String output;

    public int getStatus() {
        return status == 0 ? 200 : status;
    }

    public String getOutput() throws UnsupportedEncodingException {
        if (output == null) {
            printer.flush();
            output = out.toString("UTF-8");
        }
        return output;
    }

    public void addCookie(Cookie cookie) {
    }

    public void addDateHeader(String name, long date) {
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public void addIntHeader(String name, int value) {
    }

    public boolean containsHeader(String name) {
        return false;
    }

    public String encodeRedirectURL(String url) {
        return null;
    }

    public String encodeRedirectUrl(String url) {
        return null;
    }

    public String encodeURL(String url) {
        return null;
    }

    public String encodeUrl(String url) {
        return null;
    }

    public void sendError(int sc) throws IOException {
        status = sc;
        fout.print("ERROR: "+sc);
    }

    public void sendError(int sc, String msg) throws IOException {
        status = sc;
        fout.print("ERROR: "+sc+" - "+msg);
    }

    public void sendRedirect(String location) throws IOException {
    }

    public void setDateHeader(String name, long date) {
        throw new NotImplementedException();
    }

    public void setHeader(String name, String value) {
        addHeader(name, value);
    }

    public void setIntHeader(String name, int value) {
    }

    public void setStatus(int sc) {
        status = sc;
    }

    public void setStatus(int sc, String sm) {
        status = sc;
    }

    public void flushBuffer() throws IOException {
    }

    public int getBufferSize() {
        return 0;
    }

    public String getCharacterEncoding() {
        return "utf-8";
    }

    public String getContentType() {
        return null;
    }

    public Locale getLocale() {
        return null;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return fout;
    }

    public PrintWriter getWriter() throws IOException {
        return printer;
    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {
        throw new NotImplementedException();
    }

    public void resetBuffer() {
        throw new NotImplementedException();
    }

    public void setBufferSize(int size) {
        throw new NotImplementedException();
    }

    public void setCharacterEncoding(String charset) {
    }

    public void setContentLength(int len) {
    }

    public void setContentType(String type) {
    }

    public void setLocale(Locale loc) {
    }

}
