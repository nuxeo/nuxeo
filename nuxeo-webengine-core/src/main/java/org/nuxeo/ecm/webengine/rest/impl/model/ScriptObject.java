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

package org.nuxeo.ecm.webengine.rest.impl.model;

import java.io.File;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.annotations.WebObject;
import org.nuxeo.ecm.webengine.rest.impl.DefaultObject;
import org.nuxeo.ecm.webengine.rest.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.rest.scripting.Scripting;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject("Script")
public class ScriptObject extends DefaultObject {

    public ScriptObject() {
    }

    
    @GET @POST @PUT @DELETE @HEAD
    @Path("{path}")
    public Object runScript(@PathParam("path") String path) throws WebException {
        ScriptFile file = ctx.getApplication().getFile(path);
        file = ctx.getApplication().getFile(path);
        if (file == null) {
            return null;
        } else {
            String ext = file.getExtension();
            Scripting scripting = ctx.getEngine().getScripting();
            if (file.isTemplate()) {
                return file;
            } else if (scripting.isScript(ext)) { // script
                return ctx.runScript(file, null);
            } else { // regular file
                File f = file.getFile();
                String ctype = ctx.getEngine().getMimeType(f.getName());
                return Response.ok(f, ctype).build();
            }
        }
    }

}
