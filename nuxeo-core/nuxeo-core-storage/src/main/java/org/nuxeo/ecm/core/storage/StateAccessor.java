/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage;

import org.nuxeo.ecm.core.api.model.PropertyException;

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
