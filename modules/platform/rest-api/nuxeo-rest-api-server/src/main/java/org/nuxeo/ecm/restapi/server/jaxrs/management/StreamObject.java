/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.INTROSPECTION_KEY;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.INTROSPECTION_KV_STORE;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionConverter;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Nuxeo Stream Introspection endpoint
 *
 * @since 11.5
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "stream")
@Produces(APPLICATION_JSON)
public class StreamObject extends AbstractResource<ResourceTypeImpl> {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @GET
    public String doGetJson() {
        return getJson();
    }

    @GET
    @Path("/puml")
    public String doGetPuml() {
        String json = getJson();
        return new StreamIntrospectionConverter(json).getPuml();
    }

    protected String getJson() {
        return getKvStore().getString(INTROSPECTION_KEY);
    }

    protected KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(INTROSPECTION_KV_STORE);
    }
}
