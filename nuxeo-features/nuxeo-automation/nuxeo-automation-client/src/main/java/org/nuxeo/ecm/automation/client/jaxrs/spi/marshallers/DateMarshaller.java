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
import java.util.Date;

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.model.DateParser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Marshaller for the default ObjectCodec for the java Date class instances.
 *
 * @author ogrisel
 * @since 5.7
 */
public class DateMarshaller implements JsonMarshaller<Date> {

    @Override
    public String getType() {
        return "date";
    }

    @Override
    public Class<Date> getJavaType() {
        return Date.class;
    }

    @Override
    public Date read(JsonParser jp) throws IOException {
        jp.nextToken();
        jp.nextToken();
        return DateParser.parseW3CDateTime(jp.getText());
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", getType());
        jg.writeStringField("value", DateParser.formatW3CDateTime((Date) value));
        jg.writeEndObject();
    }

}
