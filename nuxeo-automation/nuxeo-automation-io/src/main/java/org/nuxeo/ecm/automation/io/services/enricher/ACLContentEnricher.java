/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;

import java.io.IOException;

/**
 * This enricher adds a document ACLs
 *
 * @since 5.9.5
 */
public class ACLContentEnricher extends AbstractContentEnricher {

    public static final String ACLS_CONTENT_ID= "acls";

    @Override
    public void enrich(JsonGenerator jg, RestEvaluationContext ec)
            throws ClientException, IOException {
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
