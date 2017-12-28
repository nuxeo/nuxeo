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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_COMMENT;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_NOTIFY;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.MaxDepthReachedException;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add {@link DocumentModel}'s ACP as json attachment.
 * </p>
 * <p>
 * Enable if parameter enrichers-document=acls is present.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"document",
 *   ...
 *   "contextParameters": {
 *     "acls": [
 *       {
 *         "name": "inherited",
 *         "aces" :[
 *           {
 *             "username": "administrators",
 *             "permission": "Everything",
 *             "granted": true,
 *             "creator": "Administrator",
 *             "begin": "2014-10-19T09:16:30.291Z",
 *             "end": "2016-10-19T09:16:30.291Z"
 *             "notify": true // optional
 *             "comment": "" // optional
 *           },
 *           ...
 *         ]
 *       },
 *       ...
 *     ]
 *   }
 * }
 * </pre>
 *
 * </p>
 * <p>
 * {@code username} and {@code creator} property can be fetched with fetch.acls=username or fetch.acls=creator.
 * </p>
 * <p>
 * Additional ACE fields (such as notify and notification comment) can be written by using fetch.acls=extended.
 * </p>
 *
 * @see org.nuxeo.ecm.platform.usermanager.io.NuxeoPrincipalJsonWriter
 * @see org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ACLJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "acls";

    public static final String USERNAME_PROPERTY = "username";

    public static final String CREATOR_PROPERTY = "creator";

    public static final String EXTENDED_ACLS_PROPERTY = "extended";

    public static final String COMPATIBILITY_CONFIGURATION_PARAM = "nuxeo.permissions.acl.enricher.compatibility";

    public ACLJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        ACP item = document.getACP();
        jg.writeArrayFieldStart(NAME);
        for (ACL acl : item.getACLs()) {
            jg.writeStartObject();
            jg.writeStringField("name", acl.getName());
            writeACEsField(jg, "aces", acl, document);

            ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
            if (configurationService.isBooleanPropertyTrue(COMPATIBILITY_CONFIGURATION_PARAM)) {
                writeACEsField(jg, "ace", acl, document);
            }
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    protected void writeACEsField(JsonGenerator jg, String fieldName, ACL acl, DocumentModel document)
            throws IOException {
        jg.writeArrayFieldStart(fieldName);
        for (ACE ace : acl.getACEs()) {
            jg.writeStartObject();
            jg.writeStringField("id", ace.getId());
            String username = ace.getUsername();
            writePrincipalOrGroup(USERNAME_PROPERTY, username, jg);
            jg.writeBooleanField("externalUser", NuxeoPrincipal.isTransientUsername(username));
            jg.writeStringField("permission", ace.getPermission());
            jg.writeBooleanField("granted", ace.isGranted());
            writePrincipalOrGroup(CREATOR_PROPERTY, ace.getCreator(), jg);
            DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();
            jg.writeStringField("begin",
                    ace.getBegin() != null ? dateTimeFormatter.print(new DateTime(ace.getBegin())) : null);
            jg.writeStringField("end",
                    ace.getEnd() != null ? dateTimeFormatter.print(new DateTime(ace.getEnd())) : null);
            jg.writeStringField("status", ace.getStatus().toString().toLowerCase());

            if (ctx.getFetched(NAME).contains(EXTENDED_ACLS_PROPERTY)) {
                Map<String, Serializable> m = computeAdditionalFields(document, acl.getName(), ace.getId());
                for (Map.Entry<String, Serializable> entry : m.entrySet()) {
                    jg.writeObjectField(entry.getKey(), entry.getValue());
                }
            }
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    protected void writePrincipalOrGroup(String propertyName, String value, JsonGenerator jg) throws IOException {
        if (value != null && ctx.getFetched(NAME).contains(propertyName)) {
            try (Closeable resource = ctx.wrap().controlDepth().open()) {
                UserManager userManager = Framework.getService(UserManager.class);
                Object entity = userManager.getPrincipal(value);
                if (entity == null) {
                    entity = userManager.getGroup(value);
                }

                if (entity != null) {
                    writeEntityField(propertyName, entity, jg);
                    return;
                }
            } catch (MaxDepthReachedException e) {
                // do nothing
            }
        }
        jg.writeStringField(propertyName, value);
    }

    protected Map<String, Serializable> computeAdditionalFields(DocumentModel doc, String aclName, String aceId) {
        Map<String, Serializable> m = new HashMap<>();

        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session session = directoryService.open(ACE_INFO_DIRECTORY)) {
                String id = computeDirectoryId(doc, aclName, aceId);
                DocumentModel entry = session.getEntry(id);
                if (entry != null) {
                    m.put("notify", entry.getPropertyValue(ACE_INFO_NOTIFY));
                    m.put("comment", entry.getPropertyValue(ACE_INFO_COMMENT));
                }
            }
        });

        return m;
    }

    protected String computeDirectoryId(DocumentModel doc, String aclName, String aceId) {
        return String.format("%s:%s:%s:%s", doc.getId(), doc.getRepositoryName(), aclName, aceId);
    }

}
