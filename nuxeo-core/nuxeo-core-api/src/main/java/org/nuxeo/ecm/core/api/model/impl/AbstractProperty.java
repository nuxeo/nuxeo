/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolver;
import org.nuxeo.ecm.core.api.model.resolver.PropertyObjectResolverImpl;
import org.nuxeo.ecm.core.schema.PropertyDeprecationHandler;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractProperty implements Property {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(AbstractProperty.class);

    protected final static Pattern NON_CANON_INDEX = Pattern.compile("[^/\\[\\]]+" // name
            + "\\[(-?\\d+)\\]" // index in brackets - could be -1 if element is new to list
    );

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

    protected Boolean deprecated;

    protected Property deprecatedFallback;

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
            // do not remove the field if the previous value was null, except if its a property from a
            // SimpleDocumentModel (forceDirty mode)
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
    public String getXPath() {
        StringBuilder sb = new StringBuilder();
        getXPath(sb);
        return sb.toString();
    }

    protected void getXPath(StringBuilder sb) {
        if (parent != null) {
            ((AbstractProperty) parent).getXPath(sb);
            if (parent.isList()) {
                sb.append('/');
                int i = ((ListProperty) parent).children.indexOf(this);
                sb.append(i);
            } else {
                if (sb.length() != 0) {
                    sb.append('/');
                }
                sb.append(getName());
            }
        }
    }

    @Override
    @Deprecated
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

    @Override
    public boolean hasDefaultValue() {
        if (isComplex()) {
            return getChildren().stream().anyMatch(Property::hasDefaultValue);
        } else {
            return getField().getDefaultValue() != null;
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

    protected boolean isDeprecated() {
        if (deprecated == null) {
            boolean localDeprecated = false;
            // compute the deprecated state
            // first check if this property is a child of a deprecated property
            if (parent instanceof AbstractProperty) {
                AbstractProperty absParent = (AbstractProperty) parent;
                localDeprecated = absParent.isDeprecated();
                Property parentDeprecatedFallback = absParent.deprecatedFallback;
                if (localDeprecated && parentDeprecatedFallback != null) {
                    deprecatedFallback = parentDeprecatedFallback.resolvePath(getName());
                }
            }
            if (!localDeprecated) {
                // check if this property is deprecated
                String name = getXPath();
                String schema = getSchema().getName();
                SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                PropertyDeprecationHandler deprecatedProperties = schemaManager.getDeprecatedProperties();
                localDeprecated = deprecatedProperties.isMarked(schema, name);
                if (localDeprecated) {
                    // get the possible fallback
                    String fallback = deprecatedProperties.getFallback(schema, name);
                    if (fallback != null) {
                        deprecatedFallback = resolvePath('/' + fallback);
                    }
                }
            }
            deprecated = Boolean.valueOf(localDeprecated);
        }
        return deprecated.booleanValue();
    }

    @Override
    public <T> T getValue(Class<T> type) throws PropertyException {
        return convertTo(getValue(), type);
    }

    @Override
    public void setValue(Object value) throws PropertyException {
        // 1. check the read only flag
        if (isReadOnly()) {
            throw new ReadOnlyPropertyException(getXPath());
        }
        // 1. normalize the value
        Serializable normalizedValue = normalize(value);
        // 2. backup the current
        Serializable current = internalGetValue();
        // if its a phantom, no need to check for changes, set it dirty
        if (!isSameValue(normalizedValue, current) || isForceDirty()) {
            // 3. set the normalized value and
            internalSetValue(normalizedValue);
            // 4. set also value to fallback if this property is deprecated and has one
            setValueDeprecation(normalizedValue, true);
            // 5. update flags
            setIsModified();
        } else {
            removePhantomFlag();
        }
    }

    /**
     * If this property is deprecated and has a fallback, set value to fallback.
     */
    protected void setValueDeprecation(Object value, boolean setFallback) throws PropertyException {
        if (isDeprecated()) {
            // First check if we need to set the fallback value
            if (setFallback && deprecatedFallback != null) {
                deprecatedFallback.setValue(value);
            }
            // Second check if we need to log deprecation message
            if (log.isWarnEnabled()) {
                StringBuilder msg = newDeprecatedMessage();
                msg.append("Set value to deprecated property");
                if (deprecatedFallback != null) {
                    msg.append(" and to fallback property '").append(deprecatedFallback.getXPath()).append("'");
                }
                if (log.isTraceEnabled()) {
                    log.warn(msg, new NuxeoException("debug stack trace"));
                } else {
                    log.warn(msg);
                }
            }
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
        Serializable fallbackValue = getValueDeprecation();
        return fallbackValue == null ? internalGetValue() : fallbackValue;
    }

    /**
     * @return the fallback value if this property is deprecated and has a fallback, otherwise return null
     */
    protected Serializable getValueDeprecation() {
        if (isDeprecated()) {
            // Check if we need to log deprecation message
            if (log.isWarnEnabled()) {
                StringBuilder msg = newDeprecatedMessage();
                if (deprecatedFallback == null) {
                    msg.append("Return value from deprecated property");
                } else {
                    msg.append("Return value from '")
                       .append(deprecatedFallback.getXPath())
                       .append("' if not null, from deprecated property otherwise");
                }
                if (log.isTraceEnabled()) {
                    log.warn(msg, new NuxeoException());
                } else {
                    log.warn(msg);
                }
            }
            if (deprecatedFallback != null) {
                return deprecatedFallback.getValue();
            }
        }
        return null;
    }

    protected StringBuilder newDeprecatedMessage() {
        StringBuilder builder = new StringBuilder().append("Property '")
                                                   .append(getXPath())
                                                   .append("' is marked as deprecated from '")
                                                   .append(getSchema().getName())
                                                   .append("' schema");
        Property deprecatedParent = getDeprecatedParent();
        if (deprecatedParent != this) {
            builder.append(" because property '")
                   .append(deprecatedParent.getXPath())
                   .append("' is marked as deprecated");
        }
        return builder.append(", don't use it anymore. ");
    }

    /**
     * @return the higher deprecated parent.
     */
    protected AbstractProperty getDeprecatedParent() {
        if (parent instanceof AbstractProperty) {
            AbstractProperty absParent = (AbstractProperty) parent;
            if (absParent.isDeprecated()) {
                return absParent.getDeprecatedParent();
            }
        }
        return this;
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
                throw new PropertyNotFoundException(path.toString(),
                        "segment " + segment + " points to a scalar property");
            }
            String index = null;
            if (segment.endsWith("]")) {
                int p = segment.lastIndexOf('[');
                if (p == -1) {
                    throw new PropertyNotFoundException(path.toString(), "Parse error: no matching '[' was found");
                }
                index = segment.substring(p + 1, segment.length() - 1);
            }
            if (index == null) {
                property = property.get(segment);
                if (property == null) {
                    throw new PropertyNotFoundException(path.toString(), "segment " + segment + " cannot be resolved");
                }
            } else {
                property = property.get(index);
            }
        }
        return property;
    }

    /**
     * Returns the {@link RemovedProperty} if it is a removed property or null otherwise.
     *
     * @since 9.2
     */
    protected Property computeRemovedProperty(String name) {
        String schema = getSchema().getName();
        // name is only the property name we try to get, build its path in order to check it against configuration
        String originalXpath = collectPath(new Path("/")).append(name).toString().substring(1);
        String xpath;
        // replace all something[..] in a path by *, for example files/item[2]/filename -> files/*/filename
        if (originalXpath.indexOf('[') != -1) {
            xpath = NON_CANON_INDEX.matcher(originalXpath).replaceAll("*");
        } else {
            xpath = originalXpath;
        }
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        PropertyDeprecationHandler removedProperties = schemaManager.getRemovedProperties();
        if (!removedProperties.isMarked(schema, xpath)) {
            return null;
        }
        String fallback = removedProperties.getFallback(schema, xpath);
        if (fallback == null) {
            return new RemovedProperty(this, name);
        }

        // Retrieve fallback property
        Matcher matcher = NON_CANON_INDEX.matcher(originalXpath);
        while (matcher.find()) {
            fallback = fallback.replaceFirst("\\*", matcher.group(0));
        }
        Property fallbackProperty;
        // Handle creation of complex property in a list ie: xpath contains [-1]
        int i = fallback.lastIndexOf("[-1]");
        if (i != -1) {
            // skip [-1]/ to get next property
            fallbackProperty = get(fallback.substring(i + 5));
        } else {
            fallbackProperty = resolvePath('/' + fallback);
        }
        return new RemovedProperty(this, name, fallbackProperty);
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable) value;
        }
        throw new PropertyConversionException(value.getClass(), Serializable.class, getXPath());
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
        return getClass().getSimpleName() + '(' + getXPath() + ')';
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
