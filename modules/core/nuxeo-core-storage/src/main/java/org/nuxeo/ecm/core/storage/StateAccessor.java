/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import org.nuxeo.ecm.core.api.PropertyException;

/**
 * Basic interface to get/put simple values or arrays from a state object.
 *
 * @since 7.3
 */
public interface StateAccessor {

    /**
     * Gets a single value.
     *
     * @param name the name
     * @return the value
     */
    Object getSingle(String name) throws PropertyException;

    /**
     * Gets an array value.
     *
     * @param name the name
     * @return the value
     */
    Object[] getArray(String name) throws PropertyException;

    /**
     * Sets a single value.
     *
     * @param name the name
     * @param value the value
     */
    void setSingle(String name, Object value) throws PropertyException;

    /**
     * Sets an array value.
     *
     * @param name the name
     * @param value the value
     */
    void setArray(String name, Object[] value) throws PropertyException;

}
