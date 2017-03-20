/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.util.Iterator;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolver;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolverImpl;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

public abstract class AbstractProperty implements Property {

    private static final long serialVersionUID = 1L;

    /**
     * Whether or not this property is read only.
     */
    public static final int IS_READONLY = 32;

    public final Property parent;

    /**
     * for SimpleDocumentModel uses
     */
    public boolean forceDirty = false;

    protected int flags;

    protected AbstractProperty(Property parent) {
        this.parent = parent;
    }

    protected AbstractProperty(Property parent, int flags) {
        this.parent = parent;
        this.flags = flags;
    }

    /**
     * Sets the given normalized value.
     * <p>
     * This applies only for nodes that physically store a value (that means non container nodes). Container nodes does
     * nothing.
     *
     * @param value
     */
    public abstract void internalSetValue(Serializable value) throws PropertyException;

    public abstract Serializable internalGetValue() throws PropertyException;

    @Override
    public void init(Serializable value) throws PropertyException {
        if (value == null || (value instanceof Object[] && ((Object[]) value).length == 0)) {
            // ignore null or empty values, properties will be considered phantoms
            return;
        }
        internalSetValue(value);
        removePhantomFlag();
    }

    public void removePhantomFlag() {
        flags &= ~IS_PHANTOM;
        if (parent != null) {
            ((AbstractProperty) parent).removePhantomFlag();
        }
    }

    @Override
    public void setValue(int index, Object value) throws PropertyException {
        Property property = get(index);
        property.setValue(value);
    }

    @Override
    public int size() {
        return getChildren().size();
    }

    @Override
    public Iterator<Property> iterator() {
        return getChildren().iterator();
    }

    @Override
    public Serializable remove() throws PropertyException {
        Serializable value = getValue();
        if (parent != null && parent.isList()) { // remove from list is
            // handled separately
            ListProperty list = (ListProperty) parent;
            list.remove(this);
        } else if (!isPhantom()) { // remove from map is easier -> mark the
            // field as removed and remove the value
            // do not remove the field if the previous value was null, except if its a property from a SimpleDocumentModel (forceDirty mode)
            Serializable previous = internalGetValue();
            init(null);
            if (previous != null || isForceDirty()) {
                setIsRemoved();
            }
        }
        return value;
    }

    @Override
    public Property getParent() {
        return parent;
    }

    @Override
    public String getPath() {
        Path path = collectPath(new Path("/"));
        return path.toString();
    }

    protected Path collectPath(Path path) {
        String name = getName();
        if (parent != null) {
            if (parent.isList()) {
                int i = ((ListProperty) parent).children.indexOf(this);
                name = name + '[' + i + ']';
            }
            path = ((AbstractProperty) parent).collectPath(path);
        }
        return path.append(name);
    }

    @Override
    public Schema getSchema() {
        return getRoot().getSchema();
    }

    @Override
    public boolean isList() {
        return getType().isListType();
    }

    @Override
    public boolean isComplex() {
        return getType().isComplexType();
    }

    @Override
    public boolean isScalar() {
        return getType().isSimpleType();
    }

    @Override
    public boolean isNew() {
        return areFlagsSet(IS_NEW);
    }

    @Override
    public boolean isRemoved() {
        return areFlagsSet(IS_REMOVED);
    }

    @Override
    public boolean isMoved() {
        return areFlagsSet(IS_MOVED);
    }

    @Override
    public boolean isModified() {
        return areFlagsSet(IS_MODIFIED);
    }

    @Override
    public boolean isPhantom() {
        return areFlagsSet(IS_PHANTOM);
    }

    @Override
    public final boolean isDirty() {
        return (flags & IS_DIRTY) != 0;
    }

    protected final void setDirtyFlags(int dirtyFlags) {
        flags = dirtyFlags & DIRTY_MASK | flags & ~DIRTY_MASK;
    }

    protected final void appendDirtyFlags(int dirtyFlags) {
        flags |= (dirtyFlags & DIRTY_MASK);
    }

    @Override
    public boolean isReadOnly() {
        return areFlagsSet(IS_READONLY);
    }

    @Override
    public void setReadOnly(boolean value) {
        if (value) {
            setFlags(IS_READONLY);
        } else {
            clearFlags(IS_READONLY);
        }
    }

    public final boolean areFlagsSet(long flags) {
        return (this.flags & flags) != 0;
    }

    public final void setFlags(long flags) {
        this.flags |= flags;
    }

    public final void clearFlags(long flags) {
        this.flags &= ~flags;
    }

    @Override
    public int getDirtyFlags() {
        return flags & DIRTY_MASK;
    }

    @Override
    public void clearDirtyFlags() {
        if ((flags & IS_REMOVED) != 0) {
            // if is removed the property becomes a phantom
            setDirtyFlags(IS_PHANTOM);
        } else {
            setDirtyFlags(NONE);
        }
    }

    /**
     * This method is public because of DataModelImpl which use it.
     * <p>
     * TODO after removing DataModelImpl make it protected.
     */
    public void setIsModified() {
        if ((flags & IS_MODIFIED) == 0) { // if not already modified
            // clear dirty + phatom flag if any
            flags |= IS_MODIFIED; // set the modified flag
            flags &= ~IS_PHANTOM; // remove phantom flag if any
        }
        if (parent != null) {
            ((AbstractProperty) parent).setIsModified();
        }
    }

