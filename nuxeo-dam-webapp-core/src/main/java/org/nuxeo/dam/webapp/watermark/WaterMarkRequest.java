package org.nuxeo.dam.webapp.watermark;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class WaterMarkRequest extends HttpServletRequestWrapper {
    private String requestUri;

    public WaterMarkRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getRequestURI() {
        if (requestUri == null) {
            requestUri = super.getRequestURI().replace("watermark", "nxbigfile");
        }

        return requestUri;
    }
}
