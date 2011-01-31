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
 *     Nuxeo
 */

package org.nuxeo.opensocial.webengine.gadgets;

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.opensocial.gadgets.service.InternalGadgetDescriptor;
import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;
import org.nuxeo.opensocial.webengine.gadgets.render.GadgetSpecView;

@WebObject(type = "gadget")
public class GadgetResource extends InputStreamResource {

    protected GadgetDeclaration gadget;

    public GadgetResource() {
    }

    public GadgetResource(GadgetDeclaration gadget) {
        this.gadget = gadget;
    }

    @Override
    protected void initialize(Object... args) {
        gadget = (GadgetDeclaration) args[0];
    }

    @GET
    @Path("{filename:.*}")
    public Object getGadgetFile(@PathParam("filename") String fileName)
            throws Exception {

        if (gadget.isExternal()) {
            return Response.seeOther(gadget.getGadgetDefinition().toURI()).build();
        }

        InternalGadgetDescriptor iGadget = (InternalGadgetDescriptor) gadget;
        InputStream in=null;
        if (iGadget.getEntryPoint().equals(fileName)) {
            in = GadgetSpecView.render(iGadget, null);
        } else if (fileName.startsWith("messages_")) {
            in = GadgetSpecView.i18n.getTranslationFile(iGadget, fileName);
        }
        else {
            in = getResourceAsStream(fileName);
        }
        if (in==null) {
            return Response.status(404).build();
        }
        return getObject(in, fileName);
    }

    protected InputStream getResourceAsStream(String fileName) throws Exception {
        // lookup in gadget directory (gadget specific resource)
        InputStream is = gadget.getResourceAsStream(fileName);
        if (is==null) {
            String suffix="img/";
            if (fileName.endsWith(".css")) {
                suffix="css/";
            }
            else if (fileName.endsWith(".js")) {
                suffix="scripts/";
            }
            // lookup in gadget bundle resources (bundle level shared resource)
            is = gadget.getResourceAsStream( suffix + fileName);

            if (is==null) {
                // lookup in root bundle (global shared resource)
                is = this.getClass().getClassLoader().getResourceAsStream("skin/resources/" + suffix + fileName);
            }
        }
        return is;
    }

    @GET
    @Path("getDetails")
    @Produces("text/html; charset=UTF-8")
    public Object getDetails() {
        return getView("details").arg("gadget", gadget);
    }

}
