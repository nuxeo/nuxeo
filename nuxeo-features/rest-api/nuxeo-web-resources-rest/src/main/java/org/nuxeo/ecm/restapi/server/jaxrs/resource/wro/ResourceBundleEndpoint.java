/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.restapi.server.jaxrs.resource.wro;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * Hook on REST API URLs (just redirects to the Wro servlet for now).
 *
 * @since 7.3
 */
@WebObject(type = "resource")
public class ResourceBundleEndpoint extends DefaultObject {

    private static final Log log = LogFactory.getLog(ResourceBundleEndpoint.class);

    @GET
    @Path("bundle/{var:.*}")
    public Object redirect(@Context UriInfo ui) {
        URI uri = ui.getRequestUri();
        try {
            URI other = new URI(uri.toString().replaceFirst("/site/api/", "/wapi/"));
            return Response.seeOther(other).build();
        } catch (URISyntaxException e) {
            log.error("Failed to redirect", e);
            return null;
        }
    }
}
