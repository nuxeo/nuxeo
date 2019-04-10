/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.wss.servlet;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.wss.MSWSSConsts;
import org.nuxeo.wss.WSSConfig;

public class WSSStaticResponse {

    protected HttpServletResponse httpResponse;

    protected boolean processed = false;

    protected String contentType = null;

    protected InputStream additionnalStream;

    public WSSStaticResponse(HttpServletResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public void processIfNeeded() throws ServletException, IOException {
        if (!processed) {
            process();
        }
    }

    public void process() throws ServletException, IOException {
        if (processed) {
            throw new ServletException("process called twice on WSSResponse");
        }
        processHeaders();
        processRender();
        processed = true;
    }

    public void addBinaryStream(InputStream stream) {
        this.additionnalStream = stream;
    }

    protected void processHeaders() {
        getHttpResponse().setHeader(MSWSSConsts.TSSERVER_VERSION_HEADER, WSSConfig.instance().getTSServerVersion());
        getHttpResponse().setHeader("Set-Cookie", "WSS_KeepSessionAuthenticated=80; path=/");
        // getHttpResponse().setHeader("Server","Microsoft-IIS/6.0");
        getHttpResponse().setHeader("X-Powered-By", "ASP.NET");

        if (contentType == null) {
            getHttpResponse().setHeader("Content-type", getDefaultContentType());
        } else {
            getHttpResponse().setHeader("Content-type", contentType);
        }
    }

    protected String getDefaultContentType() {
        return "text/plain";
    }

    protected void processRender() throws IOException {
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public void setContentType(String ct) {
        this.contentType = ct;
    }

}
