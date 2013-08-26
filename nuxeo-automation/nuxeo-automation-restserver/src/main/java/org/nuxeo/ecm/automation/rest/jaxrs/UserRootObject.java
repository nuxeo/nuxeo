/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
@WebObject(type = "users")
@Produces({ MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_JSON + "+nxentity" })
public class UserRootObject extends DefaultObject {


    @Path("{username}")
    public Object getUser(@PathParam("username")
    String username) {
        UserManager um = Framework.getLocalService(UserManager.class);
        try {
            NuxeoPrincipal user = um.getPrincipal(username);
            return newObject("user", user);
        } catch (ClientException e) {
            return new WebResourceNotFoundException(e.getMessage());
        }
    }

    @POST
    public Response createUser(NuxeoPrincipal principal) {
        try {
            if(principal.getName() == null) {
                throw new WebException("User MUST have a name",
                        Response.Status.PRECONDITION_FAILED.getStatusCode());
            }

            UserManager um = Framework.getLocalService(UserManager.class);
            NuxeoPrincipal user = um.getPrincipal(principal.getName());
            if (user != null) {
                throw new WebException("User already exists",
                        Response.Status.PRECONDITION_FAILED.getStatusCode());
            }

            um.createUser(principal.getModel());
            return Response.status(Status.CREATED).entity(
                    um.getPrincipal(principal.getName())).build();

        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

}
