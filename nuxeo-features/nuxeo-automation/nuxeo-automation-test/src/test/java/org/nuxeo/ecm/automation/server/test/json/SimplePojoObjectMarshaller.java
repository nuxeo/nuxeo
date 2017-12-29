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
 *     Nicolas Chapurlat
 */
package org.nuxeo.ecm.automation.server.test.json;

import java.io.IOException;

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.server.test.json.JSONOperationWithArrays.SimplePojo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public class SimplePojoObjectMarshaller implements JsonMarshaller<SimplePojo> {

    public SimplePojoObjectMarshaller() {
    }

    @Override
    public String getType() {
        return "simplePojo";
    }

    @Override
    public Class<SimplePojo> getJavaType() {
        return SimplePojo.class;
    }

    @Override
    public SimplePojo read(JsonParser jp) throws IOException {
        jp.nextValue();
        return jp.readValueAs(SimplePojo.class);
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "simplePojo");
        jg.writeObjectField("value", value);
        jg.writeEndObject();
    }

}