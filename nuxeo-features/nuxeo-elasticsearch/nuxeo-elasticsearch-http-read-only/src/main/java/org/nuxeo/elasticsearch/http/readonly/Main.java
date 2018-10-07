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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.http.readonly;

import java.io.IOException;
import java.security.Principal;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.elasticsearch.http.readonly.filter.RequestValidator;
import org.nuxeo.elasticsearch.http.readonly.filter.SearchRequestFilter;
import org.nuxeo.elasticsearch.http.readonly.service.RequestFilterService;
import org.nuxeo.elasticsearch.http.readonly.filter.DefaultSearchRequestFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * Exposes a limited set of Read Only Elasticsearch REST API.
 *
 * @since 7.3
 */
@Path("/es")
@WebObject(type = "es")
public class Main extends ModuleRoot {
    private static final Log log = LogFactory.getLog(Main.class);
    private static final String DEFAULT_ES_BASE_URL = "http://localhost:9200/";
    private static final java.lang.String ES_BASE_URL_PROPERTY = "elasticsearch.httpReadOnly.baseUrl";
    private String esBaseUrl;

    public Main() {
        super();
        if (getContext() == null) {
        }
        if (log.isDebugEnabled()) {
            log.debug("New instance of ES module");
        }
    }

    @GET
    @Path("_search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String searchWithPayload(@Context UriInfo uriInf, MultivaluedMap<String, String> formParams)
            throws IOException, JSONException {
        return doSearchWithPayload("_all", "_all", uriInf.getRequestUri().getRawQuery(),
                formParams.keySet().iterator().next());
    }

    @POST
    @Path("_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String searchWithPost(@Context UriInfo uriInf, String payload) throws IOException, JSONException {
        return doSearchWithPayload("_all", "_all", uriInf.getRequestUri().getRawQuery(), payload);
    }

    @GET
    @Path("{indices}/_search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String searchWithPayload(@PathParam("indices") String indices, @Context UriInfo uriInf,
            MultivaluedMap<String, String> formParams) throws IOException, JSONException {
        return doSearchWithPayload(indices, "_all", uriInf.getRequestUri().getRawQuery(),
                formParams.keySet().iterator().next());
    }

    @POST
    @Path("{indices}/_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String searchWithPost(@PathParam("indices") String indices, @Context UriInfo uriInf, String payload)
            throws IOException, JSONException {
        return doSearchWithPayload(indices, "_all", uriInf.getRequestUri().getRawQuery(), payload);
    }

    @GET
    @Path("{indices}/{types}/_search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String searchWithPayload(@PathParam("indices") String indices, @PathParam("types") String types,
            @Context UriInfo uriInf, MultivaluedMap<String, String> formParams) throws IOException, JSONException {
        return doSearchWithPayload(indices, types, uriInf.getRequestUri().getRawQuery(),
                formParams.keySet().iterator().next());
    }

    @POST
    @Path("{indices}/{types}/_search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String searchWithPost(@PathParam("indices") String indices, @PathParam("types") String types,
            @Context UriInfo uriInf, String payload) throws IOException, JSONException {
        return doSearchWithPayload(indices, types, uriInf.getRequestUri().getRawQuery(), payload);
    }

    protected String doSearchWithPayload(String indices, String types, String rawQuery, String payload)
            throws IOException, JSONException {
        RequestFilterService requestFilterService = Framework.getService(RequestFilterService.class);
        try {
            SearchRequestFilter req = requestFilterService.getRequestFilters(indices);
            if (req == null) {
                req = new DefaultSearchRequestFilter();
            }
            req.init(getContext().getCoreSession(), indices, types, rawQuery, payload);
            log.debug(req);
            return HttpClient.get(getElasticsearchBaseUrl() + req.getUrl(), req.getPayload());
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Error when trying to get Search Request Filter for indice " + indices, e);
            return null;
        }
    }

    @GET
    @Path("{indices}/{types}/_search")
    @Produces(MediaType.APPLICATION_JSON)
    public String searchWithUri(@PathParam("indices") String indices, @PathParam("types") String types, @Context UriInfo uriInf)
            throws IOException, JSONException {
        DefaultSearchRequestFilter req = new DefaultSearchRequestFilter();
        req.init(getContext().getCoreSession(), indices, types,
                uriInf.getRequestUri().getRawQuery(), null);
        log.debug(req);
        return HttpClient.get(getElasticsearchBaseUrl() + req.getUrl(), req.getPayload());
    }

    @GET
    @Path("{indices}/{types}/{documentId: [a-zA-Z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDocument(@PathParam("indices") String indices, @PathParam("types") String types,
            @PathParam("documentId") String documentId, @Context UriInfo uriInf) throws IOException, JSONException {
        NuxeoPrincipal principal = getPrincipal();
        RequestValidator validator = new RequestValidator();
        indices = validator.getIndices(indices);
        types = validator.getTypes(indices, types);
        validator.checkValidDocumentId(documentId);
        DocRequestFilter req = new DocRequestFilter(principal, indices, types, documentId,
                uriInf.getRequestUri().getRawQuery());
        log.debug(req);
        if (!principal.isAdministrator()) {
            String docAcl = HttpClient.get(getElasticsearchBaseUrl() + req.getCheckAccessUrl());
            validator.checkAccess(principal, docAcl);
        }
        return HttpClient.get(getElasticsearchBaseUrl() + req.getUrl());
    }

    protected String getElasticsearchBaseUrl() {
        if (esBaseUrl == null) {
            esBaseUrl = Framework.getProperty(ES_BASE_URL_PROPERTY, DEFAULT_ES_BASE_URL);
        }
        return esBaseUrl;
    }

    public @NotNull NuxeoPrincipal getPrincipal() {
        NuxeoPrincipal principal = ctx.getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException("No principal found");
        }
        return principal;
    }

}
