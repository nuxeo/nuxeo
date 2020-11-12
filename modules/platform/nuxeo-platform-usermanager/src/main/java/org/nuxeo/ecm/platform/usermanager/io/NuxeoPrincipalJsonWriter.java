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
import java.util.Collection;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentPropertyJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Convert {@link NuxeoPrincipal} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing
 * {@link NuxeoPrincipal}.
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link NuxeoPrincipalJsonWriter#extend(Object, JsonGenerator)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"user",
 *   "id":"USERNAME",
 *   "properties":{   <- depending on the user schema / format is managed by {@link DocumentPropertyJsonWriter }
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
 *   },
 *   "extendedGroups":[
 *     {
 *       "name":"GROUP1NAME",
 *       "label":"GROUP1 DISPLAY NAME",
 *       "url":"GROUP1 URL"
 *     },
 *     ...
 *   ],
 *   "isAdministrator":true|false,
 *   "isAnonymous":false|false
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
public class NuxeoPrincipalJsonWriter extends ExtensibleEntityJsonWriter<NuxeoPrincipal> {

    public static final String ENTITY_TYPE = "user";

    @Inject
    private UserManager userManager;

    @Inject
    private DirectoryService directoryService;

    public NuxeoPrincipalJsonWriter() {
        super(ENTITY_TYPE, NuxeoPrincipal.class);
    }

    @Override
    protected void writeEntityBody(NuxeoPrincipal principal, JsonGenerator jg) throws IOException {
        jg.writeStringField("id", principal.getName());
        writeProperties(jg, principal);
        writeExtendedGroups(jg, principal);
        jg.writeBooleanField("isAdministrator", principal.isAdministrator());
        jg.writeBooleanField("isAnonymous", principal.isAnonymous());
    }

    private void writeProperties(JsonGenerator jg, NuxeoPrincipal principal) throws IOException {
        DocumentModel doc = principal.getModel();
        if (doc == null) {
            return;
        }
        String userSchema = userManager.getUserSchemaName();
        Collection<Property> properties = doc.getPropertyObjects(userSchema);
        if (properties.isEmpty()) {
            return;
        }
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
        jg.writeObjectFieldStart("properties");
        for (Property property : properties) {
            String localName = property.getField().getName().getLocalName();
            if (!localName.equals(getPasswordField())) {
                jg.writeFieldName(localName);
                OutputStream out = new OutputStreamWithJsonWriter(jg);
                propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE, out);
            }
        }
        jg.writeEndObject();
    }

    private void writeExtendedGroups(JsonGenerator jg, NuxeoPrincipal principal) throws IOException {
        jg.writeArrayFieldStart("extendedGroups");
        for (String strGroup : principal.getAllGroups()) {
            NuxeoGroup group = userManager.getGroup(strGroup);
            String label = group == null ? strGroup : group.getLabel();
            jg.writeStartObject();
            jg.writeStringField("name", strGroup);
            jg.writeStringField("label", label);
            jg.writeStringField("url", "group/" + strGroup);
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    private String getPasswordField() {
        String userDirectoryName = userManager.getUserDirectoryName();
        return directoryService.getDirectory(userDirectoryName).getPasswordField();
    }

}
