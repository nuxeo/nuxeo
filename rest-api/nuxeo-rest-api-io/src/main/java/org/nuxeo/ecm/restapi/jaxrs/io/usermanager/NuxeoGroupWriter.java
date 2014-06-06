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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;

/**
 *
 *
 * @since 5.7.3
 */
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
     *
     */
    @Override
    public void writeEntityBody(JsonGenerator jg, NuxeoGroup group) throws ClientException, JsonGenerationException, IOException{
        jg.writeStringField("groupname", group.getName());

        jg.writeStringField("grouplabel", group.getLabel());

        jg.writeArrayFieldStart("memberUsers");
        for(String user : group.getMemberUsers()) {
            jg.writeString(user);
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("memberGroups");
        for(String user : group.getMemberGroups()) {
            jg.writeString(user);
        }
        jg.writeEndArray();



    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

}
