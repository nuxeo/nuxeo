/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.elasticsearch.http.readonly;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@Path("/es")
@WebObject(type = "es")
@Produces(MediaType.APPLICATION_JSON)
public class Main extends ModuleRoot {

    private static final Log log = LogFactory.getLog(Main.class);

    private static final String DEFAULT_ES_BASE_URL = "http://localhost:9200/";

    public class HttpGetWithEntity extends HttpPost {

        public final static String METHOD_NAME = "GET";

        public HttpGetWithEntity(URI url) {
            super(url);
        }

        public HttpGetWithEntity(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }
    }

    @GET
    @Path("{indices}/{types}/_search")
    @Consumes("application/x-www-form-urlencoded")
    public String searchWithPayLoad(@PathParam("indices") String indices, @PathParam("types") String types,
            @Context UriInfo uriInf, MultivaluedMap<String, String> formParams) throws IOException, JSONException {
        String url = getSearchUrl(indices, types, uriInf);
        String payload = formParams.keySet().iterator().next();
        NuxeoPrincipal principal = getPrincipal();
        log.warn("Body Search: " + url + " user: " + principal + " payload: " + payload);
        if (principal == null) {
            throw new IllegalArgumentException("No principal provided");
        }
        if (!principal.isAdministrator()) {
            payload = addSecurityFilter(payload, principal);
        }
        return doGet(url, payload);
    }

    protected String addSecurityFilter(final String payload, NuxeoPrincipal principal) throws JSONException {
        String[] principals = SecurityService.getPrincipalsToCheck(principal);
        JSONObject payloadJson = new JSONObject(payload);
        JSONObject query;
        if (payloadJson.has("query")) {
            query = payloadJson.getJSONObject("query");
            payloadJson.remove("query");
        } else {
            query = new JSONObject("{\"match_all\":{}}");
        }
        JSONObject filter = new JSONObject().put("terms", new JSONObject().put("ecm:acl", principals));
        JSONObject newQuery = new JSONObject().put("filtered",
                new JSONObject().put("query", query).put("filter", filter));
        payloadJson.put("query", newQuery);
        return payloadJson.toString();
    }

    @GET
    @Path("{indices}/{types}/_search")
    public String search(@PathParam("indices") String indices, @PathParam("types") String types, @Context UriInfo uriInf)
            throws IOException {
        String url = getSearchUrl(indices, types, uriInf);
        NuxeoPrincipal principal = getPrincipal();
        log.warn("URI Search: " + url + " user: " + principal);
        if (principal == null) {
            throw new IllegalArgumentException("No principal provided");
        }
        if (principal.isAdministrator()) {
            return doGet(url);
        }
        return performUriSearchWithSecurity(url, principal);
    }

    protected String performUriSearchWithSecurity(String url, NuxeoPrincipal principal) {
        // TODO: convert URI Search query to a body query
        throw new IllegalArgumentException("URI Search not implemented for non admin user");
    }

    @GET
    @Path("{indices}/{types}/{documentId: [a-zA-Z0-9\\-]+}")
    public String getDocument(@PathParam("indices") String indices, @PathParam("types") String types,
            @PathParam("documentId") String documentId, @Context UriInfo uriInf) throws IOException {
        String url = getDocumentUrl(indices, types, documentId, uriInf);
        log.warn("Get: " + url);
        return doGet(url);
    }

    protected String doGet(String url) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        try (CloseableHttpResponse response = client.execute(httpget)) {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        }
    }

    protected String doGet(String url, String payload) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGetWithEntity e = new HttpGetWithEntity(url);
        StringEntity myEntity = new StringEntity(payload, ContentType.create("application/x-www-form-urlencoded",
                "UTF-8"));
        e.setEntity(myEntity);
        try (CloseableHttpResponse response = client.execute(e)) {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        }
    }

    protected String getSearchUrl(String indices, String types, UriInfo uriInf) {
        checkValidIndices(indices);
        checkValidTypes(types);
        String url = getElasticsearchBaseUrl() + "/" + indices + "/" + types + "/_search";
        String query = uriInf.getRequestUri().getRawQuery();
        if (query != null) {
            url += "?" + query;
        }
        return url;
    }

    protected String getDocumentUrl(String indices, String types, String documentId, UriInfo uriInf) {
        checkValidIndices(indices);
        checkValidTypes(types);
        checkValidDocumentId(documentId);
        String url = getElasticsearchBaseUrl() + "/" + indices + "/" + types + "/" + documentId;
        String query = uriInf.getRequestUri().getRawQuery();
        if (query != null) {
            url += "?" + query;
        }
        return url;
    }

    protected void checkValidDocumentId(String documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Invalid document id");
        }
    }

    protected void checkValidTypes(String types) {
        if (types == null || !"doc".equals(types)) {
            throw new IllegalArgumentException("Invalid type found");
        }
        // TODO check that indices are define in Nuxeo
    }

    protected void checkValidIndices(String indices) {
        if (indices == null || "*".equals(indices) || "_all".equals(indices)) {
            throw new IllegalArgumentException("Invalid index submitted");
        }
    }

    protected String getElasticsearchBaseUrl() {
        // TODO: make ES base url configurable
        return DEFAULT_ES_BASE_URL;
    }

    public NuxeoPrincipal getPrincipal() {
        Principal principal = ctx.getPrincipal();
        return (NuxeoPrincipal) principal;
    }

}
