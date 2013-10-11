/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.client.rest.api;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.automation.client.Constants.CTYPE_ENTITY;
import static org.nuxeo.ecm.automation.client.Constants.HEADER_NX_SCHEMAS;
import static org.nuxeo.ecm.automation.client.rest.api.RequestType.POSTREQUEST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.client.AutomationException;
import org.nuxeo.ecm.automation.client.Constants;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * A REST request on Nuxeo REST API.
 *
 * @since 5.8
 */
public class RestRequest {

    protected final WebResource service;

    protected final String path;

    protected RequestType requestType = RequestType.GET;

    protected String data;

    protected String repositoryName;

    protected List<String> schemas = new ArrayList<String>();

    protected Map<String, Object> headers = new HashMap<String, Object>();

    protected MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

    public RestRequest(WebResource service, String path) {
        this.service = service;
        this.path = path;
    }

    public RestRequest requestType(RequestType requestType) {
        this.requestType = requestType;
        return this;
    }

    public RestRequest data(String data) {
        this.data = data;
        return this;
    }

    public RestRequest repositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        return this;
    }

    public RestRequest schema(String schema) {
        schemas.add(schema);
        return this;
    }

    public RestRequest schemas(List<String> schemas) {
        this.schemas.addAll(schemas);
        return this;
    }

    public RestRequest header(String key, Object value) {
        this.headers.put(key, value);
        return this;
    }

    public RestRequest headers(Map<String, Object> headers) {
        this.headers.putAll(headers);
        return this;
    }

    public RestRequest queryParam(String key, String value) {
        queryParams.add(key, value);
        return this;
    }

    public RestRequest queryParams(MultivaluedMap<String, String> queryParams) {
        this.queryParams.putAll(queryParams);
        return this;
    }

    public String getPath() {
        return path;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getData() {
        return data;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public MultivaluedMap<String, String> getQueryParams() {
        return queryParams;
    }

    public RestResponse execute() {
        WebResource wr = service;
        if (!StringUtils.isBlank(repositoryName)) {
            wr = wr.path("repo").path(repositoryName);
        }
        wr = wr.path(path);
        if (queryParams != null && !queryParams.isEmpty()) {
            wr = wr.queryParams(queryParams);
        }

        WebResource.Builder builder = wr.accept(APPLICATION_JSON);
        for (Map.Entry<String, Object> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        if (!schemas.isEmpty()) {
            String documentPropertiesHeader = StringUtils.join(schemas, ",");
            builder.header(HEADER_NX_SCHEMAS, documentPropertiesHeader);
        }

        if (requestType == POSTREQUEST) {
            builder.header(CONTENT_TYPE, Constants.CTYPE_REQUEST);
        } else {
            builder.header(CONTENT_TYPE, CTYPE_ENTITY);
        }

        ClientResponse response;
        switch (requestType) {
        case GET:
            response = builder.get(ClientResponse.class);
            break;
        case POST:
        case POSTREQUEST:
            response = builder.post(ClientResponse.class, data);
            break;
        case PUT:
            response = builder.put(ClientResponse.class, data);
            break;
        case DELETE:
            response = builder.delete(ClientResponse.class, data);
            break;
        default:
            throw new AutomationException();
        }

        return new RestResponse(response);
    }
}
