/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.webengine.samples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * Web Engine Samples Root
 * <p>
 * This demonstrates how to define the entry point for a WebEngine module.
 * <p>
 * The module entry points are regular JAX-RS resources with an additional @WebObject
 * annotation. This annotation is mainly used to specify the resource name. A
 * Web Module is declared in the MANIFEST.MF using the directive
 * {code}NuxeoWebModule{/code}. You can also configure a Web Module using a
 * module.xml file located in the module root directory. This file can be used
 * to define: root resources (as we've seen in the previous example), links,
 * media type IDs random extensions to other extension points;
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author Stephane Lacoin (aka matic)
 */
@WebObject(type = "Root", administrator = Access.GRANT)
@Path("samples")
@Produces("text/html;charset=UTF-8")
public class SamplesRoot extends ModuleRoot {

    @GET
    public Object doGet() {
        return getTemplate("index.ftl");
    }

    @Path("hello")
    public Object doGetHello() {
        return newObject("Hello");
    }

    @Path("templating")
    public Object doGetTemplating() {
        return newObject("Templating");
    }

    @Path("basics")
    public Object doGetObjects() {
        return newObject("Basics");
    }

    @Path("documents")
    public Object doGetBrowser() {
        return newObject("Documents");
    }

    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            // display a login page
            return Response.status(401).entity(
                    getTemplate("error/error_401.ftl")).build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(
                    getTemplate("error/error_404.ftl")).build();
        } else {
            // not interested in that exception - use default handling
            return super.handleError(e);
        }
    }
}
