/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;

/**
 * This enricher adds a document ACLs
 *
 * @since 5.9.5
 * @deprecated This enricher was migrated to {@link org.nuxeo.ecm.permissions.ACLJsonEnricher}. The content enricher
 *             service doesn't work anymore.
 */
@Deprecated
public class ACLContentEnricher extends AbstractContentEnricher {

    public static final String ACLS_CONTENT_ID = "acls";

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec) throws IOException {
        DocumentModel doc = ec.getDocumentModel();
        ACP item = doc.getACP();
        jg.writeStartArray();
        for (ACL acl : item.getACLs()) {
            jg.writeStartObject();
            jg.writeStringField("name", acl.getName());

            jg.writeArrayFieldStart("ace");

            for (ACE ace : acl.getACEs()) {
                jg.writeStartObject();
                jg.writeStringField("username", ace.getUsername());
                jg.writeStringField("permission", ace.getPermission());
                jg.writeBooleanField("granted", ace.isGranted());
                jg.writeEndObject();
            }

            jg.writeEndArray();
            jg.writeEndObject();
        }
        jg.writeEndArray();
        jg.flush();
    }

}
