/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.admin;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.rest.DocumentRoot;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@WebObject(type = "Admin", administrator = Access.GRANT)
@Produces("text/html;charset=UTF-8")
@Path("/admin")
public class Main extends ModuleRoot {


    @Path("users")
    public Object getUserManagement() {
        return newObject("UserManager");
    }

    @Path("engine")
    public Object getEngine() {
        return newObject("Engine");
    }

    @Path("repository")
    public Object getRepository() {
        return new DocumentRoot(ctx, "/");
    }

    @GET
    public Object getIndex() {
        return getView("index");
    }

    @GET
    @Path("help")
    public Object getHelp() {
        return getTemplate("help/help.ftl");
    }

    @GET
    @Path("about")
    public Object getAbout() {
        return getTemplate("help/about.ftl");
    }

    // handle errors
    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity(
                    getTemplate("error/error_401.ftl")).type("text/html").build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(
                    getTemplate("error/error_404.ftl")).type("text/html").build();
        } else {
            return super.handleError(e);
        }
    }

}
