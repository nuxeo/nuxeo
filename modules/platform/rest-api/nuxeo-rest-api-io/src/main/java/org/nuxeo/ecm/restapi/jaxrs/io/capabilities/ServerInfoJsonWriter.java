/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.jaxrs.io.capabilities;

import static org.nuxeo.common.function.ThrowableConsumer.asConsumer;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 11.5
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ServerInfoJsonWriter extends ExtensibleEntityJsonWriter<ServerInfo> {

    public static final String ENTITY_TYPE = "server";

    public ServerInfoJsonWriter() {
        super(ENTITY_TYPE, ServerInfo.class);
    }

    @Override
    protected void writeEntityBody(ServerInfo entity, JsonGenerator jg) throws IOException {
        jg.writeStringField("distributionName", entity.getDistributionName());
        jg.writeStringField("distributionVersion", entity.getDistributionVersion());
        jg.writeStringField("distributionServer", entity.getDistributionServer());
        entity.getHotfixVersion().ifPresent(asConsumer(h -> jg.writeStringField("hotfixVersion", h)));
        jg.writeBooleanField("clusterEnabled", entity.isClusterEnabled());
        jg.writeStringField("clusterNodeId", entity.getClusterNodeId());

    }
}
