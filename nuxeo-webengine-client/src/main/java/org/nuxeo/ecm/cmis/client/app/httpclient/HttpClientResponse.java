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
 *     bstefanescu
 */
package org.nuxeo.ecm.cmis.client.app.httpclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.nuxeo.ecm.cmis.ContentManagerException;
import org.nuxeo.ecm.cmis.client.app.Connector;
import org.nuxeo.ecm.cmis.client.app.Feed;
import org.nuxeo.ecm.cmis.client.app.Response;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class HttpClientResponse implements Response {

    final static int MIN_BUF_LEN = 32 * 1024;
    final static int MAX_BUF_LEN = 128 * 1024;
    
    protected HttpMethod method;
    protected Connector connector;
    
    public HttpClientResponse(Connector connector, HttpMethod method) {
        this.method = method;
        this.connector = connector;
    }
    

    public String getHeader(String key) {
        Header h = method.getResponseHeader(key);
        return h == null ? null : h.getValue();
    }

    public int getStatusCode() {
        return method.getStatusCode();
    }

    public boolean isOk() {
        return method.getStatusCode() < 400;
    }

    public InputStream getStream() throws ContentManagerException {
        try {
            return method.getResponseBodyAsStream();
        } catch (IOException e) {
            throw new ContentManagerException("Failed to get response stream", e);
        }
    }

    public String getString() throws ContentManagerException {
        try {
            return method.getResponseBodyAsString();
        } catch (IOException e) {
            throw new ContentManagerException("Failed to get response stream", e);
        }
    }

    public byte[] getBytes() throws ContentManagerException {
        InputStream in = null;
        try {
            in = getStream();
            int len = in.available();
            if (len < MIN_BUF_LEN) {
                len = MIN_BUF_LEN;
            } else {
                len = MAX_BUF_LEN;
            }
            byte[] buffer = new byte[len];
            int read;        
            ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
            while ((read = in.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ContentManagerException("Failed to get response", e); 
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getEntity(Object context, Class<T> clazz) throws ContentManagerException {        
        InputStream in = getStream();
        try {
            Object result = connector.getSerializationManager().readEntity(context, clazz, in);
            return (T)result;
        } finally {
            try {in.close();} catch (IOException e) { e.printStackTrace(); }
        }
    }
    
    public <T> Feed<T> getFeed(Object context, Class<T> clazz) throws ContentManagerException {
        InputStream in = getStream();
        try {
            return connector.getSerializationManager().readFeed(context, clazz, in);
        } finally {
            try {in.close();} catch (IOException e) { e.printStackTrace(); }
        }
    }

}
