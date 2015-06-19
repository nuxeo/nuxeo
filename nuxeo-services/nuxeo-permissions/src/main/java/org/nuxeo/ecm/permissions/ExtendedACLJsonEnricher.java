/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * Enrich {@link DocumentModel} Json.
 * <p>
 * Add {@link DocumentModel}'s ACP as json attachment with notifications info for each ACE (such as whether a
 * notification should be send and the notification comment).
 * </p>
 * <p>
 * Enable if parameter enrichers.document=extendedAcls is present.
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
 *         "name":" inherited",
 *         "ace": [
 *           {
 *             "username": "administrators",
 *             "permission": "Everything",
 *             "granted": true,
 *             "notify": false,
 *             "comment": ""
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
 *
 * @since 7.4
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class ExtendedACLJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "extendedAcls";

    public ExtendedACLJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        ACP item = document.getACP();
        jg.writeArrayFieldStart("acls");
        for (ACL acl : item.getACLs()) {
            jg.writeStartObject();
            jg.writeStringField("name", acl.getName());
            jg.writeArrayFieldStart("ace");
            for (ACE ace : acl.getACEs()) {
                jg.writeStartObject();
                jg.writeStringField("id", ace.getId());
                jg.writeStringField("username", ace.getUsername());
                jg.writeStringField("permission", ace.getPermission());
                jg.writeBooleanField("granted", ace.isGranted());
                jg.writeStringField("creator", ace.getCreator());
                DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();
                jg.writeStringField("begin",
                        ace.getBegin() != null ? dateTimeFormatter.print(new DateTime(ace.getBegin())) : null);
                jg.writeStringField("end", ace.getEnd() != null ? dateTimeFormatter.print(new DateTime(ace.getEnd()))
                        : null);
                Map<String, Serializable> m = computeAdditionalFields(document, acl.getName(), ace.getId());
                for (Map.Entry<String, Serializable> entry : m.entrySet()) {
                    jg.writeObjectField(entry.getKey(), entry.getValue());
                }
                jg.writeEndObject();
            }
            jg.writeEndArray();
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    protected Map<String, Serializable> computeAdditionalFields(DocumentModel doc, String aclName, String aceId) {
        Map<String, Serializable> m = new HashMap<>();

        DirectoryService directoryService = Framework.getLocalService(DirectoryService.class);
        Session session = null;
        try {
            session = directoryService.open(ACE_INFO_DIRECTORY);
            String id = computeDirectoryId(doc, aclName, aceId);
            DocumentModel entry = session.getEntry(id);
            if (entry != null) {
                m.put("notify", entry.getPropertyValue("aceinfo:notify"));
                m.put("comment", entry.getPropertyValue("aceinfo:comment"));
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return m;
    }

    protected String computeDirectoryId(DocumentModel doc, String aclName, String aceId) {
        return String.format("%s:%s:%s:%s", doc.getId(), doc.getRepositoryName(), aclName, aceId);
    }

}
