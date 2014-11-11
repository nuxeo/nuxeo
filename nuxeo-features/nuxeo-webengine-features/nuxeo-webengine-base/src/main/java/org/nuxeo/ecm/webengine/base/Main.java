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
package org.nuxeo.ecm.webengine.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleConfiguration;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.ecm.webengine.model.impl.ModuleShortcut;

/**
 * The web entry point of WebEngine.
 * <p>
 * This is a mix between an webengine module and a JAX-RS root resource
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Path("/")
@Produces("text/html; charset=UTF-8")
@WebObject(type = "base")
public class Main extends ModuleRoot {

    @GET
    public Object doGet() {
        ArrayList<ModuleShortcut> list = new ArrayList<ModuleShortcut>();
        for (ModuleConfiguration mc : ctx.getEngine().getModuleManager().getModules()) {
            List<ModuleShortcut> items = mc.getShortcuts();
            if (items != null && !items.isEmpty()) {
                for (ModuleShortcut item : items) {
                    if (item.title == null) {
                        item.title = mc.name;
                    }
                }
                list.addAll(items);
            } else if (!mc.isHeadless) {
                if (mc.roots != null && mc.roots.length > 0){
                    Path path = mc.roots[0].getAnnotation(Path.class);
                    if (path != null) {
                        list.add(new ModuleShortcut(path.value(), mc.name));
                    }
                }
            }
        }
        Collections.sort(list, new Comparator<ModuleShortcut>() {
            public int compare(ModuleShortcut o1, ModuleShortcut o2) {
                return o1.title.compareTo(o2.title);
            }
        });
        return getView("index").arg("moduleLinks", list);
    }

    @GET
    @Path("help")
    public Object getHelp() {
        return getTemplate("help/help.ftl");
    }

    @GET
    @Path("about")
    public Object getAbout() {
        return getTemplate("help/about.ftl");
    }

    // handle errors
    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity(
                    getTemplate("error/error_401.ftl")).type("text/html").build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(
                    getTemplate("error/error_404.ftl")).type("text/html").build();
        } else {
            return super.handleError(e);
        }
    }

}
