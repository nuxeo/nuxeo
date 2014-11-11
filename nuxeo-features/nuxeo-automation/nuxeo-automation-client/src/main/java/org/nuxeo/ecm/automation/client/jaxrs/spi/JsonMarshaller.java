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
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import net.sf.json.JSONObject;

/**
 * Plugs in automation client new input/output marshalling logic.
 *
 * @author matic
 *
 * @param <T>
 */
public interface JsonMarshaller<T> {
    /**
     * The type name that appears in serialization 
     * 
     * @return
     */
    String getType();
    
    /**
     * The marshalled java type
     * 
     * @return
     */
    Class<T> getJavaType();
    
    /**
     * Gets an input reference from the POJO object that
     * can be fetched server side.
     * 
     * @param ref
     * @return
     */
    String getReference(T value);
    
    /**
     * Builds and returns a POJO from the JSON object
     * 
     * @param json
     * @return
     */
    T read(JSONObject object);
    
    /**
     * Writes in the JSON object the POJO's data
     * 
     * @param o
     * @param value
     */
    void write(JSONObject object, T data);
}