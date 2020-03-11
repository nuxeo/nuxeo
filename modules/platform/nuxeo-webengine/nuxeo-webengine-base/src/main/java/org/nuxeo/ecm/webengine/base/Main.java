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
package org.nuxeo.ecm.webengine.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
        List<ModuleShortcut> list = new ArrayList<>();
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
                if (mc.roots != null && mc.roots.length > 0) {
                    Path path = mc.roots[0].getAnnotation(Path.class);
                    if (path != null) {
                        list.add(new ModuleShortcut(path.value(), mc.name));
                    }
                }
            }
        }
        Collections.sort(list, new Comparator<ModuleShortcut>() {
            @Override
            public int compare(ModuleShortcut o1, ModuleShortcut o2) {
                return o1.title.compareTo(o2.title);
            }
        });
        return getView("index").arg("moduleLinks", list);
    }

    // handle errors

    @Override
    public Object handleError(Throwable t) {
        if (t instanceof WebSecurityException) {
            return Response.status(401).entity(getTemplate("error/error_401.ftl")).type("text/html").build();
        } else if (t instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(getTemplate("error/error_404.ftl")).type("text/html").build();
        } else {
            return super.handleError(t);
        }
    }
}
