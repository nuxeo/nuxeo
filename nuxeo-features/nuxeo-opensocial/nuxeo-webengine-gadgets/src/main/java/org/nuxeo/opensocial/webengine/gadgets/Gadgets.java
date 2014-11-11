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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.gadgets.service.api.GadgetService;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "gadgets")
@Produces("text/html; charset=UTF-8")
public class Gadgets extends ModuleRoot {

    private final GadgetService gm;

    public Gadgets() throws Exception {
        gm = Framework.getService(GadgetService.class);
    }

    @GET
    public Object getGallery(@QueryParam("cat") String category, @QueryParam("mode") String mode) {

        List<GadgetDeclaration> gadgetList;
        String ftlName = null;
        if (mode==null) {
            mode = "gallery";
        }
        if (mode.equalsIgnoreCase("gallery")) {
            ftlName = "gallery";
        }else if (mode.equalsIgnoreCase("popup")) {
            ftlName = "chooser-body";
        }else {
            return Response.serverError().build();
        }

        if (category==null) {
            gadgetList = gm.getGadgetList();
            category="all";
        } else {
            gadgetList = gm.getGadgetList(category);
        }

        List<String> categories = gm.getGadgetCategory();
        categories.add(0, "all");

        return getView(ftlName).arg("gadgets",
                gadgetList).arg("categories", categories).arg("category", category).arg("mode", mode);
    }

    @GET
    @Path("listGadgets")
    public Object getGadgetList(@QueryParam("cat") String category) {

        List<GadgetDeclaration> gadgetList;

        if (category==null) {
            gadgetList = gm.getGadgetList();
            category="all";
        } else {
            gadgetList = gm.getGadgetList(category);
        }
        return getView("list").arg("gadgets",
                gadgetList);
    }


    @GET
    @Path("sample")
    public Object getSample() {
        return getView("sample-popup");
    }

    @Path("{name}")
    public Object getGadget(@PathParam("name") String gadgetName)
            throws Exception {

        if (gm == null)
            return Response.ok(500).build();

        GadgetDeclaration gadget = gm.getGadget(gadgetName);
        if (gadget != null) {
            return ctx.newObject("gadget", gadget);
            //return new GadgetResource(gadget);
        } else {
            return Response.ok(404).build();
        }
    }

    public String getCategoryLabel(String categoryKey) {

        if (categoryKey==null) {
            return "";
        }
        if (!categoryKey.startsWith("gadget.category")) {
            categoryKey = "gadget.category." + categoryKey;
        }

        String label = getContext().getMessage(categoryKey);
        if (label.startsWith("!")) {
            label = categoryKey.replace("gadget.category.", "");
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
        }
        return label;
    }

}
