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

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;

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
     * Builds and returns a POJO from the JSON object
     *
     * @param json
     * @return
     */
    T read(JsonParser jp) throws Exception;

    /**
     * Writes the POJO object to the JsonGenerator
     *
     * @param o
     * @param value
     */
    void write(JsonGenerator jg, T value) throws Exception;

}
