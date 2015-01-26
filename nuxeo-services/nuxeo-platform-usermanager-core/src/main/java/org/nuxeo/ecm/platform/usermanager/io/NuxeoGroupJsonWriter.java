/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
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
        jg.writeArrayFieldStart("memberUsers");
        for (String user : group.getMemberUsers()) {
            jg.writeString(user);
        }
        jg.writeEndArray();
        jg.writeArrayFieldStart("memberGroups");
        for (String user : group.getMemberGroups()) {
            jg.writeString(user);
        }
        jg.writeEndArray();
    }

}
