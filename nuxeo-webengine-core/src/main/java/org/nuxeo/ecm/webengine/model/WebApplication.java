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

package org.nuxeo.ecm.webengine.model;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebApplication {

    protected WebContext ctx;
    protected Module module;
    protected String path;
  
    public WebApplication() {
        ctx = WebEngine.getActiveContext();
        module = ctx.getEngine().getModule(getClass().getAnnotation(WebModule.class).name());        
        path = guessPath();
        ctx.setApplication(this);
        
        //TODO: invoke application guard if any
    }
    
    public Module getModule() {
        return module;
    }
    
    public WebContext getContext() {
        return ctx; 
    }    
    
    public String getName() {
        return module.getName();
    }


    public Object getErrorView(WebApplicationException e) {
        return null;
    }
    
    
    @GET @POST @PUT @DELETE @HEAD 
    public Object getView() {
        try {
            return getFile("index.ftl");
        } catch (Throwable t) {
            throw WebException.wrap("Failed to find index file", t);
        }
    }
    
    public String getPath() {
        return path; 
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
        } else if (!path.startsWith("/")) {
            path = new StringBuilder().append('/').append(path).toString();
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
    
    
    public ScriptFile getFile(String path) throws WebException {
        return module.getFile(path);
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return module.loadClass(className);
    }

    public ResourceType getType(String typeName) throws TypeNotFoundException {
        return module.getType(typeName);
    }
    
    
    public ResourceType[] getTypes() {
        return module.getTypes();
    }
    
    public ServiceType[] getServices() {
      return module.getServices();  
    }
    
    public ServiceType getService(Resource ctx, String name) throws ServiceNotFoundException {
        return module.getService(ctx, name);  
    }
    
    public List<ServiceType> getServices(Resource ctx) {
        return module.getServices(ctx);
    }
    
    public List<String> getServiceNames(Resource ctx) {
        return module.getServiceNames(ctx);        
    }

    public List<ServiceType> getEnabledServices(Resource ctx) {
        return module.getEnabledServices(ctx);
    }
    
    public List<String> getEnabledServiceNames(Resource ctx) {
        return module.getEnabledServiceNames(ctx);        
    }
}
