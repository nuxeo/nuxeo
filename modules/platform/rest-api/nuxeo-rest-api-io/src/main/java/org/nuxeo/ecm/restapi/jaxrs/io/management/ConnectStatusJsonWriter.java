/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.time.Instant;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 2025.5
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ConnectStatusJsonWriter extends ExtensibleEntityJsonWriter<ConnectStatus> {

    public static final String ENTITY_TYPE = "connectStatus";

    public ConnectStatusJsonWriter() {
        super(ENTITY_TYPE, ConnectStatus.class);
    }

    @Override
    protected void writeEntityBody(ConnectStatus status, JsonGenerator jg) throws IOException {
        jg.writeBooleanField("registered", status.isRegistered());
        if (status.isRegistered()) {
            var timestamp = status.registrationExpirationTimestamp();
            if (timestamp > -1) {
                jg.writeStringField("registrationExpiration", Instant.ofEpochSecond(timestamp).toString());
            }
            var subscriptionStatus = status.subscriptionStatus();
            if (subscriptionStatus != null) {
                var errorMessage = subscriptionStatus.getErrorMessage();
                if (errorMessage != null) {
                    jg.writeStringField("errorMessage", errorMessage);
                } else {
                    jg.writeStringField("instanceType", subscriptionStatus.getInstanceType().getValue());
                    jg.writeStringField("contractStatus", subscriptionStatus.getContractStatus());
                    jg.writeStringField("description", subscriptionStatus.getDescription());
                    jg.writeStringField("message", subscriptionStatus.getMessage());
                    jg.writeStringField("endDate", subscriptionStatus.getEndDate());
                    jg.writeStringField("CLID", status.clid());
                    jg.writeStringField("CTID", status.ctid());
                }
            }
        }

    }
}
