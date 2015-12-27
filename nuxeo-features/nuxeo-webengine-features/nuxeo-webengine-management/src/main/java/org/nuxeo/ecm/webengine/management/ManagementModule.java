/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Web object implementation corresponding to the root module for management (module used for administrative purpose).
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
