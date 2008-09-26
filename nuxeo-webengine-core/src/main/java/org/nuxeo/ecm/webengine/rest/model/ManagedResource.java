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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;

/**
 * Managed resources are singleton root resources that are bound to WEB paths from a configuration file.
 * These resources are initialized using a configuration object.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@ProduceMime({"text/html", "*/*"})
public class ManagedResource {

    protected WebApplication app;
    
    public ManagedResource(WebApplication app) {
        this.app = app;
    }
    
    public WebApplication getConfiguration() {
        return app;
    }
    
    protected WebType getResourceType(WebContext2 ctx) throws WebException {
        return null;// cfg.getDefaultType();
    }
    
    protected WebObject resolve(WebContext2 ctx, String path) throws WebException {
        WebObject obj = getResourceType(ctx).newInstance();
        if (obj != null) {
            obj.initialize(ctx, path);
            return obj;
        }
        return obj;
    }

    @GET
    @POST
    @PUT
    @DELETE
    @HEAD
    @Path(value="{path}", limited=false)
    public WebObject dispatch(@PathParam("path") String path, @Context WebContext2 ctx) throws Exception {
        ctx.setApplication(app);
        return resolve(ctx, path);
    }

}
