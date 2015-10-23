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
package org.nuxeo.ecm.restapi.jaxrs.io.usermanager;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupJsonWriter;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

/**
 * @since 5.7.3
 * @deprecated since 7.10 The Nuxeo JSON marshalling was migrated to nuxeo-core-io. This class is replaced by
 *             {@link NuxeoGroupJsonWriter} which is registered by default and available to marshal {@link NuxeoGroup}
 *             from the Nuxeo Rest API thanks to the JAX-RS marshaller {@link JsonCoreIODelegate}
 */
@Deprecated
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class NuxeoGroupWriter extends EntityWriter<NuxeoGroup> {

    /**
     *
     */
    public static final String ENTITY_TYPE = "group";

    /**
     * @param createGenerator
     * @param group
     * @return
     * @throws IOException
     * @throws JsonGenerationException
     */
    @Override
    public void writeEntityBody(JsonGenerator jg, NuxeoGroup group) throws JsonGenerationException,
            IOException {
        jg.writeStringField("groupname", group.getName());

        jg.writeStringField("grouplabel", group.getLabel());

        jg.writeArrayFieldStart("memberUsers");
        for (String user : group.getMemberUsers()) {
            jg.writeString(user);
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("memberGroups");
        for (String user : group.getMemberGroups()) {
            jg.writeString(user);
        }
        jg.writeEndArray();

    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

}
