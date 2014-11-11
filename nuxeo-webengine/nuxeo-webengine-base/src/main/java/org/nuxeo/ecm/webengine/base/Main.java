/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.base;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleImpl;
import org.nuxeo.ecm.webengine.model.impl.ModuleManager;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;

/**
 * The web entry point of WebEngine.
 * <p>
 * This is a mix between an webengine module and a JAX-RS root resource
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Path("/")
@Produces("text/html; charset=UTF-8")
@WebObject(type = "base")
public class Main extends ModuleRoot {

    protected final ModuleManager mgr;
    protected Module module;

    public Main() {
        ctx = WebEngine.getActiveContext();
        path = ctx.getBasePath();
        mgr = ctx.getEngine().getModuleManager();
    }

    /**
     * Initializes the current module context.
     */
    protected void init() {
        ModuleConfiguration mc = mgr.getRootModule();
        if (mc == null) {
            throw new WebResourceNotFoundException("Root module not registered");
        }
        module = mc.get();
        ((AbstractWebContext) ctx).setModule(module);
        type = (ResourceTypeImpl) ((ModuleImpl) module).getRootType();
        setRoot(true);
        ctx.push(this);
    }

    @GET
    public Object doGet() {
        init();
        return getView("index");
    }

    @GET
    @Path("help")
    public Object getHelp() {
        init();
        return getTemplate("help/help.ftl");
    }

    @GET
    @Path("about")
    public Object getAbout() {
        init();
        return getTemplate("help/about.ftl");
    }

    @Path("{path}")
    public Object dispatch(@PathParam("path") String path) {
        ModuleConfiguration md = mgr.getModuleByPath(path);
        if (md != null) {
            return md.get().getRootObject(ctx);
        } else {
            throw new WebResourceNotFoundException("No resource found");
        }
    }

    // handle errors
    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity(getTemplate("error/error_401.ftl")).build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(getTemplate("error/error_404.ftl")).build();
        } else {
            return super.handleError(e);
        }
    }

}
