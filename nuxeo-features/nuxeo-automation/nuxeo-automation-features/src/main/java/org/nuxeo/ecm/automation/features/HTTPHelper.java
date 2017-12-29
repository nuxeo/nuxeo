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
 *      Ricardo Dias <rdias@nuxeo.com>
 */
package org.nuxeo.ecm.automation.features;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.core.util.Base64;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final String HTTP_CONTENT_DISPOSITION = "Content-Disposition";

    public Blob call(String username, String password, String requestType, String path) throws IOException {
        return call(username, password, requestType, path, null, null, null, null);
    }

    public Blob call(String username, String password, String requestType, String path, Map<String, String> headers)
            throws IOException {
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

    /**
     * @since 8.4
     */
    public Blob get(String url, Map<String, Object> options) throws IOException {
        return invoke("GET", url, null, null, options);
    }

    /**
     * @since 8.4
     */
    public Blob post(String url, Object data, Map<String, Object> options) throws IOException {
        return invoke("POST", url, data, null, options);
    }

    /**
     * @since 8.4
     */
    public Blob post(String url, MultiPart multiPart, Map<String, Object> options) throws IOException {
        return invoke("POST", url, null, multiPart, options);
    }

    /**
     * @since 8.4
     */
    public Blob put(String url, Object data, Map<String, Object> options) throws IOException {
        return invoke("PUT", url, data, null, options);
    }

    /**
     * @since 8.4
     */
    public Blob put(String url, MultiPart multiPart, Map<String, Object> options) throws IOException {
        return invoke("PUT", url, null, multiPart, options);
    }

    /**
     * @since 8.4
     */
    public Blob delete(String url, Object data, Map<String, Object> options) throws IOException {
        return invoke("DELETE", url, data, null, options);
    }

    private Blob invoke(String requestType, String url, Object data, MultiPart multipart, Map<String, Object> options)
            throws IOException {
        MultivaluedMap<String, String> queryParams = getQueryParameters(options);
        Map<String, String> headers = getHeaderParameters(options);

        ClientConfig config = new DefaultClientConfig();
        config.getClasses().add(MultiPartWriter.class);
        Client client = Client.create(config);
        client.setConnectTimeout(TIMEOUT);

        WebResource wr = client.resource(url);

        if (queryParams != null && !queryParams.isEmpty()) {
            wr = wr.queryParams(queryParams);
        }
        WebResource.Builder builder;
        builder = wr.accept(MediaType.APPLICATION_JSON);
        if (multipart != null) {
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
                if (multipart != null) {
                    response = builder.post(ClientResponse.class, multipart);
                } else {
                    response = builder.post(ClientResponse.class, data);
                }
                break;
            case "PUT":
                if (multipart != null) {
                    response = builder.put(ClientResponse.class, multipart);
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
            return setUpBlob(response, url);
        } else {
            return new StringBlob(response.getStatusInfo() != null ? response.getStatusInfo().toString() : "error");
        }
    }

    private Map<String, String> getHeaderParameters(Map<String, Object> options) {
        if (options != null) {
            Map<String, String> headers = new HashMap<>();

            Map<String, String> authorization = (Map<String, String>) options.get("auth");
            if (authorization != null) {
                String method = authorization.get("method");
                switch (method) {
                case "basic":
                    Map<String, String> header = basicAuthentication(authorization.get("username"),
                            authorization.get("password"));
                    headers.putAll(header);
                    break;
                default:
                    break;
                }
            }

            Map<String, String> headersOptions = (Map<String, String>) options.get("headers");
            if (headersOptions != null) {
                headers.putAll(headersOptions);
            }

            return headers;
        }
        return null;
    }

    private MultivaluedMap<String, String> getQueryParameters(Map<String, Object> options) {
        if (options != null) {
            Map<String, List<String>> params = (Map<String, List<String>>) options.get("params");
            if (params != null) {
                MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
                for (String key : params.keySet()) {
                    queryParams.put(key, params.get(key));
                }
                return queryParams;
            }
        }
        return null;
    }

    private Blob setUpBlob(ClientResponse response, String url) throws IOException {
        MultivaluedMap<String, String> headers = response.getHeaders();
        String disposition = headers.getFirst(HTTP_CONTENT_DISPOSITION);

        String filename = "";
        if (disposition != null) {
            // extracts file name from header field
            int index = disposition.indexOf("filename=");
            if (index > -1) {
                filename = disposition.substring(index + 9);
            }
        } else {
            // extracts file name from URL
            filename = url.substring(url.lastIndexOf("/") + 1, url.length());
        }

        Blob resultBlob = Blobs.createBlob(response.getEntityInputStream());
        if (!StringUtils.isEmpty(filename)) {
            resultBlob.setFilename(filename);
        }

        String encoding = headers.getFirst(HttpHeaders.CONTENT_ENCODING);
        if (encoding != null) {
            resultBlob.setEncoding(encoding);
        }

        MediaType contentType = response.getType();
        if (contentType != null) {
            resultBlob.setMimeType(contentType.getType());
        }

        return resultBlob;
    }

    private Map<String, String> basicAuthentication(String username, String password) {

        if (username == null || password == null) {
            return null;
        }

        Map<String, String> authenticationHeader;
        try {
            final byte[] prefix = (username + ":").getBytes(Charset.forName("iso-8859-1"));
            final byte[] usernamePassword = new byte[prefix.length + password.getBytes().length];

            System.arraycopy(prefix, 0, usernamePassword, 0, prefix.length);
            System.arraycopy(password.getBytes(), 0, usernamePassword, prefix.length, password.getBytes().length);

            String authentication = "Basic " + new String(Base64.encode(usernamePassword), "ASCII");

            authenticationHeader = new HashMap<>();
            authenticationHeader.put(HttpHeaders.AUTHORIZATION, authentication);

        } catch (UnsupportedEncodingException ex) {
            // This should never occur
            throw new RuntimeException(ex);
        }
        return authenticationHeader;
    }
}
