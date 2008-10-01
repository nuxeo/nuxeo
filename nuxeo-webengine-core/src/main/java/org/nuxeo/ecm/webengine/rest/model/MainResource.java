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

package org.nuxeo.ecm.webengine.rest.model;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.WebEngine2;
import org.nuxeo.ecm.webengine.rest.annotations.WebApp;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MainResource {

    protected WebContext2 ctx;
    protected WebApplication app;
    protected String path;
  
    public MainResource() {
        ctx = WebEngine2.getActiveContext();
        app = ctx.getEngine().getApplication(getApplicationName());
        ctx.setApplication(app);        
        path = guessPath();
        ctx.setRootResource(this);
        
        //TODO: invoke application guard if any
        
        System.out.println("@@@ MAIN: "+getClass().getSimpleName()+" >> "+path+" >> "+ctx.getUriInfo().getMatchedResources().getClass().getSimpleName()+" => "+ctx.getUriInfo().getMatchedURIs());
    }
    
    public WebApplication getApplication() {
        return app;
    }
    
    public WebContext2 getContext() {
        return ctx; 
    }    
    
    protected String getApplicationName() {
        WebApp anno = (WebApp)this.getClass().getAnnotation(WebApp.class);
        if (anno != null) {
            return anno.name();
        }
        throw new UnsupportedOperationException("This method must be implement by derived main resource classes");
    }

    @Path(value="{segment}")
    public Object dispatch(@PathParam("segment") String segment) throws WebException {
        Object result = resolveObject(segment);        
        if (result == null) {
            throw new NoSuchResourceException(segment);
        }
        return  result;
    }
    
    protected Object resolveObject(String segment) throws WebException {
        throw new NoSuchResourceException("No Such Object: "+segment);
    }

    public Object getErrorView(WebApplicationException e) {
        return null;
    }
    
    
    @GET
    public Object getIndexView() {
        try {
            return app.getFile("index.ftl");
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
