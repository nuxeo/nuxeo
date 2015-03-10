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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@Path("/es")
@WebObject(type = "es")
@Produces(MediaType.APPLICATION_JSON)
public class Main extends ModuleRoot {

    private static final Log log = LogFactory.getLog(Main.class);

    private static final String DEFAULT_ES_BASE_URL = "http://localhost:9200/";

    @GET
    @Path("{path: [a-zA-Z0-9_/]+}")
    @Consumes("application/x-www-form-urlencoded")
    public String doGet(@PathParam("path") String path, @Context UriInfo uriInf,
            MultivaluedMap<String, String> formParams) throws IOException {
        URL url = getElasticsearchUrl(path, uriInf, formParams);
        log.warn("Open: " + url + " formParams: " + formParams);
        URLConnection conn = url.openConnection();
        if (!formParams.isEmpty()) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            try (OutputStream output = conn.getOutputStream()) {
                output.write(formParams.keySet().iterator().next().getBytes("UTF-8"));
            }
        }
        try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream())) {
            return s.useDelimiter("\\A").next();
        }
    }

    protected URL getElasticsearchUrl(String path, UriInfo uriInf, MultivaluedMap<String, String> formParams)
            throws MalformedURLException {
        String url = getElasticsearchBaseUrl() + (path == null ? "" : path);
        String query = uriInf.getRequestUri().getRawQuery();
        if (query != null) {
            url += "?" + query;
        }
        return new URL(url);
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
