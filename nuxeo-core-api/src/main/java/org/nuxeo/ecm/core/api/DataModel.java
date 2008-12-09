/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;

/**
 * A data model is a concrete representation of a schema.
 * <p>
 * The schema describe the data structure and the data model object
 * is storing concrete values according to that structure.
 * <p>
 * When the user modifies a data structure the modified fields are tracked
 * so that at any time you can query about the dirty state of the data model
 * by using the {@link DataModel#isDirty()} and
 * {@link DataModel#isDirty(String)} methods.
 * <p>
 * The data model can be modified only through the set methods:
 * <ul>
 * <li> {@link DataModel#setData(String, Object)}
 * <li> {@link DataModel#setMap(Map)}
 * </ul>
 * This is ensuring the dirty state will be correctly updated
 * <p>
 * This is the reason why the {@link DataModel#getMap()} method is returning
 * a read only map.
 * <p>
 * Data structure are usually part of a composite model as the
 * {@link DocumentModel}.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DataModel extends Serializable {

    /**
     * Gets the schema of this data model.
     *
     * @return the data model schema
     */
    String getSchema();

    /**
     * Sets the name field.
     *
     * @param key the field name
     * @param value the value to set. Accept null values.
     * @throws PropertyException
     */
    void setData(String key, Object value) throws PropertyException;

    /**
     * Gets the named field value.
     *
     * @param key the field key
     * @return the value or null if no such field exists
     * @throws PropertyException
     */
    Object getData(String key) throws PropertyException;

    /**
     * Gets all the fields set in this data model.
     * <p>
     * This is not guaranteed that the returned mao contains all
     * the fields defined by the schema. It may even be empty.
     * <p>
     * The returned map is null if the data model was not yet loaded
     *
     * @return a read only map containing actual data in this object
     * @throws PropertyException
     */
    Map<String, Object> getMap() throws PropertyException;

    /**
     * Sets several field at once.
     *
     * @param data the fields to set as a map
     * @throws PropertyException
     */
    void setMap(Map<String, Object> data) throws PropertyException;


    /**
     * Tests whether or not this data model is dirty
     * (i.e. it was changed by the client).
     *
     * @return true if the data model is dirty, false otherwise
     */
    boolean isDirty();

    /**
     * Tests whether or not the specified field from this data model
     * is dirty.
     *
     * @param name the field name to tests
     * @return true if the field is dirty, false otherwise
     * @throws PropertyNotFoundException
     */
    boolean isDirty(String name) throws PropertyNotFoundException;

    /**
     * Marks the specified field from this data model
     * as dirty.
     *
     * @param name the field name to be dirty
     * @throws PropertyNotFoundException
     */
    void setDirty(String name) throws PropertyNotFoundException;

    /**
     * Gets the collection of the dirty fields in this data model.
     *
     * @return the dirty fields or null if there are no dirty fields
     */
    Collection<String> getDirtyFields();

    /**
     * Get a value given its path. the path is a subset of xpath: / and [] are supported
     * @param path the property path
     * @return
     * @throws ParseException
     * @throws PropertyException
     */
    Object getValue(String path) throws PropertyException;

    /**
     * Set a value to a property given its path
     * @param path
     * @param value
     * @return
     * @throws PropertyNotFoundException
     * @throws PropertyException
     */
    Object setValue(String path, Object value) throws PropertyException;

}
