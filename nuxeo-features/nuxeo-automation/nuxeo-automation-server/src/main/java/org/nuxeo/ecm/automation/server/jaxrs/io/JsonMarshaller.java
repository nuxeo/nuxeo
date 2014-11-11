/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     slacoin
 */
package org.nuxeo.ecm.automation.server.jaxrs.io;

import net.sf.json.JSONObject;

/**
 * Plugs in automation server new input/output type marshalling logic.
 * 
 * @author matic
 */
public interface JsonMarshaller<T> {

    /**
     * The type name that appears in json content 
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
     * Resolve a reference and returns the POJO object
     * 
     * @param ref
     * @return
     */
    T resolveReference(String ref);

    /**
     * Returns a server reference to a POJO object
     * 
     * @param value
     * @return
     */
    String newReference(T value);
    
    /**
     * Builds and returns a POJO from the JSON object
     * 
     * @param json
     * @return
     */
    T read(JSONObject json);
    
    /**
     * Writes in the JSON object the POJO's data
     * 
     * @param o
     * @param value
     */
    void write(JSONObject o, Object value);

}