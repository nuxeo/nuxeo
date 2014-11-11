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

import java.io.InputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.opensocial.gadgets.service.api.GadgetDeclaration;

public class GadgetResource extends InputStreamResource {

    GadgetDeclaration gadget;

    public GadgetResource(GadgetDeclaration gadget) {
        this.gadget = gadget;
    }

    @GET
    @Path("{filename:.*}")
    public Object getGadgetFile(@PathParam("filename") String fileName)
            throws Exception {
        InputStream in = gadget.getResourceAsStream(fileName);
        return getObject(in, fileName);
    }
}
