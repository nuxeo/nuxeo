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
package org.nuxeo.ecm.webengine.admin.management;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileResource {

    protected File file;
    protected boolean isReadOnly;
    
    public FileResource(File file) {
        this (file, false);
    }
    
    public FileResource(File file, boolean isReadOnly) {
        this.file = file;
        this.isReadOnly = isReadOnly;
    }
    
    @GET
    public Response get() {
        if (file.isDirectory()) {
            return Response.ok(404).build(); 
        }
        if (!file.isFile()) {
            return Response.ok(404).build();
        }
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p > -1) {
            String mime = WebEngine.getActiveContext().getEngine().getMimeType(name.substring(p + 1));
            if (mime == null) {
                if (name.endsWith(".xsd")) {
                    mime = "text/xml";
                }
            }
            return Response.ok(file).type(mime).build();
        }
        return Response.ok(file).type("application/octet-stream").build();
    }
    
    @DELETE
    public Response deleteFile() {
        if (isReadOnly) {
            return Response.ok(403).build();
        }
        if (!file.isFile()) {
            return Response.ok(404).build(); //TODO send correct code
        }
        file.delete();
        return Response.ok().build();
    }

    @PUT
    public Response updateFile() {
        if (isReadOnly) {
            return Response.ok(403).build();
        }
        if (!file.isFile()) {
            return Response.ok(404).build(); //TODO send correct code
        }
        HttpServletRequest req = WebEngine.getActiveContext().getRequest();
        try {
            FileUtils.copyToFile(req.getInputStream(), file);
            return Response.ok().build();
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

}
