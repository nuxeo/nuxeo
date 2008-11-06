package org.nuxeo.ecm.webengine.server.resteasy;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

public class HttpRequestLazyInputStream extends InputStream {

    protected InputStream requestStream=null;

    protected HttpServletRequest httpRequest;

    public HttpRequestLazyInputStream(HttpServletRequest request)
    {
        httpRequest=request;
    }

    protected InputStream getStream() throws IOException
    {
        if (requestStream==null)
        {
            requestStream=httpRequest.getInputStream();
        }
        return requestStream;
    }

    @Override
    public int read() throws IOException {
        return getStream().read();
    }

    @Override
    public int read(byte b[]) throws IOException {
        return getStream().read(b);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return getStream().read(b,off, len);
    }
}
