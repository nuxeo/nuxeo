/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.client.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.nuxeo.ecm.client.Connection;
import org.nuxeo.ecm.client.ContentHandler;
import org.nuxeo.ecm.client.ContentHandlerRegistry;

/**
 * @author matic
 *
 */
public class HttpClientConnection implements Connection {

    protected final HttpMethod method;
    protected final ContentHandlerRegistry contentHandlerRegistry;
    
    public HttpClientConnection(HttpMethod method, ContentHandlerRegistry contentHandlerRegistry) {
        this.method = method;
        this.contentHandlerRegistry = contentHandlerRegistry;
    }
    
    
    public void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    public <T> T getContent(Class<T> objectType) throws IOException {
        ContentHandler<T> contentHandler =
            contentHandlerRegistry.getContentHandler(getContentType(), objectType);
        return contentHandler.read(method.getResponseBodyAsStream());
    }

    public Object getContent() throws IOException {
        List<ContentHandler<?>> contentHandlers =
            contentHandlerRegistry.getContentHandler(getContentType());
        return contentHandlers.get(0).read(method.getResponseBodyAsStream());
    }

    public byte[] getContentAsBytes() throws IOException {
        return method.getResponseBody();
    }

    public String getContentAsString() throws IOException {
       return method.getResponseBodyAsString();
    }

    public String getContentType() {
       return method.getResponseHeader("Content-Type").getValue();
    }

    public String getHeader(String key) {
        return method.getResponseHeader(key).getValue();
    }


    public Map<String, String> getHeaders() {
        Map<String,String> values = 
            new HashMap<String,String>();
        for (Header header:method.getResponseHeaders()) {
            values.put(header.getName(), header.getValue());
        }
        return values;
    }


    public int getStatus() {
        return method.getStatusCode();
    }


    public InputStream getStream() throws IOException {
        return method.getResponseBodyAsStream();
    }

}
