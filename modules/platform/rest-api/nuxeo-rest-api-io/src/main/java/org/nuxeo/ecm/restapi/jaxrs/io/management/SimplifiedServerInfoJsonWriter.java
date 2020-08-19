/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour Al Kotob
 */

package org.nuxeo.ecm.restapi.jaxrs.io.management;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.admin.runtime.SimplifiedBundleInfo;
import org.nuxeo.ecm.admin.runtime.SimplifiedServerInfo;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 11.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class SimplifiedServerInfoJsonWriter extends ExtensibleEntityJsonWriter<SimplifiedServerInfo> {

    public static final String ENTITY_TYPE = "serverInfo";

    public SimplifiedServerInfoJsonWriter() {
        super(ENTITY_TYPE, SimplifiedServerInfo.class);
    }

    @Override
    protected void writeEntityBody(SimplifiedServerInfo entity, JsonGenerator jg) throws IOException {
        jg.writeStringField("applicationName", entity.getApplicationName());
        jg.writeStringField("applicationVersion", entity.getApplicationVersion());
        jg.writeStringField("distributionName", entity.getDistributionName());
        jg.writeStringField("distributionVersion", entity.getDistributionVersion());
        jg.writeStringField("distributionDate", entity.getDistributionDate());

        jg.writeArrayFieldStart("bundles");
        for (SimplifiedBundleInfo b : entity.getBundleInfos()) {
            jg.writeStartObject();
            jg.writeStringField("name", b.getName());
            jg.writeStringField("version", b.getVersion());
            jg.writeEndObject();
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("warnings");
        for (String w : entity.getWarnings()) {
            jg.writeStartObject();
            jg.writeStringField("message", w);
            jg.writeEndObject();
        }
        jg.writeEndArray();

        jg.writeArrayFieldStart("errors");
        for (String w : entity.getErrors()) {
            jg.writeStartObject();
            jg.writeStringField("message", w);
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

}
