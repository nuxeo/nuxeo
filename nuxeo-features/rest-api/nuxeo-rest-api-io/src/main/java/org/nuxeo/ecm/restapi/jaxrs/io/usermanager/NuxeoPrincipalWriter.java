/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.jaxrs.io.usermanager;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalJsonWriter;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;
import org.nuxeo.runtime.api.Framework;

/**
 * Serialization for a Nuxeo principal.
 *
 * @since 5.7.3
 * @deprecated since 7.10 The Nuxeo JSON marshalling was migrated to nuxeo-core-io. This class is replaced by
 *             {@link NuxeoPrincipalJsonWriter} which is registered by default and available to marshal
 *             {@link NuxeoPrincipal} from the Nuxeo Rest API thanks to the JAX-RS marshaller {@link JsonCoreIODelegate}
 */
@Deprecated
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class NuxeoPrincipalWriter extends EntityWriter<NuxeoPrincipal> {

    /**
     *
     */
    public static final String ENTITY_TYPE = "user";

    @Context
    JsonFactory factory;

    /**
     * @param createGenerator
     * @throws IOException
     * @throws JsonGenerationException
     * @since 5.7.3
     */
    @Override
    public void writeEntityBody(JsonGenerator jg, NuxeoPrincipal principal) throws JsonGenerationException,
            IOException {

        jg.writeStringField("id", principal.getName());

        writeProperties(jg, principal.getModel());
        writeExtendedGroups(jg, principal.getAllGroups());

        jg.writeBooleanField("isAdministrator", principal.isAdministrator());
        jg.writeBooleanField("isAnonymous", principal.isAnonymous());

    }

    /**
     * @param jg
     * @param model
     * @throws IOException
     * @throws JsonGenerationException
     * @since 5.7.3
     */
    static private void writeProperties(JsonGenerator jg, DocumentModel doc) throws JsonGenerationException,
            IOException {
        UserManager um = Framework.getLocalService(UserManager.class);

        jg.writeFieldName("properties");
        jg.writeStartObject();

        DocumentPart part = doc.getPart(um.getUserSchemaName());
        if (part == null) {
            return;
        }

        for (Property p : part.getChildren()) {
            String fieldName = p.getField().getName().getLocalName();
            jg.writeFieldName(fieldName);

            if (!fieldName.equals(getPasswordField(um))) {
                JsonDocumentWriter.writePropertyValue(jg, p, "");
            } else {
                jg.writeString("");
            }
        }
        jg.writeEndObject();

    }

    /**
     * @param um
     * @return
     * @since 5.8
     */
    private static String getPasswordField(UserManager um) {
        String userDirectoryName = um.getUserDirectoryName();
        DirectoryService dirService = Framework.getLocalService(DirectoryService.class);
        return dirService.getDirectory(userDirectoryName).getPasswordField();
    }

    /**
     * This part adds all groupe that the user belongs to directly or indirectly and adds the label in the result.
     *
     * @param jg
     * @param allGroups
     * @throws IOException
     * @throws JsonGenerationException
     * @since 5.7.3
     */
    static private void writeExtendedGroups(JsonGenerator jg, List<String> allGroups) throws JsonGenerationException,
            IOException {
        UserManager um = Framework.getLocalService(UserManager.class);

        jg.writeArrayFieldStart("extendedGroups");
        for (String strGroup : allGroups) {
            NuxeoGroup group = um.getGroup(strGroup);
            String label = group == null ? strGroup : group.getLabel();
            jg.writeStartObject();
            jg.writeStringField("name", strGroup);
            jg.writeStringField("label", label);
            jg.writeStringField("url", "group/" + strGroup);
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

}
