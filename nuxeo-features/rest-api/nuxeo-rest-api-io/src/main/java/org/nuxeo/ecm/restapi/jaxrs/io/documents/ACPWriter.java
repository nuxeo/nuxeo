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
package org.nuxeo.ecm.restapi.jaxrs.io.documents;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.io.marshallers.json.document.ACPJsonWriter;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

/**
 * Json writer for ACP.
 *
 * @since 5.7.3
 * @deprecated since 7.10 The Nuxeo JSON marshalling was migrated to nuxeo-core-io. This class is replaced by
 *             {@link ACPJsonWriter} which is registered by default and available to marshal {@link ACP} from the Nuxeo
 *             Rest API thanks to the JAX-RS marshaller {@link JsonCoreIODelegate}.
 */
@Deprecated
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class ACPWriter extends EntityWriter<ACP> {

    public static final String ENTITY_TYPE = "acls";

    @Override
    protected void writeEntityBody(JsonGenerator jg, ACP item) throws IOException {

        jg.writeArrayFieldStart("acl");
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
                jg.writeStringField("begin",
                        ace.getBegin() != null ? DateParser.formatW3CDateTime(ace.getBegin().getTime()) : null);
                jg.writeStringField("end", ace.getEnd() != null ? DateParser.formatW3CDateTime(ace.getEnd().getTime())
                        : null);
                jg.writeStringField("status", ace.getStatus().toString().toLowerCase());
                jg.writeEndObject();
            }

            jg.writeEndArray();
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }
}
