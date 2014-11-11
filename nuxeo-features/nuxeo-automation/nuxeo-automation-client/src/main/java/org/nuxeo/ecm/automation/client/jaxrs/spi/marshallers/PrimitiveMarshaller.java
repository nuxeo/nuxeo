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

import net.sf.json.JSONObject;

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

    @Override
    public T read(JSONObject json) {
        return clazz.cast(json.get(type));
   }

    @Override
    public void write(JSONObject object, Object value) {
        object.put(type, value);
    }
}
