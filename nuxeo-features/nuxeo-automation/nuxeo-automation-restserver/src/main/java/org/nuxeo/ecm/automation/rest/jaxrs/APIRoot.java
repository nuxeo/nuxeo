/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */

package org.nuxeo.ecm.automation.rest.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;


/**
 * The root entry for the WebEngine module.
 *
 */
@Path("/api")
@Produces("text/html;charset=UTF-8")
@WebObject(type="APIRoot")
public class APIRoot extends ModuleRoot {

    @GET
    public Object doGet() {
        return getView("index");
    }


    @Path("path")
    public Object getDocsByPath() {
        return new JSONDocumentRoot(ctx, "/");
    }

    @Path("id/{id}")
    public Object getDocsById(@PathParam("id") String id) {
        return new JSONDocumentRoot(ctx, new IdRef(id));
    }


}
