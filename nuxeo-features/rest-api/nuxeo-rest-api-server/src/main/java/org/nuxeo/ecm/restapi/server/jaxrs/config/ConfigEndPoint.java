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
