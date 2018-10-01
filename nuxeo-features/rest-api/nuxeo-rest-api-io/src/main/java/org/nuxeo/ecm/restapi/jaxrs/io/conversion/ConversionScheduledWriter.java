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

package org.nuxeo.ecm.restapi.jaxrs.io.conversion;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 7.4
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
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
