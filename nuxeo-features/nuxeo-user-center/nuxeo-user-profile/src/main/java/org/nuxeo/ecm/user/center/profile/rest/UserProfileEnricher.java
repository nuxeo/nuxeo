/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.user.center.profile.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter.ENTITY_TYPE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_AVATAR_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_BIRTHDATE_FIELD;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_FACET;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_PHONENUMBER_FIELD;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.GregorianCalendar;

import javax.inject.Inject;

import org.apache.commons.lang3.time.FastDateFormat;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.marshallers.json.OutputStreamWithJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 8.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class UserProfileEnricher extends AbstractJsonEnricher<NuxeoPrincipal> {

    public static final String NAME = "userprofile";

    private static final FastDateFormat FORMATTER = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * @since 9.3
     */
    public static final String COMPATIBILITY_CONFIGURATION_PARAM = "nuxeo.userprofile.enricher.compatibility";

    @Inject
    protected SchemaManager schemaManager;

    @Inject
    protected UserProfileService userProfileService;

    @Inject
    protected ConfigurationService configurationService;

    @Inject
    protected DownloadService downloadService;

    public UserProfileEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, NuxeoPrincipal nuxeoPrincipal) throws IOException {
        try (RenderingContext.SessionWrapper sw = ctx.getSession(null)) {
            DocumentModel up = userProfileService.getUserProfileDocument(nuxeoPrincipal.getName(), sw.getSession());
            jg.writeFieldName(NAME);
            if (up == null) {
                jg.writeNull();
                return;
            }

            jg.writeStartObject();
            if (configurationService.isBooleanPropertyTrue(COMPATIBILITY_CONFIGURATION_PARAM)) {
                writeCompatibilityUserProfile(jg, up);
            } else {
                writeUserProfile(jg, up);
            }
            jg.writeEndObject();
        } catch (DocumentSecurityException e) {
            // we lacked permission to existing docs (collision), ignore
        }
    }

    protected void writeCompatibilityUserProfile(JsonGenerator jg, DocumentModel up) throws IOException {
        Serializable propertyValue = up.getPropertyValue(USER_PROFILE_BIRTHDATE_FIELD);
        jg.writeStringField("birthdate",
                propertyValue == null ? null : FORMATTER.format(((GregorianCalendar) propertyValue).getTime()));
        jg.writeStringField("phonenumber", (String) up.getPropertyValue(USER_PROFILE_PHONENUMBER_FIELD));
        Blob avatar = (Blob) up.getPropertyValue(USER_PROFILE_AVATAR_FIELD);
        if (avatar != null) {
            String url = downloadService.getDownloadUrl(up, USER_PROFILE_AVATAR_FIELD, avatar.getFilename());
            jg.writeStringField("avatar", ctx.getBaseUrl() + url);
        } else {
            jg.writeNullField("avatar");
        }
    }

    protected void writeUserProfile(JsonGenerator jg, DocumentModel up) throws IOException {
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, APPLICATION_JSON_TYPE);
        // provides the user profile document to the property marshaller
        try (Closeable resource = ctx.wrap().with(ENTITY_TYPE, up).open()) {
            for (Schema schema : schemaManager.getFacet(USER_PROFILE_FACET).getSchemas()) {
                for (Field field : schema.getFields()) {
                    jg.writeFieldName(field.getName().getLocalName());
                    Property property = up.getProperty(field.getName().getPrefixedName());
                    OutputStream out = new OutputStreamWithJsonWriter(jg);
                    propertyWriter.write(property, Property.class, Property.class, APPLICATION_JSON_TYPE, out);
                }
            }
        }
    }
}
