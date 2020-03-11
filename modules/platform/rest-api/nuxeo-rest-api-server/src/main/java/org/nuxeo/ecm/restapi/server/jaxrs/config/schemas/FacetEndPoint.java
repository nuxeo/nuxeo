/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 */
package org.nuxeo.ecm.restapi.server.jaxrs.config.schemas;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.restapi.jaxrs.io.types.Facets;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "facet")
public class FacetEndPoint extends DefaultObject {

    @GET
    public Facets getAll() {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        return new Facets(sm.getFacets());
    }

    @GET
    @Path("{name}")
    public CompositeType getSchema(@PathParam("name") String name) {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        return sm.getFacet(name);
    }

}
