/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.wss.MSWSSConsts;
import org.nuxeo.wss.WSSConfig;
import org.nuxeo.wss.fm.FreeMarkerRenderer;

public class WSSResponse {

    protected HttpServletResponse httpResponse;

    protected boolean processed=false;

    protected boolean bufferizeRendering = true;

    protected static final int BUFFER_SIZE = 1024 * 10;

    protected Map<String, Object> renderingContext = new HashMap<String, Object>();

    protected String renderingTemplateName = null;

    protected InputStream additionnalStream;

    protected String contentType=null;

    public WSSResponse(HttpServletResponse httpResponse) {
        this.httpResponse=httpResponse;
    }

    public void processIfNeeded() throws Exception {
        if (!processed) {
            process();
        }
    }

    public void process() throws Exception {
        if (processed) {
            throw new ServletException("process called twice on WSSResponse");
        }
        processHeaders();
        processRender();
        processed=true;
    }


    public void addBinaryStream(InputStream stream) {
        this.additionnalStream=stream;
    }

    protected void processHeaders() throws Exception {
        getHttpResponse().setHeader(MSWSSConsts.TSSERVER_VERSION_HEADER, WSSConfig.instance().getTSServerVersion());
        getHttpResponse().setHeader("Set-Cookie","WSS_KeepSessionAuthenticated=80; path=/");
        //getHttpResponse().setHeader("Server","Microsoft-IIS/6.0");
        getHttpResponse().setHeader("X-Powered-By", "ASP.NET");

        if (contentType==null) {
            getHttpResponse().setHeader("Content-type", getDefaultContentType());
        }
        else {
            getHttpResponse().setHeader("Content-type", contentType);
        }
    }

    protected String getDefaultContentType() {
        return "text/plain";
    }

    protected void processRender() throws Exception {
        if (renderingTemplateName!=null) {

            Writer writer = null;
            ByteArrayOutputStream bufferedOs = null;

            if (bufferizeRendering) {
                bufferedOs = new ByteArrayOutputStream();
                writer = new BufferedWriter(new OutputStreamWriter(bufferedOs));
            } else {
                writer = new BufferedWriter(new OutputStreamWriter(getHttpResponse().getOutputStream()));
            }

            FreeMarkerRenderer.instance().render(renderingTemplateName, renderingContext, writer);

            writer.flush();

            if (bufferizeRendering && additionnalStream!=null) {
                 byte[] buffer = new byte[BUFFER_SIZE];
                 int read;
                 while ((read = additionnalStream.read(buffer)) != -1) {
                     bufferedOs.write(buffer, 0, read);
                     bufferedOs.flush();
                 }

            }

            writer.close();

            if (bufferizeRendering) {
                int size = bufferedOs.size();
                getHttpResponse().setContentLength(size);
                OutputStream out = getHttpResponse().getOutputStream();
                out.write(bufferedOs.toByteArray());
                out.close();
            }
        }
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

    public Map<String, Object> getRenderingContext() {
        return renderingContext;
    }

    public void setRenderingContext(Map<String, Object> renderingContext) {
        this.renderingContext = renderingContext;
    }

    public String getRenderingTemplateName() {
        return renderingTemplateName;
    }

    public void setRenderingTemplateName(String renderingTemplateName) {
        this.renderingTemplateName = renderingTemplateName;
    }


    public void addRenderingParameter(String name, Object value) {
        renderingContext.put(name, value);
    }

    public void setContentType(String ct) {
        this.contentType=ct;
    }




}
