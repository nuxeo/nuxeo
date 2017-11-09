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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;

public class HttpURLConnection extends java.net.HttpURLConnection {

    protected final HttpURLClientProvider clientProvider;

    protected HttpUriRequest request;

    protected HttpResponse response;

    public HttpURLConnection(HttpURLClientProvider provider, URL url) {
        super(url);
        this.clientProvider = provider;
        setRequestMethod("GET");
    }

    protected HttpUriRequest newMethod(String name, URI uri) {
        if ("GET".equals(name)) {
            return new HttpGet(uri);
        }
        if ("POST".equals(name)) {
            return new HttpPost(uri);
        }
        if ("DELETE".equals(name)) {
            return new HttpDelete(uri);
        }
        if ("PUT".equals(name)) {
            return new HttpPut(uri);
        }
        throw new UnsupportedOperationException("Unsupported method " + name);
    }

    @Override
    public void setRequestMethod(String name) {
        try {
            request = newMethod(name, url.toURI());
        } catch (URISyntaxException e) {
            throw new Error("unsupported URL " + url, e);
        }
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() throws IOException {
        response = clientProvider.getClient().execute(request);
    }

    @Override
    public void setRequestProperty(String key, String value) {
        request.setHeader(key, value);
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
        // ignore
    }

    @Override
    public void setDefaultUseCaches(boolean defaultusecaches) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return response.getEntity().getContent();
    }

    @SuppressWarnings("resource")
    @Override
    public OutputStream getOutputStream() throws IOException {
        PipedOutputStream source = new PipedOutputStream();
        PipedInputStream sink = new PipedInputStream();
        source.connect(sink);
        HttpEntity entity = new InputStreamEntity(sink);
        ((HttpEntityEnclosingRequest) request).setEntity(entity);
        return source;
    }

    @Override
    public int getResponseCode() throws IOException {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return response.getStatusLine().getReasonPhrase();
    }

    @Override
    public String getHeaderField(String name) {
        Header header = response.getFirstHeader(name);
        return header == null ? null : header.getValue();
    }

    @Override
    public String getHeaderFieldKey(int keyPosition) {
        if (keyPosition == 0) {
            return null;
        }
        Header[] headers = response.getAllHeaders();
        if (keyPosition < 0 || keyPosition > headers.length) {
            return null;
        }
        return headers[keyPosition - 1].getName();
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        Map<String, List<String>> fields = new HashMap<String, List<String>>();
        for (Header header : response.getAllHeaders()) {
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
        if (response.getStatusLine().getStatusCode() != 200) {
            try {
                return response.getEntity().getContent();
            } catch (IOException e) {
                throw new Error("Cannot get response content", e);
            }
        }
        return null;
    }
}
