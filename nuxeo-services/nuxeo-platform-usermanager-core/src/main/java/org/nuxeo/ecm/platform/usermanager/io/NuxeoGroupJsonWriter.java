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

package org.nuxeo.ecm.platform.usermanager.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.thoughtworks.xstream.io.json.JsonWriter;

/**
 * Convert {@link NuxeoGroup} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link NuxeoGroup}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(NuxeoGroup, JsonWriter)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"group",
 *   "groupname": "GROUP_NAME",
 *   "grouplabel": "GROUP_DISPLAY_NAME",
 *   "memberUsers": [
 *     "USERNAME1",
 *     "USERNAME2",
 *     ...
 *   ],
 *   "memberGroups": [
 *     "GROUPNAME1",
 *     "GROUPNAME2",
 *     ...
 *   ]
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoGroupJsonWriter extends ExtensibleEntityJsonWriter<NuxeoGroup> {

    public static final String ENTITY_TYPE = "group";

    public NuxeoGroupJsonWriter() {
        super(ENTITY_TYPE, NuxeoGroup.class);
    }

    @Override
    protected void writeEntityBody(NuxeoGroup group, JsonGenerator jg) throws IOException {
        jg.writeStringField("groupname", group.getName());
        jg.writeStringField("grouplabel", group.getLabel());
        if (ctx.getFetched(ENTITY_TYPE).contains("memberUsers")) {
            jg.writeArrayFieldStart("memberUsers");
            for (String user : group.getMemberUsers()) {
                jg.writeString(user);
            }
            jg.writeEndArray();
        }
        if (ctx.getFetched(ENTITY_TYPE).contains("memberGroups")) {
            jg.writeArrayFieldStart("memberGroups");
            for (String user : group.getMemberGroups()) {
                jg.writeString(user);
            }
            jg.writeEndArray();
        }
    }

}
