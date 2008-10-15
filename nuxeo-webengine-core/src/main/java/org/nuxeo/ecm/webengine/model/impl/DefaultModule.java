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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.ModuleResource;
import org.nuxeo.ecm.webengine.model.ModuleType;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.ResourceType;
import org.nuxeo.ecm.webengine.model.ServiceResource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebModule;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultModule extends AbstractResource<ModuleType> implements ModuleResource {

    protected Module module;
  
    public DefaultModule() {        
        ctx = WebEngine.getActiveContext();
        module = ctx.getEngine().getModule(getClass().getAnnotation(WebModule.class).name());
        type = module.getModuleType();
        path = guessPath();
        setRoot(true);
        ctx.push(this);
        if (!this.type.getGuard().check(this)) {
            throw new WebSecurityException("Failed to initialize object: "+getPath()+". Object is not accessible in the current context", getPath());
        }        
    }
  
    @Path(value="@{segment}")
    public ServiceResource disptachService(@PathParam("segment") String serviceName) throws WebException {
        return ctx.newService(this, serviceName);
    }    
   
    @Override
    public Resource initialize(WebContext ctx, ResourceType type,
            Object... args) throws WebException {
        return this; // initialization is done in constructor
    }
    
    @Override
    public Module getModule() {
        return module;
    }
    
    public boolean isService() {
        return false;
    }
    
    public String getName() {
        return module.getName();
    }

    public Object handleError(WebApplicationException e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return Response.status(500).entity(sw.toString()).build();
    }
        

    /**
     * This method try to guess the actual path under this resource was called.
     * RestEasy version 1.0-beta-8 has a bug in UriInfo.getMatchedURIs(). See:
     * https://jira.jboss.org/jira/browse/RESTEASY-100
     * <p>
     * So we cannot use JAX-RS API to correctly retrieve the paths.
     * This method should be replaced with {@link #_guessPath()} when the bug will be fixed (in RC1)
     * @return
     */
    protected String guessPath() {
        Path p = getClass().getAnnotation(Path.class);
        String path = p.value();
        if (path.indexOf('{') > -1) {
            path = _guessPath();
        } 
        if (!path.startsWith("/")) {
            path = new StringBuilder().append(ctx.getBasePath()).append('/').append(path).toString();
        } else {
            path = new StringBuilder().append(ctx.getBasePath()).append(path).toString();
        }
        return path;
    }
    
    /**
     * The correct method to guess the path that is not working for now because of a bug in RestEasy
     * @return
     */
    protected String _guessPath() {
        return ctx.getUriInfo().getMatchedURIs().get(0);        
    }
            
}
