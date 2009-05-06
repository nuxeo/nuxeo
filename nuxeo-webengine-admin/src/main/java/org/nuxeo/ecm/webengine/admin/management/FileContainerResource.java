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
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileContainerResource {

    protected File root;

    public FileContainerResource(File root) {
        this.root = root;
    }

    public File getRoot() {
        return root;
    }

    @GET
    @Produces("application/atom+xml")
    public Object listFiles() {
        return new TemplateView(this, "resources.ftl")
                .arg("root", root.getName()).arg("resources", root.listFiles());
    }

    @POST
    public Response postFile(@QueryParam("name") String name) {
        File file = new File(root, name);
        HttpServletRequest req = WebEngine.getActiveContext().getRequest();
        try {
            FileUtils.copyToFile(req.getInputStream(), file);
            return Response.ok().build();
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("{name}")
    public Object getFile(@PathParam("name") String name) {
        File file = new File(root, name);
        if (!file.isFile()) {
            return Response.ok(404).build();
        }
        int p = name.lastIndexOf('.');
        if (p > -1) {
            String mime = WebEngine.getActiveContext().getEngine().getMimeType(name.substring(p + 1));
            return Response.ok(file).type(mime).build();
        }

        return file;
    }

    @PUT
    @Path("{name}")
    public Response updateFile(@PathParam("name") String name) {
        File file = new File(root, name);
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

    @DELETE
    @Path("{name}")
    public Response deleteFile(@PathParam("name") String name) {
        new File(root, name).delete();
        return Response.ok().build();
    }

    public static File getContainerRoot(String type) {
        Environment env = Environment.getDefault();
        if (env.getConfig().getName().equals(type)) {
            return env.getConfig();
        } else if ("schemas".equals(type)) {
            return new File(env.getHome(), "schemas");
        } else if ("extensions".equals(type)) {
            return new File(env.getHome(), "extensions");
        } else {
            return new File(env.getHome(), "resources/" + type);
        }
    }

}
