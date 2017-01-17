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

import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.PropertyVisitor;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * A scalar property that is linked to a schema field
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class ComplexProperty extends AbstractProperty implements Map<String, Property> {

    private static final long serialVersionUID = 1L;

    protected Map<String, Property> children;

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
     * This method will always be called using a valid property name (a property specified by the schema). The returned
     * property will be cached by its parent so the next time it is needed, it will be reused from the cache. That means
     * this method servers as a initializer for properties - usually you create a new property and return it - you don't
     * need to cache created properties.
     * <p>
     * If you want to change the way a property is fetched / stored, you must override this method.
     *
     * @return the child. Cannot return null
     * @throws UnsupportedOperationException
     */
    protected Property internalGetChild(Field field) {
        return null; // we don't store property that are not in the cache
    }

    @Override
    public abstract ComplexType getType();

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value instanceof Map;
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable) value;
        }
        throw new PropertyConversionException(value.getClass(), Map.class, getXPath());
    }

    @Override
    public Property get(int index) {
        throw new UnsupportedOperationException("accessing children by index is not allowed for complex properties");
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
            property = getRoot().createProperty(this, field, IS_PHANTOM);
            children.put(property.getName(), property); // cache it
        }
        return property;
    }

    public final Collection<Property> getNonPhantomChildren() {
        ComplexType type = getType();
        if (children.size() < type.getFieldsCount()) { // populate with
                                                       // unloaded props only
                                                       // if needed
            for (Field field : type.getFields()) {
                getNonPhantomChild(field); // force loading non phantom props
            }
        }
        return Collections.unmodifiableCollection(children.values());
    }

    @Override
    public Collection<Property> getChildren() {
        ComplexType type = getType();
        if (children.size() < type.getFieldsCount()) { // populate with
                                                       // phantoms if needed
            for (Field field : type.getFields()) {
                getChild(field); // force loading all props including
                                 // phantoms
            }
        }
        return Collections.unmodifiableCollection(children.values());
    }

    @Override
    public Property get(String name) throws PropertyNotFoundException {
        Field field = getType().getField(name);
        if (field == null) {
            return computeRemovedProperty(name);
        }
        return getChild(field);
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        // noinspection CollectionDeclaredAsConcreteClass
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        for (Property property : getChildren()) {
            map.put(property.getName(), property.getValue());
        }
        return map;
    }

    @Override
    public Serializable getValueForWrite() throws PropertyException {
        if (isPhantom() || isRemoved()) {
            return getDefaultValue();
        }
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        for (Property property : getChildren()) {
            map.put(property.getName(), property.getValueForWrite());
        }
        return map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Serializable value) throws PropertyException {
        if (value == null) { // IGNORE null values - properties will be
                             // considered PHANTOMS
            return;
        }
        Map<String, Serializable> map = (Map<String, Serializable>) value;
        for (Entry<String, Serializable> entry : map.entrySet()) {
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
    public void setValue(Object value) throws PropertyException {
        if (!isContainer()) { // if not a container use default setValue()
            super.setValue(value);
            return;
        }
        if (isReadOnly()) {
            throw new ReadOnlyPropertyException(getXPath());
        }
        if (value == null) {
            remove();
            // completly clear this property
            for (Property child : children.values()) {
                child.remove();
            }
            return; // TODO how to treat nulls?
        }
        if (!(value instanceof Map)) {
            throw new InvalidPropertyValueException(getXPath());
        }
        Map<String, Object> map = (Map<String, Object>) value;
        for (Entry<String, Object> entry : map.entrySet()) {
            Property property = get(entry.getKey());
            if (property.isPhantom() && this.isNew()) {
                // make sure complex list elements are rewritten
                property.setForceDirty(true);
            }
            property.setValue(entry.getValue());
        }
        setValueDeprecation(value, false);
    }

    @Override
    public Property addValue(Object value) {
        throw new UnsupportedOperationException("add(value) operation not supported on map properties");
    }

    @Override
    public Property addValue(int index, Object value) {
        throw new UnsupportedOperationException("add(value, index) operation not supported on map properties");
    }

    @Override
    public Property addEmpty() {
        throw new UnsupportedOperationException("add() operation not supported on map properties");
    }

    public void visitChildren(PropertyVisitor visitor, Object arg) throws PropertyException {
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
     * Should be used by container properties. Non container props must overwrite this.
     */
    @Override
    public boolean isSameAs(Property property) throws PropertyException {
        if (!(property instanceof ComplexProperty)) {
            return false;
        }
        ComplexProperty cp = (ComplexProperty) property;
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

    @Override
    public Iterator<Property> getDirtyChildren() {
        if (!isContainer()) {
            throw new UnsupportedOperationException("Cannot iterate over children of scalar properties");
        }
        return new DirtyPropertyIterator(children.values().iterator());
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property> interface
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property> interface
     */
    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property> interface
     */
    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, Property>> entrySet() {
        return children.entrySet();
    }

    @Override
    public Property get(Object key) {
        return children.get(key);
    }

    @Override
    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return children.keySet();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property> interface
     */
    @Override
    public Property put(String key, Property value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property> interface
     */
    @Override
    public void putAll(Map<? extends String, ? extends Property> t) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws UnsupportedOperationException, added to implement List<Property> interface
     */
    @Override
    public Property remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Property> values() {
        return children.values();
    }

    @Override
    public void clearDirtyFlags() {
        // even makes child properties not dirty
        super.clearDirtyFlags();
        for (Property child : children.values()) {
            if (!child.isRemoved() && !child.isPhantom()) {
                child.clearDirtyFlags();
            }
        }
    }

}
