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

package org.nuxeo.ecm.restapi.jaxrs.io.conversion;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;

/**
 * @since 7.4
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class ConversionScheduledWriter extends EntityWriter<ConversionScheduled> {

    public static final String ENTITY_TYPE = "conversionScheduled";

    @Override
    protected void writeEntityBody(JsonGenerator jg, ConversionScheduled conversionScheduled) throws IOException {
        jg.writeStringField("conversionId", conversionScheduled.id);
        jg.writeStringField("pollingURL", conversionScheduled.pollingURL);
        jg.writeStringField("resultURL", conversionScheduled.resultURL);
    }

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }
}
