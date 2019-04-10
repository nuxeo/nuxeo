/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Stephane Lacoin (aka matic)
 */
package org.nuxeo.ecm.core.opencmis.impl.client.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

public class HttpURLConnection extends java.net.HttpURLConnection {

    protected final HttpURLClientProvider clientProvider;

    protected HttpMethod method;

    public HttpURLConnection(HttpURLClientProvider provider, URL url) {
        super(url);
        this.clientProvider = provider;
        setRequestMethod("GET");
    }

    protected HttpMethod newMethod(String name) {
        if ("GET".equals(name)) {
            return new GetMethod();
        }
        if ("POST".equals(name)) {
            return new PostMethod();
        }
        if ("DELETE".equals(name)) {
            return new DeleteMethod();
        }
        if ("PUT".equals(name)) {
            return new PutMethod();
        }
        throw new UnsupportedOperationException("Unsupported method " + name);
    }

    @Override
    public void setRequestMethod(String name) {
        method = newMethod(name);
        try {
            method.setURI(new URI(url.toExternalForm(), false));
        } catch (Exception e) {
            throw new Error("unsupported URL " + url, e);
        }
    }

    @Override
    public void disconnect() {
        method.releaseConnection();
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() throws IOException {
        clientProvider.getClient().executeMethod(method);
    }

    @Override
    public void setRequestProperty(String key, String value) {
        method.setRequestHeader(key, value);
    }

    @Override
    public void setChunkedStreamingMode(int chunklen) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAllowUserInteraction(boolean allowuserinteraction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConnectTimeout(int timeout) {
        clientProvider.getClient().getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
    }

    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return method.getResponseBodyAsStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        PipedOutputStream source = new PipedOutputStream();
        PipedInputStream sink = new PipedInputStream();
        source.connect(sink);
        RequestEntity entity = new InputStreamRequestEntity(sink);
        ((EntityEnclosingMethod) method).setRequestEntity(entity);
        return source;
    }

    @Override
    public int getResponseCode() throws IOException {
        return method.getStatusCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return this.method.getStatusText();
    }

    @Override
    public String getHeaderField(String name) {
        Header[] headers = this.method.getResponseHeaders();
        for (int i = headers.length - 1; i >= 0; i--) {
            if (headers[i].getName().equalsIgnoreCase(name)) {
                return headers[i].getValue();
            }
        }

        return null;
    }

    @Override
    public String getHeaderFieldKey(int keyPosition) {
        if (keyPosition == 0) {
            return null;
        }
        Header[] headers = this.method.getResponseHeaders();
        if (keyPosition < 0 || keyPosition > headers.length) {
            return null;
        }
        return headers[keyPosition - 1].getName();
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        Map<String, List<String>> fields = new HashMap<String, List<String>>();
        for (Header header : this.method.getResponseHeaders()) {
            String name = header.getName();
            String value = header.getValue();
            if (!fields.containsKey(name)) {
                fields.put(name, new ArrayList<String>());
            }
            fields.get(name).add(value);
        }
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public InputStream getErrorStream() {
        if (method.getStatusCode() != 200) {
            try {
                return method.getResponseBodyAsStream();
            } catch (IOException e) {
                throw new Error("Cannot get response content", e);
            }
        }
        return null;
    }
}
