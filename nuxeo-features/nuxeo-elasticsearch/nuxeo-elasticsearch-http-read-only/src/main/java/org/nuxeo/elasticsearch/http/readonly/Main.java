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
import java.net.MalformedURLException;
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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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
    @Path("{urlPath: [a-zA-Z0-9/]+_search}")
    @Consumes("application/x-www-form-urlencoded")
    public String doGetWithPayLoad(@PathParam("urlPath") String urlPath, @Context UriInfo uriInf,
            MultivaluedMap<String, String> formParams) throws IOException {
        String url = getElasticsearchUrl(uriInf);
        log.warn("Open: " + url + " formParams: " + formParams);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response;
        HttpGetWithEntity e = new HttpGetWithEntity(url);
        if (! formParams.isEmpty()) {
            StringEntity myEntity = new StringEntity(formParams.keySet().iterator().next(), ContentType.create(
                    "application/x-www-form-urlencoded", "UTF-8"));
            e.setEntity(myEntity);
        }
        response = client.execute(e);
        try {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        } finally {
            response.close();
        }
    }

    @GET
    @Path("{urlPath: [a-zA-Z0-9/]+_search}")
    public String doGet(@Context UriInfo uriInf) throws IOException {
        String url = getElasticsearchUrl(uriInf);
        log.warn("Open GET: " + url);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response;
        response = client.execute(new HttpGet(url));
        try {
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity) : null;
        } finally {
            response.close();
        }
    }

    protected String getElasticsearchUrl(UriInfo uriInf) throws MalformedURLException {
        String url = getElasticsearchBaseUrl() + uriInf.getPath().substring(3);
        String query = uriInf.getRequestUri().getRawQuery();
        if (query != null) {
            url += "?" + query;
        }
        return url;
    }

    private String getElasticsearchBaseUrl() {
        return DEFAULT_ES_BASE_URL;
    }

    public boolean isAdministrator() {
        Principal principal = ctx.getPrincipal();
        if (principal == null) {
            return false;
        }
        return ((NuxeoPrincipal) principal).isAdministrator();
    }

}
