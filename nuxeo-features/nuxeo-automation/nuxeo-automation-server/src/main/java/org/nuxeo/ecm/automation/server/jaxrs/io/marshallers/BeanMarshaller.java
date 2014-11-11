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


import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

import org.nuxeo.ecm.automation.server.jaxrs.io.JsonMarshaller;

public class BeanMarshaller<T> implements JsonMarshaller<T> {
  
    protected final Class<T> clazz;
    protected final JsonConfig config;
        
    public BeanMarshaller(Class<T> clazz) {
        this.clazz = clazz;
        this.config = new JsonConfig();
        this.config.setRootClass(clazz);
    }
    
    @Override
    public String getType() {
        return clazz.getSimpleName().toLowerCase();
    }
    
    @Override
    public Class<T> getJavaType() {
        return clazz;
    }
    
    @Override
    public T resolveReference(String ref) {
        return read(JSONObject.fromObject(ref, config));
    }

    @Override
    public String newReference(T value) {
        JSONObject json =new JSONObject();
        write(json, value);
        return json.toString(2);
    }
    
    @Override
    public T read(JSONObject json) {
        return clazz.cast(JSONObject.toBean(json, config));
    }
    
    @Override
    public void write(JSONObject json, Object value) {
        JSONObject o = JSONObject.fromObject(value, config);
        for (Object e:o.entrySet()) {
            Map.Entry<?,?> me = (Map.Entry<?, ?>)e;
            json.put(me.getKey(), me.getValue());
        }
    }
}