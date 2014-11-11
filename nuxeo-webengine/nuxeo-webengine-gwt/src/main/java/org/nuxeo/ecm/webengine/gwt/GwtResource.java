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
package org.nuxeo.ecm.webengine.gwt;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.nuxeo.ecm.webengine.model.impl.DefaultObject;


/**
 * Server Entry point to a server GWT module.
 * Must be extended by the webengine resource used to load the studio application.
 * The <code>@GET</code> method must be defined by the subclasses to point to the main HTML file of
 * the GWT module. 
 * Example:
 * <pre>
    <code>@GET</code> <code>@Produces("text/html")</code>
    public Object getIndex() {
        return getTemplate("index.ftl");
    } 
 * </pre>
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class GwtResource extends DefaultObject {

    /**
     * Gets a static resource from the GWT module.
     */
    @GET
    @Path("{path:.*}")
    public Response getResource(@PathParam("path") String path) {
        //System.out.println(">>> "+GWT_ROOT.getAbsolutePath());
        // avoid putting automatic no cache headers
        ctx.getRequest().setAttribute("org.nuxeo.webengine.DisableAutoHeaders", "true");
        File file = new File(GwtBundleActivator.GWT_ROOT, path);
        if (file.isFile()) {
            ResponseBuilder resp = Response.ok(file);
            String fpath = file.getPath();
            int p = fpath.lastIndexOf('.');
            String ext = "";
            if (p > -1) {
                ext = fpath.substring(p+1);
            }
            String mimeType = ctx.getEngine().getMimeType(ext);
            if (mimeType == null) {
                mimeType = "text/plain";
            }
            resp.type(mimeType);
            return resp.build();
        }
        return Response.status(404).build();
    }

}
