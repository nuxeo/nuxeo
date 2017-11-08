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

import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertiesJsonReader.DEFAULT_SCHEMA_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter.ENTITY_TYPE;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * Convert Json as {@link NuxeoGroup}.
 * <p>
 * Format is (any additional json property is ignored):
 *
 * <pre>
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
 * }
 * </pre>
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoGroupJsonReader extends EntityJsonReader<NuxeoGroup> {

    @Inject
    private UserManager userManager;

    public NuxeoGroupJsonReader() {
        super(ENTITY_TYPE);
    }

    @Override
    protected NuxeoGroup readEntity(JsonNode jn) throws IOException {
        NuxeoGroup group = null;
        String id = getStringField(jn, "groupname");
        if (id != null) {
            group = userManager.getGroup(id);
        }
        if (group == null) {
            group = new NuxeoGroupImpl(id);
        }
        String label = getStringField(jn, "grouplabel");
        group.setLabel(label);
        List<String> users = getArrayStringValues(jn.get("memberUsers"));
        group.setMemberUsers(users);
        List<String> groups = getArrayStringValues(jn.get("memberGroups"));
        group.setMemberGroups(groups);

        List<String> parentGroups = getArrayStringValues(jn.get("parentGroups"));
        group.setParentGroups(parentGroups);

        readProperties(group, jn);

        return group;
    }

    protected void readProperties(NuxeoGroup group, JsonNode jn) throws IOException {
        List<String> excludedProperties = Arrays.asList(userManager.getGroupIdField(), userManager.getGroupLabelField(),
                userManager.getGroupMembersField(), userManager.getGroupSubGroupsField(),
                userManager.getGroupParentGroupsField());
        JsonNode propsNode = jn.get("properties");
        if (propsNode != null && !propsNode.isNull() && propsNode.isObject()) {
            String groupSchemaName = userManager.getGroupSchemaName();
            DocumentModel model = new SimpleDocumentModel(groupSchemaName);
            ParameterizedType genericType = TypeUtils.parameterize(List.class, Property.class);
            try (Closeable resource = ctx.wrap().with(DEFAULT_SCHEMA_NAME, groupSchemaName).open()) {
                List<Property> properties = readEntity(List.class, genericType, propsNode);
                properties.stream().filter(p -> !excludedProperties.contains(p)).forEach(
                        p -> model.setPropertyValue(p.getName(), p.getValue()));
            }
            group.setModel(model);
        }
    }

    private List<String> getArrayStringValues(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && !node.isNull() && node.isArray()) {
            node.getElements().forEachRemaining(n -> {
                if (n != null && !n.isNull() && n.isTextual()) {
                    values.add(n.getTextValue());
                }
            });
        }
        return values;
    }

}
