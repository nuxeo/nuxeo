/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     tdelprat
 */

package org.nuxeo.ecm.restapi.server.jaxrs.config;

import javax.ws.rs.Path;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type = "config")
public class ConfigEndPoint extends DefaultObject {

    @Path("types")
    public Object getTypes() {
        return newObject("docType");
    }

    @Path("schemas")
    public Object getSchemas() {
        return newObject("schema");
    }

    @Path("facets")
    public Object getDocFacets() {
        return newObject("facet");
    }
}
