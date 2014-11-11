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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RootContainerResource extends FileContainerResource {


    public RootContainerResource(File root) {
        super (root);
    }


    @GET
    @Produces("application/atom+xml")
    public Object listFiles() {
        File[] files = root.listFiles();
        files = root.listFiles();
        if (files == null) {
            files = new File[0];
        }
        return new TemplateView(this, "root-resources.ftl")
                .arg("root", root.getName()).arg("resources", files);
    }
    
    @Path("@schemas")
    public FileContainerResource getSchemas() {
        Environment env = Environment.getDefault();
        return new FileContainerResource(new File(env.getHome(), "schemas"), true);
    }

    @Path("@components")
    public FileContainerResource getComponents() {
        Environment env = Environment.getDefault();
        return new FileContainerResource(new File(env.getData(), "components"), true);
    }
    
    @GET
    @Path("@reload")
    public void reload() {
        try {
            System.out.println("Reloading resources ...");
            Framework.reloadResourceLoader();
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }
    
    
}

