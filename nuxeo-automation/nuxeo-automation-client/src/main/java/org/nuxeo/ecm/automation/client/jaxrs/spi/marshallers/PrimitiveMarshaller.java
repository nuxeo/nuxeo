/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

/**
 * @author matic
 *
 */
public class PrimitiveMarshaller<T> implements JsonMarshaller<T> {

    protected final Class<T> clazz;

    protected final String type;

    public PrimitiveMarshaller(Class<T> clazz) {
        this.clazz = clazz;
        this.type = clazz.getSimpleName().toLowerCase();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Class<T> getJavaType() {
        return clazz;
    }

    @Override
    public String getReference(T value) {
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T read(JsonParser jp) throws Exception {
        jp.nextToken();
        JsonToken token = jp.nextToken(); // skip key
        if (token == JsonToken.VALUE_STRING) {
            return (T)jp.getText();
        } else if (token == JsonToken.VALUE_FALSE || token == JsonToken.VALUE_TRUE) {
            return (T)Boolean.valueOf(jp.getBooleanValue());
        } else if (token == JsonToken.VALUE_NUMBER_INT) {
            if (clazz == Long.class) {
                return (T)new Long(jp.getLongValue());
            } else {
                return (T)new Integer(jp.getIntValue());
            }
        } else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
            if (clazz == Double.class) {
                return (T)new Double(jp.getDoubleValue());
            } else {
                return (T)new Float(jp.getFloatValue());
            }
        }
        return null;
    }

    @Override
    public void write(JsonGenerator jg, T value) throws Exception {
        if (value == null) {
            jg.writeNullField(type);
        } else if (clazz == String.class) {
            jg.writeStringField(type, (String)value);
        } else if (clazz == Boolean.class) {
            jg.writeBooleanField(type, (Boolean)value);
        } else if (clazz == Integer.class) {
            jg.writeNumberField(type, (Integer)value);
        } else if (clazz == Long.class) {
            jg.writeNumberField(type, (Long)value);
        } else if (clazz == Double.class) {
            jg.writeNumberField(type, (Double)value);
        } else if (clazz == Float.class) {
            jg.writeNumberField(type, (Float)value);
        } else {
            throw new IllegalArgumentException("Unknown primitive type: "+clazz);
        }
    }

}
