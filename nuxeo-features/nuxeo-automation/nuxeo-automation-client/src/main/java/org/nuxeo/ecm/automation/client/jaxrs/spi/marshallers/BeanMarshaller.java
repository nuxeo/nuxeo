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

import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

/**
 * @author matic
 *
 */
public class BeanMarshaller<T> implements JsonMarshaller<T> {

    protected final Class<T> clazz;
    
    protected final String type;
    
    protected final JsonConfig config;
    
    public BeanMarshaller(Class<T> clazz) {
        this.clazz = clazz;
        this.type = clazz.getSimpleName().toLowerCase();
        this.config = new JsonConfig();
        this.config.setRootClass(clazz);
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
        JSONObject json = new JSONObject();
        write(json, data);
        return json.toString(2);
    }
    
    @Override
    public T read(JSONObject json) {
        return clazz.cast(JSONObject.toBean(json, config));
    }

    @Override
    public void write(JSONObject object, T value) {
        for (Object e:JSONObject.fromObject(value, config).entrySet()) {
            Map.Entry<?,?> me = (Map.Entry<?, ?>)e;
            object.put(me.getKey(), me.getValue());
        }
    }
}
