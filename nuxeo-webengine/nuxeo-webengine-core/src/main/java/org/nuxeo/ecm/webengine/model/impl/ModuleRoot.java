/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.app.DefaultContext;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.ModuleResource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.server.impl.inject.ServerInjectableProviderContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ModuleRoot extends DefaultObject implements ModuleResource {

    @Context
    protected HttpServletRequest request;

    @Context
    protected ServerInjectableProviderContext sic;

    @Context
    public void setContext(HttpContext hc) {
        DefaultContext ctx = (DefaultContext) request.getAttribute(WebContext.class.getName());
        if (ctx == null) {
            throw new java.lang.IllegalStateException(
                    "No WebContext found in http request! You should install the WebContextFilter");
        }
        if (ctx.getModule() != null) { // just a resource, not a module root
            return;
        }
        try {
            ctx.setJerseyContext(sic, hc);
            Module module = findModule(ctx);
            ResourceType type = module.getType(getClass().getAnnotation(WebObject.class).type());
            ctx.setModule(module);
            initialize(ctx, type);
            setRoot(true);
        } finally {
            ctx.push(this);
        }
    }

    private Module findModule(DefaultContext ctx) {
        Path path = getClass().getAnnotation(Path.class);
        if (path == null) {
            throw new java.lang.IllegalStateException("ModuleRoot not annotated with @Path: " + getClass());
        }
        ModuleConfiguration mc = ctx.getEngine().getModuleManager().getModuleByRootClass(getClass());
        if (mc == null) {
            throw new java.lang.IllegalStateException("No module found for root resource: " + getClass());
        }
        return mc.get(ctx);
    }

    @GET
    @Path("skin/{path:.*}")
    public Response getSkinResource(@PathParam("path") String path) {
        try {
            ScriptFile file = getModule().getSkinResource("/resources/" + path);
            if (file != null) {
                long lastModified = file.lastModified();
                ResponseBuilder resp = Response.ok(file.getFile())
                                               .lastModified(new Date(lastModified))
                                               .header("Cache-Control", "public")
                                               .header("Server", "Nuxeo/WebEngine-1.0");

                String mimeType = ctx.getEngine().getMimeType(file.getExtension());
                if (mimeType == null) {
                    mimeType = "text/plain";
                }
                resp.type(mimeType);
                return resp.build();
            }
        } catch (IOException e) {
            throw new NuxeoException("Failed to get resource file: " + path, e);
        }
        return Response.status(SC_NOT_FOUND).build();
    }

    /**
     * You should override this method to resolve objects to links. This method is usually called by a search view to
     * generate links for object that are listed
     *
     * @param doc the document
     * @return the link corresponding to that object
     */
    @Override
    public String getLink(DocumentModel doc) {
        return new StringBuilder().append(getPath()).append("/@nxdoc/").append(doc.getId()).toString();
    }

    @Override
    public Object handleError(Throwable t) {
        return t;
    }
}
