/*
 * Copyright (c) 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

/**
 * Marshaller for the default ObjectCodec for the java Boolean class instances.
 *
 * @author ogrisel
 * @since 5.7
 */
public class PojoMarshaller<T> implements JsonMarshaller<T> {

    final Class<T> type;

    public PojoMarshaller(Class<T> type) {
       this.type  = type;
    }

    public static <T> PojoMarshaller<T> forClass(Class<T> type) {
        return new PojoMarshaller<T>(type);
    }

    @Override
    public String getType() {
        return type.getCanonicalName();
    }

    @Override
    public Class<T> getJavaType() {
        return type;
    }

    @Override
    public T read(JsonParser jp) throws Exception {
        jp.nextToken();
        jp.nextToken();
        return jp.readValueAs(type);
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws Exception {
        jg.writeStartObject();
        jg.writeStringField("entity-type", getType());
        jg.writeObjectField("value", value);
        jg.writeEndObject();
    }

}
