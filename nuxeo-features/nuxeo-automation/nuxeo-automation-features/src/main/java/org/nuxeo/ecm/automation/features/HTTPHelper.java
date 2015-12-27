/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Thibaud Arguillere <targuillere@nuxeo.com>
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.features;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;

/**
 * @since 7.3
 */
public class HTTPHelper implements ContextHelper {

    protected static volatile ObjectMapper mapper = new ObjectMapper();

    private static final Integer TIMEOUT = 1000 * 60 * 5; // 5min

    public Blob call(String username, String password, String requestType, String path) throws IOException {
        return call(username, password, requestType, path, null, null, null, null);
    }

    public Blob call(String username, String password, String requestType, String path,
            Map<String, String> headers) throws IOException {
        return call(username, password, requestType, path, null, null, null, headers);
    }

    public Blob call(String username, String password, String requestType, String path, MultiPart mp)
            throws IOException {
        return call(username, password, requestType, path, null, null, mp, null);
    }

    public Blob call(String username, String password, String requestType, String path, MultiPart mp,
            Map<String, String> headers) throws IOException {
        return call(username, password, requestType, path, null, null, mp, headers);
    }

    public Blob call(String username, String password, String requestType, String path,
            MultivaluedMap<String, String> queryParams) throws IOException {
        return call(username, password, requestType, path, null, queryParams, null, null);
    }

    public Blob call(String username, String password, String requestType, String path, Object data)
            throws IOException {
        return call(username, password, requestType, path, data, null, null, null);
    }

    public Blob call(String username, String password, String requestType, String path, Object data,
            Map<String, String> headers) throws IOException {
        return call(username, password, requestType, path, data, null, null, headers);
    }

    public Blob call(String username, String password, String requestType, String url, Object data,
            MultivaluedMap<String, String> queryParams, MultiPart mp, Map<String, String> headers) throws IOException {
        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(MultiPartWriter.class);
        Client client = Client.create(config);
        client.setConnectTimeout(TIMEOUT);
        client.setReadTimeout(TIMEOUT);
        if (username != null && password != null) {
            client.addFilter(new HTTPBasicAuthFilter(username, password));
        }

        WebResource wr = client.resource(url);

        if (queryParams != null && !queryParams.isEmpty()) {
            wr = wr.queryParams(queryParams);
        }
        WebResource.Builder builder;
        builder = wr.accept(MediaType.APPLICATION_JSON);
        if (mp != null) {
            builder = wr.type(MediaType.MULTIPART_FORM_DATA_TYPE);
        }

        // Adding some headers if needed
        if (headers != null && !headers.isEmpty()) {
            for (String headerKey : headers.keySet()) {
                builder.header(headerKey, headers.get(headerKey));
            }
        }
        ClientResponse response = null;
        try {
            switch (requestType) {
            case "HEAD":
            case "GET":
                response = builder.get(ClientResponse.class);
                break;
            case "POST":
                if (mp != null) {
                    response = builder.post(ClientResponse.class, mp);
                } else {
                    response = builder.post(ClientResponse.class, data);
                }
                break;
            case "PUT":
                if (mp != null) {
                    response = builder.put(ClientResponse.class, mp);
                } else {
                    response = builder.put(ClientResponse.class, data);
                }
                break;
            case "DELETE":
                response = builder.delete(ClientResponse.class, data);
                break;
            default:
                break;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (response != null && response.getStatus() >= 200 && response.getStatus() < 300) {
            return Blobs.createBlob(response.getEntityInputStream());
        } else {
            return new StringBlob(response.getStatusInfo() != null ? response.getStatusInfo().toString() : "error");
        }
    }

}
