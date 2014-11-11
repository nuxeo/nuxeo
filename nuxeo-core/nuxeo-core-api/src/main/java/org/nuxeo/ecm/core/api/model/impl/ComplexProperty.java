/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 *  A scalar property that is linked to a schema field
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ComplexProperty extends AbstractProperty implements
        Map<String, Property> {

    private static final long serialVersionUID = -8189463982083623237L;

    protected transient Map<String, Property> children;

    protected ComplexProperty(Property parent) {
        super(parent);
        children = new HashMap<String, Property>();
    }

    protected ComplexProperty(Property parent, int flags) {
        super(parent, flags);
        children = new HashMap<String, Property>();
    }

    /**
     * Gets the property given its name. If the property was not set, returns null.
     * <p>
     * This method will always be called using a valid property name (a property specified by the schema).
     * The returned property will be cached by its parent so the next time it is needed, it will be reused
     * from the cache. That means this method servers as a initializer for properties - usually you create a new property
     * and return it - you don't need to cache created properties.
     * <p>
     * If you want to change the way a property is fetched / stored you must overwride this method.
     *
     * @param name the child to return
     * @return the child. Cannot return null
     * @throws UnsupportedOperationException
     *
     */
    protected Property internalGetChild(Field field)
            throws UnsupportedOperationException {
        return null; // we don't store property that are not in the cache
    }

    public abstract ComplexType getType();

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value instanceof Map;
    }

    @Override
    public Serializable normalize(Object value)
            throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable)value;
        }
        throw new PropertyConversionException(value.getClass(), Map.class, getPath());
    }

    public Property get(int index) throws PropertyNotFoundException,
        UnsupportedOperationException {
            throw new UnsupportedOperationException(
                    "accessing children by index is not allowed for complex properties");
    }

    public final Property getNonPhantomChild(Field field) {
        String name = field.getName().getPrefixedName();
        Property property = children.get(name);
        if (property == null) {
            property = internalGetChild(field);
            if (property == null) {
                return null;
            }
            children.put(name, property);
        }
        return property;
    }

    public final Property getChild(Field field) {
        Property property = getNonPhantomChild(field);
        if (property == null) {
            property = getRoot().createProperty(this, field,
                    isValidating() ? IS_VALIDATING | IS_PHANTOM : IS_PHANTOM);
            children.put(property.getName(), property); // cache it
        }
        return property;
    }

    public final Collection<Property> getNonPhantomChildren() {
        ComplexType type = getType();
        if (children.size() < type.getFieldsCount()) { // populate with unloaded props only if needed
            for (Field field : type.getFields()) {
                getNonPhantomChild(field); // force loading non phantom props
            }
        }
        return Collections.unmodifiableCollection(children.values());
    }

    public Collection<Property> getChildren() {
        ComplexType type = getType();
        if (children.size() < type.getFieldsCount()) { // populate with phantoms if needed
            for (Field field : type.getFields()) {
                getChild(field); // force loading all props including phantoms
            }
        }
        return Collections.unmodifiableCollection(children.values());
    }

    public Property get(String name) throws PropertyNotFoundException,
            UnsupportedOperationException {
        Field field = getType().getField(name);
        if (field == null) {
            throw new PropertyNotFoundException(name, "");
        }
        return getChild(field);
    }

    public Property set(String name, Object value) throws PropertyException {
        Property property = get(name);
        property.setValue(value);
        return property;
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        //noinspection CollectionDeclaredAsConcreteClass
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        for (Property property : getChildren()) {
            map.put(property.getName(), property.getValue());
        }
        return map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Serializable value) throws PropertyException {
        if (value == null) { // IGNORE null values - properties will be considered PHANTOMS
            return;
        }
        Map<String, Serializable> map = (Map<String, Serializable>)value;
        for (Map.Entry<String, Serializable> entry : map.entrySet()) {
            Property property = get(entry.getKey());
            property.init(entry.getValue());
        }
        removePhantomFlag();
    }

    @Override
    protected Serializable getDefaultValue() {
        return new HashMap<String, Serializable>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Object value)  throws PropertyException {
        if (!isContainer()) { // if not a container use default setValue()
            super.setValue(value);
            return;
        }
        if (isReadOnly()) {
            throw new ReadOnlyPropertyException(getPath());
        }
        if (value == null) {
            remove();
            return; //TODO how to treat nulls?
        }
        if (!(value instanceof Map)) {
            throw new InvalidPropertyValueException(getPath());
        }
        Map<String, Object> map = (Map<String, Object>)value;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Property property = get(entry.getKey());
            property.setValue(entry.getValue());
        }
    }

    public Property add(Object value) throws InvalidPropertyValueException,
            UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "add(value) operation not supported on map properties");
    }

    public Property add(int index, Object value) throws
            InvalidPropertyValueException, UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "add(value, index) operation not supported on map properties");
    }

    public Property add() throws InvalidPropertyValueException,
            UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "add() operation not supported on map properties");
    }

    protected void visitChildren(PropertyVisitor visitor, Object arg)
            throws PropertyException {
        boolean includePhantoms = visitor.acceptPhantoms();
        if (includePhantoms) {
            for (Property property : getChildren()) {
                property.accept(visitor, arg);
            }
        } else {
            for (Field field : getType().getFields()) {
                Property property = getNonPhantomChild(field);
                if (property == null) {
                    continue; // a phantom property not yet initialized
                } else if (property.isPhantom()) {
                    continue; // a phantom property
                } else {
                    property.accept(visitor, arg);
                }
            }
        }
    }

    /**
     * Should be used by container properties.
     * Non container props must overwrite this.
     */
    public boolean isSameAs(Property property) throws PropertyException {
        if (!(property instanceof ComplexProperty)) {
            return false;
        }
        ComplexProperty cp = (ComplexProperty)property;
        if (isContainer()) {
            if (!cp.isContainer()) {
                return false;
            }
            Collection<Property> c1 = getNonPhantomChildren();
            Collection<Property> c2 = cp.getNonPhantomChildren();
            if (c1.size() != c2.size()) {
                return false;
            }
            for (Property p : c1) {
                Property child = cp.getNonPhantomChild(p.getField());
                if (child == null) {
                    return false;
                }
                if (!p.isSameAs(child)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Iterator<Property> getDirtyChildren() {
        if (!isContainer()) {
            throw new UnsupportedOperationException(
                    "Cannot iterate over children of scalar properties");
        }
        return new DirtyPropertyIterator(children.values().iterator());
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    public Set<java.util.Map.Entry<String, Property>> entrySet() {
        return children.entrySet();
    }

    public Property get(Object key) {
        return children.get(key);
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public Set<String> keySet() {
        return children.keySet();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public Property put(String key, Property value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public void putAll(Map<? extends String, ? extends Property> t) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property>
     * interface
     */
    public Property remove(Object key) {
        throw new UnsupportedOperationException();
    }

    public Collection<Property> values() {
        return children.values();
    }

}
