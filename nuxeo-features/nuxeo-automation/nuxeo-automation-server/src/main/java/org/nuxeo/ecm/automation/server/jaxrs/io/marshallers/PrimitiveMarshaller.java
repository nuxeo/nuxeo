package org.nuxeo.ecm.automation.server.jaxrs.io.marshallers;

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

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.server.jaxrs.io.JsonMarshaller;

public final class PrimitiveMarshaller<T> implements JsonMarshaller<T> {

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
    public T resolveReference(String ref) {
        if (type.equals("boolean")) {
            return clazz.cast(Boolean.valueOf(ref));
        }
        if (type.equals("string")) {
            return clazz.cast(ref);
        }
        if (type.equals("integer")) {
            return clazz.cast(Integer.valueOf(ref));
        }
        if (type.equals("long")) {
            return clazz.cast(Long.valueOf(ref));
        }
        if (type.equals("double")) {
            return clazz.cast(Double.valueOf(ref));
        }
        throw new UnsupportedOperationException("cannot decode " + type);
    }

    @Override
    public String newReference(T value) {
        return value.toString();
    }

    @Override
    public T read(JSONObject json) {
        return clazz.cast(json.get(type));
    }

    @Override
    public void write(JSONObject json, Object value) {
        json.put(type, value);
    }
}