/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.usermanager;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Serialization for a Nuxeo principal.
 *
 * @since 5.7.3
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class NuxeoPrincipalWriter implements MessageBodyWriter<NuxeoPrincipal> {

    @Context
    JsonFactory factory;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {

        return NuxeoPrincipal.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(NuxeoPrincipal t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(NuxeoPrincipal principal, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {

        try {
            writePrincipal(factory.createJsonGenerator(entityStream, JsonEncoding.UTF8), principal);
        } catch (ClientException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * @param createGenerator
     * @throws IOException
     * @throws JsonGenerationException
     * @throws ClientException
     *
     * @since 5.7.3
     */
    public static void writePrincipal(JsonGenerator jg, NuxeoPrincipal principal)
            throws JsonGenerationException, IOException, ClientException {

        jg.writeStartObject();
        jg.writeStringField("entity-type", "user");
        jg.writeStringField("id", principal.getName());

        writeProperties(jg, principal.getModel());
        writeExtendedGroups(jg, principal.getAllGroups());

        jg.writeBooleanField("isAdministrator", principal.isAdministrator());
        jg.writeBooleanField("isAnonymous", principal.isAnonymous());

        jg.writeEndObject();

        jg.flush();

    }

    /**
     * @param jg
     * @param model
     * @throws IOException
     * @throws JsonGenerationException
     * @throws ClientException
     * @since 5.7.3
     */
    static private void writeProperties(JsonGenerator jg, DocumentModel doc)
            throws JsonGenerationException, IOException, ClientException {
        UserManager um = Framework.getLocalService(UserManager.class);

        jg.writeFieldName("properties");
        jg.writeStartObject();

        DocumentPart part = doc.getPart(um.getUserSchemaName());
        if (part == null) {
            return;
        }

        for (Property p : part.getChildren()) {
            jg.writeFieldName(p.getField().getName().getLocalName());

            writePropertyValue(jg, p);
        }
        jg.writeEndObject();

    }

    /**
     * Converts the value of the given core property to JSON format. The given
     * filesBaseUrl is the baseUrl that can be used to locate blob content and
     * is useful to generate blob urls.
     *
     * @throws IOException
     * @throws PropertyException
     * @throws JsonGenerationException
     */
    protected static void writePropertyValue(JsonGenerator jg, Property prop)
            throws JsonGenerationException, PropertyException, IOException {
        if (prop.isScalar()) {
            writeScalarPropertyValue(jg, prop);
        } else if (prop.isList()) {
            writeListPropertyValue(jg, prop);
        }
    }

    protected static void writeScalarPropertyValue(JsonGenerator jg,
            Property prop) throws JsonGenerationException, IOException,
            PropertyException {
        org.nuxeo.ecm.core.schema.types.Type type = prop.getType();
        Object v = prop.getValue();
        if (v == null) {
            jg.writeNull();
        } else {
            jg.writeString(type.encode(v));
        }
    }

    protected static void writeListPropertyValue(JsonGenerator jg, Property prop)
            throws JsonGenerationException, PropertyException, IOException {
        jg.writeStartArray();
        if (prop instanceof ArrayProperty) {
            Object[] ar = (Object[]) prop.getValue();
            if (ar == null) {
                return;
            }
            org.nuxeo.ecm.core.schema.types.Type type = ((ListType) prop.getType()).getFieldType();
            for (Object o : ar) {
                jg.writeString(type.encode(o));
            }
        } else {
            ListProperty listp = (ListProperty) prop;
            for (Property p : listp.getChildren()) {
                writePropertyValue(jg, p);
            }
        }
        jg.writeEndArray();
    }

    /**
     * This part adds all groupe that the user belongs to directly or indirectly
     * and adds the label in the result.
     *
     * @param jg
     * @param allGroups
     * @throws IOException
     * @throws JsonGenerationException
     * @throws ClientException
     *
     * @since 5.7.3
     */
    static private void writeExtendedGroups(JsonGenerator jg, List<String> allGroups)
            throws JsonGenerationException, IOException, ClientException {
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

}
