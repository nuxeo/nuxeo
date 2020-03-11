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
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter.GROUP_LABEL_COMPATIBILITY_FIELD;
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter.GROUP_NAME_COMPATIBILITY_FIELD;
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter.MEMBER_GROUPS_FETCH_PROPERTY;
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter.MEMBER_USERS_FETCH_PROPERTY;
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter.PARENT_GROUPS_FETCH_PROPERTY;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.usermanager.GroupConfig;
import org.nuxeo.ecm.platform.usermanager.NuxeoGroupImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Convert Json as {@link NuxeoGroup}.
 * <p>
 * Format is (any additional json property is ignored):
 *
 * <pre>
 * {
 *   "entity-type":"group",
 *   "groupname": "GROUP_NAME", <- deprecated, for backward compatibility
 *   "grouplabel": "GROUP_DISPLAY_NAME", <- deprecated, for backward compatibility
 *   "id": "GROUP_NAME",
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
        GroupConfig groupConfig = userManager.getGroupConfig();
        String id = getStringField(jn, "id");
        String groupName = getStringField(jn, GROUP_NAME_COMPATIBILITY_FIELD);
        if (StringUtils.isBlank(id) || (StringUtils.isNotBlank(groupName) && !id.equals(groupName))) {
            // backward compatibility if `id` not found or if `groupname` is different
            id = groupName;
        }

        DocumentModel groupModel = null;
        if (StringUtils.isNotBlank(id)) {
            groupModel = userManager.getGroupModel(id);
        }
        if (groupModel == null) {
            groupModel = userManager.getBareGroupModel();
            groupModel.setProperty(groupConfig.schemaName, groupConfig.idField, id);
        }

        String beforeReadLabel = (String) groupModel.getProperty(groupConfig.schemaName, groupConfig.labelField);

        readProperties(groupModel, groupConfig, jn);
        readMemberUsers(groupModel, groupConfig, jn);
        readMemberGroups(groupModel, groupConfig, jn);
        readParentGroups(groupModel, groupConfig, jn);

        // override the `groupname` that may have been in the `properties` object
        if (StringUtils.isNotBlank(id)) {
            groupModel.setProperty(groupConfig.schemaName, groupConfig.idField, id);
        }

        // override with the compatibility `grouplabel` if needed
        if (jn.has(GROUP_LABEL_COMPATIBILITY_FIELD)) {
            String compatLabel = getStringField(jn, GROUP_LABEL_COMPATIBILITY_FIELD);
            String label = (String) groupModel.getProperty(groupConfig.schemaName, groupConfig.labelField);
            if (!Objects.equals(label, compatLabel)
                    && (beforeReadLabel == null || Objects.equals(beforeReadLabel, label))) {
                groupModel.setProperty(groupConfig.schemaName, groupConfig.labelField, compatLabel);
            }
        }

        return new NuxeoGroupImpl(groupModel, groupConfig);
    }

    protected void readProperties(DocumentModel groupModel, GroupConfig groupConfig, JsonNode jn) throws IOException {
        List<String> excludedProperties = Arrays.asList(groupConfig.membersField, groupConfig.subGroupsField,
                groupConfig.parentGroupsField);
        JsonNode propsNode = jn.get("properties");
        if (propsNode != null && !propsNode.isNull() && propsNode.isObject()) {
            ParameterizedType genericType = TypeUtils.parameterize(List.class, Property.class);
            try (Closeable resource = ctx.wrap().with(DEFAULT_SCHEMA_NAME, groupConfig.schemaName).open()) {
                List<Property> properties = readEntity(List.class, genericType, propsNode);
                properties.stream().filter(p -> !excludedProperties.contains(p.getName())).forEach(
                        p -> groupModel.setPropertyValue(p.getName(), p.getValue()));
            }
        }
    }

    protected void readMemberUsers(DocumentModel groupModel, GroupConfig groupConfig, JsonNode jn) {
        if (jn.has(MEMBER_USERS_FETCH_PROPERTY)) {
            List<String> users = getArrayStringValues(jn.get(MEMBER_USERS_FETCH_PROPERTY));
            groupModel.setProperty(groupConfig.schemaName, groupConfig.membersField, users);
        }
    }

    protected void readMemberGroups(DocumentModel groupModel, GroupConfig groupConfig, JsonNode jn) {
        if (jn.has(MEMBER_GROUPS_FETCH_PROPERTY)) {
            List<String> groups = getArrayStringValues(jn.get(MEMBER_GROUPS_FETCH_PROPERTY));
            groupModel.setProperty(groupConfig.schemaName, groupConfig.subGroupsField, groups);
        }
    }

    protected void readParentGroups(DocumentModel groupModel, GroupConfig groupConfig, JsonNode jn) {
        if (jn.has(PARENT_GROUPS_FETCH_PROPERTY)) {
            List<String> parents = getArrayStringValues(jn.get(PARENT_GROUPS_FETCH_PROPERTY));
            groupModel.setProperty(groupConfig.schemaName, groupConfig.parentGroupsField, parents);
        }
    }

    private List<String> getArrayStringValues(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && !node.isNull() && node.isArray()) {
            node.elements().forEachRemaining(n -> {
                if (n != null && !n.isNull() && n.isTextual()) {
                    values.add(n.textValue());
                }
            });
        }
        return values;
    }

}
