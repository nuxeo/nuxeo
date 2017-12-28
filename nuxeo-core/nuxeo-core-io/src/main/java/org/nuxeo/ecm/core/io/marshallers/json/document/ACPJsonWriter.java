/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.utils.DateParser;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Convert {@link ACP} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing {@link ACP}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(ACP, JsonGenerator)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"acls",
 *   "acl": [
 *     {
 *       "name":"inherited",
 *       "ace":[
 *         {
 *           "username":"administrators",
 *           "permission":"Everything",
 *           "granted":true
 *         },
 *         ...
 *       ]
 *     },
 *     ...
 *   ]
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ACPJsonWriter extends ExtensibleEntityJsonWriter<ACP> {

    public static final String ENTITY_TYPE = "acls";

    public ACPJsonWriter() {
        super(ENTITY_TYPE, ACP.class);
    }

    @Override
    protected void writeEntityBody(ACP acp, JsonGenerator jg) throws IOException {
        jg.writeArrayFieldStart("acl");
        for (ACL acl : acp.getACLs()) {
            jg.writeStartObject();
            jg.writeStringField("name", acl.getName());
            jg.writeArrayFieldStart("ace");
            for (ACE ace : acl.getACEs()) {
                jg.writeStartObject();
                jg.writeStringField("id", ace.getId());
                jg.writeStringField("username", ace.getUsername());
                jg.writeStringField("permission", ace.getPermission());
                jg.writeBooleanField("granted", ace.isGranted());
                jg.writeStringField("creator", ace.getCreator());
                jg.writeStringField("begin",
                        ace.getBegin() != null ? DateParser.formatW3CDateTime(ace.getBegin().getTime()) : null);
                jg.writeStringField("end", ace.getEnd() != null ? DateParser.formatW3CDateTime(ace.getEnd().getTime())
                        : null);
                jg.writeStringField("status", ace.getStatus().toString().toLowerCase());
                jg.writeEndObject();
            }

            jg.writeEndArray();
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

}
