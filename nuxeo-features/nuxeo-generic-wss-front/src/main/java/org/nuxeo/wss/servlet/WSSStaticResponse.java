package org.nuxeo.wss.servlet;

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
        processed = true;
    }

    public void addBinaryStream(InputStream stream) {
        this.additionnalStream = stream;
    }

    protected void processHeaders() throws Exception {
        getHttpResponse().setHeader(MSWSSConsts.TSSERVER_VERSION_HEADER,
                WSSConfig.instance().getTSServerVersion());
        getHttpResponse().setHeader("Set-Cookie",
                "WSS_KeepSessionAuthenticated=80; path=/");
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

    protected void processRender() throws Exception {
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
