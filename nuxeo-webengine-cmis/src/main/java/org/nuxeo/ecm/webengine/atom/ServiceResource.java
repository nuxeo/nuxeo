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
package org.nuxeo.ecm.webengine.atom;

import java.io.IOException;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.abdera.AbderaRequest;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * A ServiceResource object is the entry point to an APP server. 
 * The resource is using a ServiceInfo configuration object to define 
 * the service structure. 
 * 
 * TODO: use constants and remove literal strings
 * TODO: add support in webengine to add interceptors when a request is started / finished 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ServiceResource extends ModuleRoot implements UrlResolver {   
         

    /**
     * Get the Atom service definition
     * @return
     */
    public abstract ServiceInfo getServiceInfo();
    
    
    @Override
    protected void initialize(Object... args) {
        // register the URL Resolver for this request
        ctx.setProperty(AbderaRequest.URL_RESOLVER_KEY, this);
    }    
    
    
    @Path("{segment}")
    public Object dispatch(@PathParam("segment") String segment) {
        WorkspaceInfo ws = getServiceInfo().getWorkspace(segment);
        if (ws == null) {
            throw new WebException(404);
        }
        return ws.getResource(ctx);
    }
        
    @GET
    public Response doGet() {
    	try {
        	StringWriter sw = new StringWriter();
        	XMLWriter xw = new XMLWriter(sw, 4);
        	xw.start();
        	getServiceInfo().writeTo(ctx.getURL(), xw);
        	xw.end();
        	return Response.ok(sw.toString()).type("application/atomsvc+xml").build();
    	} catch (IOException e) {
    		throw WebException.wrap("Failed to write down the service document", e);
    	}
    }

    
    @Override
    public Object handleError(WebApplicationException e) {
        
        return super.handleError(e);
    }


}
