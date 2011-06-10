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

import java.io.StringWriter;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;

/**
 * @author matic
 *
 */
public class BeanMarshaller<T> implements JsonMarshaller<T> {

    protected final Class<T> clazz;

    protected final String type;


    public BeanMarshaller(Class<T> clazz) {
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
    public String getReference(T data) {
        try {
            StringWriter w = new StringWriter();
            JsonGenerator jg = JsonMarshalling.getFactory().createJsonGenerator(w);
            if (jg.getCodec() == null) {
                jg.setCodec(new ObjectMapper());
            }
            jg.writeObject(data);
            jg.close();
            return w.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build json descriptor for object: "+data);
        }
    }

    @Override
    public T read(JsonParser jp) throws Exception {
        if (jp.getCodec() == null) {
            jp.setCodec(new ObjectMapper());
        }
        jp.nextToken();
        jp.nextToken(); // skip key
        // now read object
        return jp.readValueAs(clazz);
    }

    @Override
    public void write(JsonGenerator jg, T value) throws Exception {
        jg.writeObjectField("value", value);
    }
}
