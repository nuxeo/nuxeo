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
package org.nuxeo.ecm.restapi.server.jaxrs.config.facets;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.restapi.jaxrs.io.types.Schemas;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "schema")
public class SchemaEndPoint extends DefaultObject {

    @GET
    public Schemas getAll() {
        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        return new Schemas(sm.getSchemas());
    }

    @GET
    @Path("{name}")
    public Schema getSchema(@PathParam("name")
    String name) {
        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        return sm.getSchema(name);
    }

}
