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

import org.nuxeo.ecm.automation.client.annotations.EntityType;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Marshaller for the default ObjectCodec for the java Boolean class instances. Returned into entity-type primitive the
 * content of existing {@link EntityType} annotation.
 *
 * @author ogrisel
 * @since 5.7
 */
public class PojoMarshaller<T> implements JsonMarshaller<T> {

    final Class<T> type;

    protected String entityTypeName;

    public PojoMarshaller(Class<T> type) {
        this.type = type;
        this.entityTypeName = "";
        if (type.getAnnotation(EntityType.class) != null) {
            this.entityTypeName = type.getAnnotation(EntityType.class).value();
        }
    }

    public static <T> PojoMarshaller<T> forClass(Class<T> type) {
        return new PojoMarshaller<T>(type);
    }

    @Override
    public String getType() {
        return entityTypeName.isEmpty() ? type.getName() : entityTypeName;
    }

    @Override
    public Class<T> getJavaType() {
        return type;
    }

    @Override
    public T read(JsonParser jp) throws IOException {
        jp.nextToken();
        jp.nextToken();
        return jp.readValueAs(type);
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", getType());
        jg.writeObjectField("value", value);
        jg.writeEndObject();
    }

}