    protected void setIsNew() {
        if (isDirty()) {
            throw new IllegalStateException("Cannot set IS_NEW flag on a dirty property");
        }
        // clear dirty + phantom flag if any
        setDirtyFlags(IS_NEW); // this clear any dirty flag and set the new
        // flag
        if (parent != null) {
            ((AbstractProperty) parent).setIsModified();
        }
    }

    protected void setIsRemoved() {
        if (isPhantom() || parent == null || parent.isList()) {
            throw new IllegalStateException("Cannot set IS_REMOVED on removed or properties that are not map elements");
        }
        if ((flags & IS_REMOVED) == 0) { // if not already removed
            // clear dirty + phatom flag if any
            setDirtyFlags(IS_REMOVED);
            ((AbstractProperty) parent).setIsModified();
        }
    }

    protected void setIsMoved() {
        if (parent == null || !parent.isList()) {
            throw new IllegalStateException("Cannot set IS_MOVED on removed or properties that are not map elements");
        }
        if ((flags & IS_MOVED) == 0) {
            flags |= IS_MOVED;
            ((AbstractProperty) parent).setIsModified();
        }
    }

    @Override
    public <T> T getValue(Class<T> type) throws PropertyException {
        return convertTo(getValue(), type);
    }

    @Override
    public void setValue(Object value) throws PropertyException {
        // 1. check the read only flag
        if (isReadOnly()) {
            throw new ReadOnlyPropertyException(getPath());
        }
        // 1. normalize the value
        Serializable normalizedValue = normalize(value);
        // 2. backup the current
        Serializable current = internalGetValue();
        // if its a phantom, no need to check for changes, set it dirty
        if (!isSameValue(normalizedValue, current) || isForceDirty()) {
            // 3. set the normalized value and
            internalSetValue(normalizedValue);
            // 4. update flags
            setIsModified();
        } else {
            removePhantomFlag();
        }
    }

    protected boolean isSameValue(Serializable value1, Serializable value2) {
        return ((value1 == null && value2 == null) || (value1 != null && value1.equals(value2)));
    }

    @Override
    public void setValue(String path, Object value) throws PropertyException {
        resolvePath(path).setValue(value);
    }

    @Override
    public <T> T getValue(Class<T> type, String path) throws PropertyException {
        return resolvePath(path).getValue(type);
    }

    @Override
    public Serializable getValue(String path) throws PropertyException {
        return resolvePath(path).getValue();
    }

    @Override
    public Serializable getValue() throws PropertyException {
        if (isPhantom() || isRemoved()) {
            return getDefaultValue();
        }
        return internalGetValue();
    }

    @Override
    public Serializable getValueForWrite() throws PropertyException {
        return getValue();
    }

    protected Serializable getDefaultValue() {
        return (Serializable) getField().getDefaultValue();
    }

    @Override
    public void moveTo(int index) {
        if (parent == null || !parent.isList()) {
            throw new UnsupportedOperationException("Not a list item property");
        }
        ListProperty list = (ListProperty) parent;
        if (list.moveTo(this, index)) {
            setIsMoved();
        }
    }

    @Override
    public DocumentPart getRoot() {
        return parent == null ? (DocumentPart) this : parent.getRoot();
    }

    @Override
    public Property resolvePath(String path) throws PropertyNotFoundException {
        return resolvePath(new Path(path));
    }

    @Override
    public Property resolvePath(Path path) throws PropertyNotFoundException {
        // handle absolute paths -> resolve them relative to the root
        if (path.isAbsolute()) {
            return getRoot().resolvePath(path.makeRelative());
        }

        String[] segments = path.segments();
        // handle ../../ paths
        Property property = this;
        int start = 0;
        for (; start < segments.length; start++) {
            if (segments[start].equals("..")) {
                property = property.getParent();
            } else {
                break;
            }
        }

        // now start resolving the path from 'start' depth relative to
        // 'property'
        for (int i = start; i < segments.length; i++) {
            String segment = segments[i];
            if (property.isScalar()) {
                throw new PropertyNotFoundException(path.toString(), "segment " + segment
                        + " points to a scalar property");
            }
            String index = null;
            if (segment.endsWith("]")) {
                int p = segment.lastIndexOf('[');
                if (p == -1) {
                    throw new PropertyNotFoundException(path.toString(), "Parse error: no matching '[' was found");
                }
                index = segment.substring(p + 1, segment.length() - 1);
                segment = segment.substring(0, p);
            }
            if (index == null) {
                property = property.get(segment);
                if (property == null) {
                    throw new PropertyNotFoundException(path.toString(), "segment " + segments[i]
                            + " cannot be resolved");
                }
            } else {
                property = property.get(index);
            }
        }
        return property;
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable) value;
        }
        throw new PropertyConversionException(value.getClass(), Serializable.class, getPath());
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value instanceof Serializable;
    }

    @Override
    public <T> T convertTo(Serializable value, Class<T> toType) throws PropertyConversionException {
        // TODO FIXME XXX make it abstract at this level
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean validateType(Class<?> type) {
        return true; // TODO XXX FIXME
    }

    @Override
    public Object newInstance() {
        return null; // TODO XXX FIXME
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + getPath() + ')';
    }

    @Override
    public PropertyObjectResolver getObjectResolver() {
        ObjectResolver resolver = getType().getObjectResolver();
        if (resolver != null) {
            return new PropertyObjectResolverImpl(this, resolver);
        }
        return null;
    }

    @Override
    public boolean isForceDirty() {
        return forceDirty;
    }

    @Override
    public void setForceDirty(boolean forceDirty) {
        this.forceDirty = forceDirty;
    }

}
