/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Plugs in automation client new input/output marshalling logic.
 *
 * @author matic
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
    T read(JsonParser jp) throws IOException;

    /**
     * Writes the POJO object to the JsonGenerator
     *
     * @param o
     * @param value
     */
    void write(JsonGenerator jg, Object value) throws IOException;

}
