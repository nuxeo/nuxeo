/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertyJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Convert {@link NuxeoGroup} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link NuxeoGroup}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link NuxeoGroupJsonWriter#extend(Object, JsonGenerator)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"group",
 *   "groupname": "GROUP_NAME", <- deprecated, for backward compatibility
 *   "grouplabel": "GROUP_DISPLAY_NAME", <- deprecated, for backward compatibility
 *   "id": "GROUP_NAME",
 *   "properties":{   <- depending on the group schema / format is managed by {@link DocumentPropertyJsonWriter }
 *     "groupname":"GROUP_NAME",
 *     "grouplabel":"GROUP_DISPLAY_NAME",
 *     "description": "GROUP_DESCRIPTION"
 *   },
 *   "memberUsers": [
 *     "USERNAME1",
 *     "USERNAME2",
 *     ...
 *   ],
 *   "memberGroups": [
 *     "GROUPNAME1",
 *     "GROUPNAME2",
 *     ...
 *   ],
 *   "parentGroups": [
 *     "GROUPNAME1",
 *     "GROUPNAME2",
 *     ...
 *   ]
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 * </p>
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoGroupJsonWriter extends ExtensibleEntityJsonWriter<NuxeoGroup> {

    public static final String ENTITY_TYPE = "group";

    /**
     * @since 9.3
     */
    public static final String MEMBER_USERS_FETCH_PROPERTY = "memberUsers";

    /**
     * @since 9.3
     */
    public static final String MEMBER_GROUPS_FETCH_PROPERTY = "memberGroups";

    /**
     * @since 9.3
     */
    public static final String PARENT_GROUPS_FETCH_PROPERTY = "parentGroups";

    /**
     * @since 9.3
     */
    public static final String GROUP_NAME_COMPATIBILITY_FIELD = "groupname";

    /**
     * @since 9.3
     */
    public static final String GROUP_LABEL_COMPATIBILITY_FIELD = "grouplabel";

    @Inject
    private UserManager userManager;

    public NuxeoGroupJsonWriter() {
        super(ENTITY_TYPE, NuxeoGroup.class);
    }

    @Override
    protected void writeEntityBody(NuxeoGroup group, JsonGenerator jg) throws IOException {
        // for backward compatibility, those are now in the 'properties' field
        jg.writeStringField(GROUP_NAME_COMPATIBILITY_FIELD, group.getName());
        jg.writeStringField(GROUP_LABEL_COMPATIBILITY_FIELD, group.getLabel());

        jg.writeStringField("id", group.getName());
        writeProperties(group, jg);
        writeMemberUsers(group, jg);
        writeMemberGroups(group, jg);
        writeParentGroups(group, jg);
    }

    protected void writeProperties(NuxeoGroup group, JsonGenerator jg) throws IOException {
        DocumentModel doc = group.getModel();
        if (doc == null) {
            return;
        }
        String groupSchema = userManager.getGroupSchemaName();
        Collection<Property> properties = doc.getPropertyObjects(groupSchema);
        if (properties.isEmpty()) {
            return;
        }

        List<String> excludedProperties = Arrays.asList(userManager.getGroupMembersField(),
                userManager.getGroupSubGroupsField(), userManager.getGroupParentGroupsField());
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
        jg.writeObjectFieldStart("properties");
        for (Property property : properties) {
            String localName = property.getField().getName().getLocalName();
            if (!excludedProperties.contains(localName)) {
                jg.writeFieldName(localName);
                OutputStream out = new OutputStreamWithJsonWriter(jg);
                propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE, out);
            }
        }
        jg.writeEndObject();
    }

    protected void writeMemberUsers(NuxeoGroup group, JsonGenerator jg) throws IOException {
        if (ctx.getFetched(ENTITY_TYPE).contains(MEMBER_USERS_FETCH_PROPERTY)) {
            jg.writeArrayFieldStart(MEMBER_USERS_FETCH_PROPERTY);
            for (String user : group.getMemberUsers()) {
                jg.writeString(user);
            }
            jg.writeEndArray();
        }
    }

    protected void writeMemberGroups(NuxeoGroup group, JsonGenerator jg) throws IOException {
        if (ctx.getFetched(ENTITY_TYPE).contains(MEMBER_GROUPS_FETCH_PROPERTY)) {
            jg.writeArrayFieldStart(MEMBER_GROUPS_FETCH_PROPERTY);
            for (String user : group.getMemberGroups()) {
                jg.writeString(user);
            }
            jg.writeEndArray();
        }
    }

    protected void writeParentGroups(NuxeoGroup group, JsonGenerator jg) throws IOException {
        if (ctx.getFetched(ENTITY_TYPE).contains(PARENT_GROUPS_FETCH_PROPERTY)) {
            jg.writeArrayFieldStart(PARENT_GROUPS_FETCH_PROPERTY);
            for (String user : group.getParentGroups()) {
                jg.writeString(user);
            }
            jg.writeEndArray();
        }
    }

}
