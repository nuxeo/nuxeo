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
 */
package org.nuxeo.ecm.webengine.gwt;

import java.io.File;
import java.io.FileNotFoundException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * Server Entry point to a server GWT module. Must be extended by the webengine resource used to load the studio
 * application. The <code>@GET</code> method must be defined by the subclasses to point to the main HTML file of the GWT
 * module. Example:
 *
 * <pre>
 *     <code>@GET</code> <code>@Produces("text/html")</code>
 *     public Object getIndex() {
 *         return getTemplate("index.ftl");
 *     }
 * </pre>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class GwtResource extends ModuleRoot {

    /**
     * Gets a static resource from the GWT module.
     * @throws FileNotFoundException
     */
    @GET
    @Path("{path:.*}")
    public Response getResource(@PathParam("path") String path) throws FileNotFoundException {
        ctx.getRequest().setAttribute("org.nuxeo.webengine.DisableAutoHeaders", "true");
        File file = Framework.getService(GwtResolver.class).resolve(path);
        if (file != null && file.isFile()) {
            ResponseBuilder resp = Response.ok(file);
            String fpath = file.getPath();
            int p = fpath.lastIndexOf('.');
            String ext = "";
            if (p > -1) {
                ext = fpath.substring(p + 1);
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
