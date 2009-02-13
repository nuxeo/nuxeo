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

package org.nuxeo.ecm.core.model;

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface PropertyContainer {

    /**
     * Tests whether the property at the given path exists (the property may be
     * either a scalar or a composite one).
     *
     * @param path the path to test
     * @return true if the property at the given path exists, false otherwise
     * @throws DocumentException if any error occurs
     */
    boolean isPropertySet(String path) throws DocumentException;

    /**
     * Gets a property given its name.
     * <p>
     * If the named property is specified by schema of the container object this
     * method will always return a property object even if the property was not
     * yet set (doesn't exists).
     * <p>
     * In this case the method {@link Property#isNull()} of the returned
     * property will return true. If the property is not specified by any
     * container schema the {@link NoSuchPropertyException} will be thrown
     * returns non null even if the property corresponding to that name was not
     * previously set. The returned object is a scalar, composite or list
     * property.
     *
     * @param name the property name to retrieve
     * @return the property object
     * @throws DocumentException if any error occurs
     */
    Property getProperty(String name) throws DocumentException;

    /**
     * Removes the property with the given name.
     *
     * @param name the property to remove
     * @throws DocumentException if any error occurs
     */
    void removeProperty(String name) throws DocumentException;

    /**
     * Generic method to set a property value.
     * <p>
     * This is a shortcut for <code>getProperty(String).setValue(Object)</code>
     * <p>
     * The following type of objects can be used as values depending on the
     * property type:
     * <ul>
     * <li> {@link String} - for string properties
     * <li> {@link Boolean} - for boolean properties
     * <li> {@link Number} - for numeric (long and double) properties
     * <li> {@link Calendar} - for date properties
     * <li> {@link ContentSource} - for content properties
     * </ul>
     *
     * @param name the name of the property to set
     * @param value the value to set
     * @throws DocumentException if any error occurs
     */
    void setPropertyValue(String name, Object value) throws DocumentException;

    /**
     * Generic method to retrieve a property value.
     * <p>
     * This is a shortcut for <code>getProperty(String).getValue()</code>
     *
     * @see #setPropertyValue(String, Object) for the list of supported value
     *      objects
     *
     * @param name the name of the property to set
     * @return the property value or <code>null</code> if the property is not
     *         set.
     * @throws DocumentException if any error occurs
     */
    Object getPropertyValue(String name) throws DocumentException;

    /**
     * Gets the value of a scalar property as a <code>string</code>.
     * <p>
     * This is a shortcut for <code>getScalar(name).getString()</code>
     *
     * @see {@link SimpleProperty#getString()};
     * @throws DocumentException if any error occurs
     */
    String getString(String name) throws DocumentException;

    /**
     * Gets the value of a scalar property as a <code>boolean</code>.
     * <p>
     * This is a shortcut for <code>getScalar(name).getBoolean()</code>
     *
     * @see {@link SimpleProperty#getBoolean()};
     * @throws DocumentException if any error occurs
     */
    boolean getBoolean(String name) throws DocumentException;

    /**
     * Gets the value of a scalar property as a <code>double</code>.
     * <p>
     * This is a shortcut for <code>getScalar(name).getDouble()</code>
     *
     * @see {@link SimpleProperty#getDouble()};
     * @throws DocumentException if any error occurs
     */
    double getDouble(String name) throws DocumentException;

    /**
     * Gets the value of a scalar property as a <code>long</code>.
     * <p>
     * This is a shortcut for <code>getScalar(name).getLong()</code>
     *
     * @see {@link SimpleProperty#getLong()};
     * @throws DocumentException if any error occurs
     */
    long getLong(String name) throws DocumentException;

    /**
     * Gets the value of a scalar property as a <code>date</code>.
     * <p>
     * This is a shortcut for <code>getScalar(name).getDate()</code>
     *
     * @see {@link SimpleProperty#getDate()};
     * @throws DocumentException if any error occurs
     */
    Calendar getDate(String name) throws DocumentException;

    /**
     * Gets the value of the named content property.
     *
     * @throws DocumentException if any error occurs
     */
    Blob getContent(String name) throws DocumentException;

    /**
     * Sets the scalar property value to the given string value.
     * <p>
     * If the property with that name doesn't exists, it will be created.
     * <p>
     * This is a shortcut to create or set string properties.
     *
     * @see {@link SimpleProperty#setString(String)}
     * @throws DocumentException if any error occurs
     */
    void setString(String name, String value) throws DocumentException;

    /**
     * Sets the scalar property value to the given boolean value.
     * <p>
     * If the property with that name doesn't exists, it will be created.
     * <p>
     * This is a shortcut to create or set boolean properties.
     *
     * @see {@link SimpleProperty#setBoolean(boolean)}
     * @throws DocumentException if any error occurs
     */
    void setBoolean(String name, boolean value) throws DocumentException;

    /**
     * Sets the scalar property value to the given long value.
     * <p>
     * If the property with that name doesn't exists, it will be created.
     * <p>
     * This is a shortcut to create or set long properties.
     *
     * @see {@link SimpleProperty#setLong(long)}
     * @throws DocumentException if any error occurs
     */
    void setLong(String name, long value) throws DocumentException;

    /**
     * Set the scalar property value to the given double value.
     * <p>
     * If the property with that name doesn't exists, it will be created.
     * <p>
     * This is a shortcut to create or set double properties
     *
     * @see {@link SimpleProperty#setDouble(double)}
     * @throws DocumentException if any error occurs
     */
    void setDouble(String name, double value) throws DocumentException;

    /**
     * Sets the scalar property value to the given date value.
     * <p>
     * If the property with that name doesn't exists, it will be created
     * <p>
     * This is a shortcut to create or set date properties.
     *
     * @see {@link SimpleProperty#setDate(Calendar)}
     * @throws DocumentException if any error occurs
     */
    void setDate(String name, Calendar value) throws DocumentException;

    /**
     * Sets the content property to the given value.
     * <p>
     * If the property with that name doesn't exists, it will be created
     *
     * @throws DocumentException if any error occurs
     */
    void setContent(String name, Blob value) throws DocumentException;

    /**
     * Exports the properties belonging to the given schemas as a java Map.
     * <p>
     * If the given schemas array is null then all schemas will be exported
     * <p>
     * The properties are grouped by schemas
     * <p>
     * The property tree is recursively traversed and all property in that
     * schema exported as entry of the Map
     *
     * @param schemas
     * @return the exported properties as a java Map
     * @throws DocumentException if any error occurs
     */
    Map<String, Map<String, Object>> exportMap(String[] schemas)
            throws DocumentException;

    Map<String, Object> exportMap(String schemaName) throws DocumentException;

    /**
     * Imports the tree properties from the given Java Map.
     * <p>
     * The property tree is recursively traversed and all property exported as
     * entry of the Map.
     *
     * @throws DocumentException if any error occurs
     */
    void importMap(Map<String, Map<String, Object>> map)
            throws DocumentException;

    /**
     * Exports a flat view of properties in this document.
     * <p>
     * If the given schemas array is null then all schemas will be exported.
     *
     * @param schemas
     * @return
     * @throws DocumentException if any error occurs
     */
    Map<String, Object> exportFlatMap(String[] schemas)
            throws DocumentException;

    /**
     * Imports a flat map of properties into this document.
     *
     * @param map
     * @throws DocumentException if any error occurs
     */
    void importFlatMap(Map<String, Object> map) throws DocumentException;

    /**
     * Gets the collection of the sub properties in this container.
     * <p>
     * The returned properties are existing.
     *
     * @return the existing properties in this container
     * @throws DocumentException if any error occurs
     */
    Collection<Property> getProperties() throws DocumentException;

    /**
     * Gets an iterator over all existing properties in this container.
     * <p>
     * The returned properties are existing.
     *
     * @return the existing properties in this container
     * @throws DocumentException if any error occurs
     */
    Iterator<Property> getPropertyIterator() throws DocumentException;

    /**
     * Returns fields that were modified.
     * <p>
     * XXX AT: compatibility method for NXP-666
     */
    List<String> getDirtyFields();

}
