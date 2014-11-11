/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.webengine.gadgets;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.ecm.webengine.model.view.TemplateView;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "Gadgets")
@Produces("text/html; charset=UTF-8")
public class Gadgets extends ModuleRoot {

    /**
     * Jar url prefix for gwt resources
     */
    public static final String GWT_FOLDER = "/org.nuxeo.opensocial.container.ContainerEntryPoint/";

    private final GadgetService gm;

    public Gadgets() throws Exception {
        gm = Framework.getService(GadgetService.class);
    }

    @GET
    public Object getGadgetList() {
        List<GadgetDeclaration> gadgetList = gm.getGadgetList();
        return new TemplateView(this, "gadgetslist.tpl").arg("gadgets",
                gadgetList);
    }

    @Path("/manager/{name}")
    public Object getGadget(@PathParam("name") String gadgetName)
            throws Exception {

        if (gm == null)
            return Response.ok(500).build();

        GadgetDeclaration gadget = gm.getGadget(gadgetName);
        if (gadget != null) {
            return new GadgetResource(gadget);
        } else {
            return Response.ok(404).build();
        }
    }

    @Path("/resources/{name:.*}")
    public Object getGwtResource(@PathParam("name") String name)
            throws Exception {
        return new UrlResource(GWT_FOLDER.concat(name));
    }

}
