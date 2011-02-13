/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.webengine.management;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.management.statuses.StatusesObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * Web object implementation corresponding to the root module for management
 * (module used for administrative purpose).
 *
 * @author mcedica
 */
@Path("/mgmt")
@WebObject(type = "Management")
@Produces("text/html; charset=UTF-8")
public class ManagementModule extends ModuleRoot {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(ManagementModule.class);

    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("statuses")
    public Object dispatchStatuses() {
        return StatusesObject.newObject(this);
    }

    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity(getTemplate("error_401.ftl")).type("text/html").build();
        }
        return super.handleError(e);
    }

}
