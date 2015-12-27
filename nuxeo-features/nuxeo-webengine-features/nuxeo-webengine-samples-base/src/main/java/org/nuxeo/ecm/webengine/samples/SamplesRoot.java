/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * The module entry points are regular JAX-RS resources with an additional @WebObject annotation. This annotation is
 * mainly used to specify the resource name. A Web Module is declared in the MANIFEST.MF using the directive
 * {code}NuxeoWebModule{/code}. You can also configure a Web Module using a module.xml file located in the module root
 * directory. This file can be used to define: root resources (as we've seen in the previous example), links, media type
 * IDs random extensions to other extension points;
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
            return Response.status(401).entity(getTemplate("error/error_401.ftl")).build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(getTemplate("error/error_404.ftl")).build();
        } else {
            // not interested in that exception - use default handling
            return super.handleError(e);
        }
    }
}
