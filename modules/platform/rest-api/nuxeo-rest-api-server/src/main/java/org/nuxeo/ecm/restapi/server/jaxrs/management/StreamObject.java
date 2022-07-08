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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.INTROSPECTION_KEY;
import static org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionComputation.INTROSPECTION_KV_STORE;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.introspection.StreamIntrospectionConverter;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.stream.StreamService;

/**
 * Nuxeo Stream Introspection endpoint
 *
 * @since 11.5
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "stream")
@Produces(APPLICATION_JSON)
public class StreamObject extends AbstractResource<ResourceTypeImpl> {

    protected static final String PUML_FORMAT = "puml";

    @GET
    public String doGet(@QueryParam("format") String format) {
        String json = getJson();
        if (PUML_FORMAT.equals(format)) {
            return new StreamIntrospectionConverter(json).getPuml();
        }
        return json;
    }

    /**
     * @deprecated since 2022.21 use {@link StreamObject#doGet(String)} with format=puml instead.
     */
    @Deprecated
    @GET
    @Path("/puml")
    public String doGetPuml() {
        return doGet(PUML_FORMAT);
    }

    @GET
    @Path("/streams")
    public String listStreams() {
        String json = getJson();
        return new StreamIntrospectionConverter(json).getStreams();
    }

    @GET
    @Path("/consumers")
    public String listConsumers(@QueryParam("stream") String stream) {
        String json = getJson();
        return new StreamIntrospectionConverter(json).getConsumers(stream);
    }

    @DELETE
    @Path("/consumer/stop")
    public void stopConsumer(@QueryParam("consumer") String consumer) {
        // TODO: handle global param and use pub sub to stop all consumer in the cluster
        Framework.getService(StreamService.class).stopComputation(Name.ofUrn(consumer));
    }

    @PUT
    @Path("/consumer/start")
    public void startConsumer(@QueryParam("consumer") String consumer) {
        // TODO: handle global param and use pub sub to stop all consumer in the cluster
        Framework.getService(StreamService.class).restartComputation(Name.ofUrn(consumer));
    }


    protected String getJson() {
        return getKvStore().getString(INTROSPECTION_KEY);
    }

    protected KeyValueStore getKvStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(INTROSPECTION_KV_STORE);
    }
}
