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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileContainerResource {

    protected File root;
    protected boolean isReadOnly;

    public FileContainerResource(File root) {
        this (root, false);
    }

    public FileContainerResource(File root, boolean isReadOnly) {
        this.root = root;
        this.isReadOnly = isReadOnly;
    }

    public File getRoot() {
        return root;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    @GET
    @Produces("application/atom+xml")
    public Object listFiles() {
        File[] files = root.listFiles();
        files = root.listFiles();
        if (files == null) {
            files = new File[0];
        }
        return new TemplateView(this, "resources.ftl")
                .arg("root", root.getName()).arg("resources", files);
    }


    @POST
    public Response postFile(@QueryParam("file") String file, @QueryParam("dir") String dir) {
        if (isReadOnly) {
            return Response.ok(403).build();
        }
        boolean isDir = false;
        String name = null;
        if (file == null) {
            if (dir == null) {
                return Response.ok(403).build();
            }
            name = dir;
            isDir = true;
        } else {
            name = file;
        }
        if (name.contains("..")) {
            return Response.ok(403).build();
        }
        File f = new File(root, name);
        if (isDir) {
            // create dir
            f.mkdirs();
            return Response.ok().build();
        }
        // create file
        f.getParentFile().mkdirs();
        HttpServletRequest req = WebEngine.getActiveContext().getRequest();
        try {
            FileUtils.copyToFile(req.getInputStream(), f);
            return Response.ok().build();
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }


    @Path("{name}")
    public Object getFile(@PathParam("name") String name) {
        File file = new File(root, name);
        if (file.isDirectory()) {
            return new FileContainerResource(file, isReadOnly);
        }
        return new FileResource(file, isReadOnly);
    }

    @PUT
    public Response updateFile() {
        if (isReadOnly) {
            return Response.ok(403).build();
        }
        if (!root.exists()) {
            root.mkdirs();
        } else{
            root.setLastModified(new Date().getTime());
        }
        return Response.ok().build();
    }

    @DELETE
    public Response deleteFile() {
        if (isReadOnly) {
            return Response.ok(403).build();
        }
        if (!root.isDirectory()) {
            return Response.ok(404).build();
        }
        FileUtils.deleteTree(root);
        return Response.ok().build();
    }


    public String getLastModified(File file) {
        return formatDate(new Date(file.lastModified()));
    }

    public static String formatDate(Date date) {
        StringBuilder sb = new StringBuilder();
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(date);
        sb.append(c.get(Calendar.YEAR));
        sb.append('-');
        int f = c.get(Calendar.MONTH);
        if (f < 9) sb.append('0');
        sb.append(f+1);
        sb.append('-');
        f = c.get(Calendar.DATE);
        if (f < 10) sb.append('0');
        sb.append(f);
        sb.append('T');
        f = c.get(Calendar.HOUR_OF_DAY);
        if (f < 10) sb.append('0');
        sb.append(f);
        sb.append(':');
        f = c.get(Calendar.MINUTE);
        if (f < 10) sb.append('0');
        sb.append(f);
        sb.append(':');
        f = c.get(Calendar.SECOND);
        if (f < 10) sb.append('0');
        sb.append(f);
        sb.append('.');
        f = c.get(Calendar.MILLISECOND);
        if (f < 100) sb.append('0');
        if (f < 10) sb.append('0');
        sb.append(f);
        sb.append('Z');
        return sb.toString();
      }

}
