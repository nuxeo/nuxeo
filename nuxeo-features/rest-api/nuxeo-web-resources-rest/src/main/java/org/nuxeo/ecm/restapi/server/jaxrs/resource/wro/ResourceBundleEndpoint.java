/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.restapi.server.jaxrs.resource.wro;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * Hook on REST API URLs (just forwards to the Wro servlet).
 *
 * @since 7.3
 */
@WebObject(type = "resource")
public class ResourceBundleEndpoint extends DefaultObject {

    @GET
    @Path("bundle/{var:.*}")
    public Object redirect() {
        return new ResourceBundleDispatcher();
    }

    /**
     * Phony class to handle forward to servlet.
     */
    public class ResourceBundleDispatcher {
    }

}
