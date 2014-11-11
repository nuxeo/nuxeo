package org.nuxeo.ecm.automation.server.jaxrs.io;

/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */

import java.util.HashMap;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.server.jaxrs.io.marshallers.DateMarshaller;
import org.nuxeo.ecm.automation.server.jaxrs.io.marshallers.PrimitiveMarshaller;

/**
 * Handles input/output marshalling and references resolution.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JsonMarshalling {

    public JsonMarshalling() {
    }

    protected final HashMap<String, JsonMarshaller<?>> marshallersByType = new HashMap<String, JsonMarshaller<?>>();
    protected final HashMap<Class<?>, JsonMarshaller<?>> marshallersByJavaType = new HashMap<Class<?>, JsonMarshaller<?>>();
    {
        addMarshaller(new DateMarshaller());
        addMarshaller(new PrimitiveMarshaller<Boolean>(Boolean.class));
        addMarshaller(new PrimitiveMarshaller<Integer>(Integer.class));
        addMarshaller(new PrimitiveMarshaller<Long>(Long.class));
        addMarshaller(new PrimitiveMarshaller<Double>(Double.class));
        addMarshaller(new PrimitiveMarshaller<String>(String.class));
    }

    /**
     * Register the new marshalling logic
     * 
     * @param marshaller
     */
    public void addMarshaller(JsonMarshaller<?> marshaller) {
        marshallersByJavaType.put(marshaller.getJavaType(), marshaller);
        marshallersByType.put(marshaller.getType(), marshaller);
    }

    /**
     * Remove the marshalling logic associated to the provided java type
     * 
     * @param clazz
     */
    public void removeMarshaller(Class<?> clazz) {
        JsonMarshaller<?> marshaller = marshallersByJavaType.remove(clazz);
        if (marshaller == null) {
            throw new IllegalArgumentException("no registered marshaller for " + clazz.getName());
        }
        marshallersByType.remove(marshaller.getType());
    }
    
    /**
     * Indicates if we can marshall the requested java type
     * 
     * @param clazz
     * @return
     */
    public boolean canMarshall(Class<?> clazz) {
        return getMarshaller(clazz) != null;
    }

    /**
     * Access to the marshalling logic giving a JSON type
     * 
     * @param <T>
     * @param type
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> JsonMarshaller<T> getMarshaller(String type) {
        JsonMarshaller<?> js = marshallersByType.get(type);
        return (JsonMarshaller<T>)js;
    }

    /**
     * Access to the marshalling logic giving a java type
     * @param <T>
     * @param clazz
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> JsonMarshaller<T> getMarshaller(Class<T> clazz) {
         JsonMarshaller<?> js = marshallersByJavaType.get(clazz);
        return (JsonMarshaller<T>)js;
    }

    public <T> void write(Class<T> clazz, JSONObject object, Object value) {
        JsonMarshaller<T> m = getMarshaller(clazz);
        if (m == null) {
            throw new IllegalArgumentException("no json marshaller for "
                    + clazz);
        }
        m.write(object, value);
    }

    public <T> T read(String type, JSONObject object) {
        JsonMarshaller<T> m = getMarshaller(type);
        if (m == null) {
            throw new IllegalArgumentException("no json marshaller for " + type);
        }
        return m.read(object);
    }

}
