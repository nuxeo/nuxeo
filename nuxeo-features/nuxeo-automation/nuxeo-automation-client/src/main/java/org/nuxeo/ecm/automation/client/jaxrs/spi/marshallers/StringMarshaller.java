/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import java.io.IOException;

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Marshaller for the default ObjectCodec for the java String class instances.
 *
 * @author ogrisel
 * @since 5.7
 */
public class StringMarshaller implements JsonMarshaller<String> {

    @Override
    public String getType() {
        return "string";
    }

    @Override
    public Class<String> getJavaType() {
        return String.class;
    }

    @Override
    public String read(JsonParser jp) throws IOException {
        jp.nextToken();
        jp.nextToken();
        return jp.getText();
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws IOException {
        // wrap as a complex object to pass through the string input micro
        // parsing used for backward compatibility with OperationInput.
        jg.writeStartObject();
        jg.writeStringField("entity-type", getType());
        jg.writeStringField("value", value.toString());
        jg.writeEndObject();
    }

}
