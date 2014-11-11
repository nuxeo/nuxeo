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

import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebEngine;

public class InputStreamResource {

    protected Response getObject(InputStream gadgetResource, String path) {
        if (gadgetResource == null) {
            return Response.ok(404).build();
        }

        int p = path.lastIndexOf('.');
        if (p > -1) {
            String mime = WebEngine.getActiveContext().getEngine().getMimeType(
                    path.substring(p + 1));
            if (mime == null) {
                if (path.endsWith(".xsd")) {
                    mime = "text/xml";
                }
            }

            // To Avoid a small bug....
            if ("text/plain".equals(mime)) {
                mime = "text/html";
            }

            return Response.ok(new GadgetStream(gadgetResource)).type(mime).build();
        }
        return Response.ok(new GadgetStream(gadgetResource)).type(
                "application/octet-stream").build();

    }
}
