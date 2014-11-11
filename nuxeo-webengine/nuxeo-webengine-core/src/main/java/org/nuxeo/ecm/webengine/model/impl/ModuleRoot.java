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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.ModuleResource;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ModuleRoot extends DefaultObject implements ModuleResource {

    @GET
    @Path("skin/{path:.*}")
    public Response getSkinResource(@PathParam("path") String path) {
        try {
            ScriptFile file = getModule().getSkinResource("/resources/" + path);
            if (file != null) {
                long lastModified = file.lastModified();
                ResponseBuilder resp = Response.ok(file.getFile()).lastModified(
                        new Date(lastModified)).header("Cache-Control",
                        "public").header("Server", "Nuxeo/WebEngine-1.0");

                String mimeType = ctx.getEngine().getMimeType(
                        file.getExtension());
                if (mimeType == null) {
                    mimeType = "text/plain";
                }
                resp.type(mimeType);
                return resp.build();
            }
        } catch (IOException e) {
            throw WebException.wrap("Failed to get resource file: " + path, e);
        }
        return Response.status(404).build();
    }

    /**
     * You should override this method to resolve objects to links. This method
     * is usually called by a search view to generate links for object that are
     * listed
     *
     * @param doc the document
     * @return the link corresponding to that object
     */
    public String getLink(DocumentModel doc) {
        return new StringBuilder().append(getPath()).append("/@nxdoc/").append(
                doc.getId()).toString();
    }

    public Object handleError(WebApplicationException e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return Response.status(500).entity(sw.toString()).build();
    }

}
