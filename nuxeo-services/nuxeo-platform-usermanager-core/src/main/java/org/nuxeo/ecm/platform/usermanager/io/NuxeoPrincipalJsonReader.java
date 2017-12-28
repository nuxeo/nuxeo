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
import static org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalJsonWriter.ENTITY_TYPE;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertiesJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Convert Json as {@link NuxeoPrincipal}.
 * <p>
 * Format is (any additional json property is ignored):
 *
 * <pre>
 * {
 *   "entity-type":"user",
 *   "id":"USERNAME",
 *   "properties":{   <- depending on the user schema / format is managed by {@link DocumentPropertiesJsonReader}
 *     "firstName":"FIRSTNAME",
 *     "lastName":"LASTNAME",
 *     "username":"USERNAME",
 *     "email":"user@mail.com",
 *     "company":"COMPANY",
 *     "password":"", <- ALWAYS EMPTY
 *     "groups":[
 *       "GROUP1 NAME OF THE USER",
 *       "GROUP2 NAME OF THE USER",
 *       ...
 *     ]
 *   }
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NuxeoPrincipalJsonReader extends EntityJsonReader<NuxeoPrincipal> {

    @Inject
    private UserManager userManager;

    public NuxeoPrincipalJsonReader() {
        super(ENTITY_TYPE);
    }

    @Override
    protected NuxeoPrincipal readEntity(JsonNode jn) throws IOException {
        String id = getStringField(jn, "id");
        DocumentModel userDoc = null;
        if (id != null) {
            NuxeoPrincipal principal = userManager.getPrincipal(id);
            if (principal != null) {
                userDoc = principal.getModel();
            }
        }
        if (userDoc == null) {
            userDoc = userManager.getBareUserModel();
        }
        JsonNode propsNode = jn.get("properties");
        if (propsNode != null && !propsNode.isNull() && propsNode.isObject()) {
            ParameterizedType genericType = TypeUtils.parameterize(List.class, Property.class);
            String schema = userManager.getUserSchemaName();
            try (Closeable resource = ctx.wrap().with(DEFAULT_SCHEMA_NAME, schema).open()) {
                List<Property> properties = readEntity(List.class, genericType, propsNode);
                for (Property property : properties) {
                    userDoc.setPropertyValue(property.getName(), property.getValue());
                }
            }
        }
        NuxeoPrincipal principal = new NuxeoPrincipalImpl(id);
        principal.setModel(userDoc);
        return principal;
    }

}
