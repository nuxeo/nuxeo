/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceMap;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@code Node} implementation. The actual data is stored in contained objects
 * that are {@link Fragment}s.
 */
public class Node {

    /** The persistence context used. */
    private final PersistenceContext context;

    private final Model model;

    /** The hierarchy/main fragment. */
    protected final SimpleFragment hierFragment;

    /** Fragment information for each additional mixin or inherited fragment. */
    protected final FragmentsMap fragments;

    /**
     * Path, only for immediate consumption after construction (will be reset to
     * null afterwards).
     */
    protected String path;

    /**
     * Cache of property objects already retrieved. They are dumb objects, just
     * providing an indirection to an underlying {@link Fragment}.
     */
    private final Map<String, BaseProperty> propertyCache;

    private Boolean isVersion;

    /**
     * Creates a Node.
     *
     * @param context the persistence context
     * @param fragmentGroup the group of fragments for the node
     * @param path the path, if known at construction time
     */
    @SuppressWarnings("unchecked")
    protected Node(PersistenceContext context, FragmentGroup fragmentGroup,
            String path) throws StorageException {
        this.context = context;
        model = context.model;
        hierFragment = fragmentGroup.hier;
        if (fragmentGroup.fragments == null) {
            fragments = new FragmentsMap();
        } else {
            fragments = fragmentGroup.fragments;
        }
        this.path = path;
        // memory-sensitive
        propertyCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
    }

    // ----- basics -----

    /**
     * Gets the node unique id, usually a Long or a String.
     *
     * @return the node id
     */
    public Serializable getId() {
        /*
         * We don't cache the id as it changes between the initial creation and
         * the first save.
         */
        return hierFragment.getId();
    }

    public String getName() {
        try {
            return getHierFragment().getString(model.HIER_CHILD_NAME_KEY);
        } catch (StorageException e) {
            // do not propagate this unlikely exception as a checked one
            throw new RuntimeException(e);
        }
    }

    public String getPrimaryType() {
        try {
            return hierFragment.getString(model.MAIN_PRIMARY_TYPE_KEY);
        } catch (StorageException e) {
            // do not propagate this unlikely exception as a checked one
            throw new RuntimeException(e);
        }
    }

    public Serializable getParentId() {
        try {
            return getHierFragment().get(model.HIER_PARENT_KEY);
        } catch (StorageException e) {
            // do not propagate this unlikely exception as a checked one
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the path that was assigned at {@link Node} construction time. Then
     * it's reset to {@code null}. Should only be used once.
     *
     * @return the path, or {@code null} for unknown
     */
    public String getPath() {
        String p = path;
        if (p != null) {
            path = null;
        }
        return p;
    }

    protected SimpleFragment getHierFragment() {
        return hierFragment;
    }

    // cache the isVersion computation
    public boolean isVersion() {
        if (isVersion == null) {
            try {
                isVersion = (Boolean) getSimpleProperty(
                        model.MAIN_IS_VERSION_PROP).getValue();
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
            if (isVersion == null) {
                isVersion = Boolean.FALSE;
            }
        }
        return isVersion.booleanValue();
    }

    public boolean isProxy() {
        return getPrimaryType().equals(model.PROXY_TYPE);
    }

    private static final String[] NO_MIXINS = {};

    /**
     * Gets the instance mixins. Mixins from the type are not returned.
     * <p>
     * Never returns {@code null}.
     */
    public String[] getMixinTypes() {
        try {
            String[] value = (String[]) hierFragment.get(model.MAIN_MIXIN_TYPES_KEY);
            return value == null ? NO_MIXINS : value.clone();
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the mixins. Includes mixins from the type. Returns a fresh set.
     */
    public Set<String> getAllMixinTypes() {
        // linked for deterministic result
        Set<String> mixins = new LinkedHashSet<String>(
                model.getDocumentTypeFacets(getPrimaryType()));
        mixins.addAll(Arrays.asList(getMixinTypes()));
        return mixins;
    }

    /**
     * Checks the mixins. Includes mixins from the type.
     */
    public boolean hasMixinType(String mixin) {
        if (model.getDocumentTypeFacets(getPrimaryType()).contains(mixin)) {
            return true; // present in type
        }
        for (String m : getMixinTypes()) {
            if (m.equals(mixin)) {
                return true; // present in node
            }
        }
        return false;
    }

    /**
     * Clears the properties cache, used when removing mixins.
     */
    protected void clearCache() {
        // some properties have now become invalid
        propertyCache.clear();
    }

    // ----- properties -----

    /**
     * Gets a simple property from the node, given its name.
     *
     * @param name the property name
     * @return the property
     * @throws IllegalArgumentException if the name is invalid
     */
    public SimpleProperty getSimpleProperty(String name)
            throws StorageException {
        SimpleProperty property = (SimpleProperty) propertyCache.get(name);
        if (property == null) {
            ModelProperty propertyInfo = getPropertyInfo(name);
            if (propertyInfo == null) {
                throw new IllegalArgumentException("Unknown field: " + name);
            }
            property = makeSimpleProperty(name, propertyInfo);
            propertyCache.put(name, property);
        }
        return property;
    }

    protected SimpleProperty makeSimpleProperty(String name,
            ModelProperty propertyInfo) throws StorageException {
        String fragmentName = propertyInfo.fragmentName;
        Fragment fragment = fragments.get(fragmentName);
        if (fragment == null) {
            // lazy fragment, fetch from session
            RowId rowId = new RowId(fragmentName, getId());
            fragment = context.get(rowId, true);
            fragments.put(fragmentName, fragment);
        }
        return new SimpleProperty(name, propertyInfo.propertyType,
                propertyInfo.readonly, (SimpleFragment) fragment,
                propertyInfo.fragmentKey);
    }

    /**
     * Gets a collection property from the node, given its name.
     *
     * @param name the property name
     * @return the property
     * @throws IllegalArgumentException if the name is invalid
     */
    public CollectionProperty getCollectionProperty(String name)
            throws StorageException {
        CollectionProperty property = (CollectionProperty) propertyCache.get(name);
        if (property == null) {
            ModelProperty propertyInfo = getPropertyInfo(name);
            if (propertyInfo == null) {
                throw new IllegalArgumentException("Unknown field: " + name);
            }
            property = makeCollectionProperty(name, propertyInfo);
            propertyCache.put(name, property);
        }
        return property;
    }

    protected CollectionProperty makeCollectionProperty(String name,
            ModelProperty propertyInfo) throws StorageException {
        String fragmentName = propertyInfo.fragmentName;
        Fragment fragment = fragments.get(fragmentName);
        if (fragment == null) {
            // lazy fragment, fetch from session
            RowId rowId = new RowId(fragmentName, getId());
            fragment = context.get(rowId, true);
        }
        if (fragment instanceof CollectionFragment) {
            return new CollectionProperty(name, propertyInfo.propertyType,
                    propertyInfo.readonly, (CollectionFragment) fragment);
        } else {
            fragments.put(fragmentName, fragment);
            return new CollectionProperty(name, propertyInfo.propertyType,
                    propertyInfo.readonly, (SimpleFragment) fragment,
                    propertyInfo.fragmentKey);
        }
    }

    protected ModelProperty getPropertyInfo(String name) {
        // check primary type
        ModelProperty propertyInfo = model.getPropertyInfo(getPrimaryType(),
                name);
        if (propertyInfo != null) {
            return propertyInfo;
        }
        // check mixins
        for (String mixin : getMixinTypes()) {
            propertyInfo = model.getMixinPropertyInfo(mixin, name);
            if (propertyInfo != null) {
                return propertyInfo;
            }
        }
        // check proxy schemas
        if (isProxy()) {
            propertyInfo = model.getProxySchemasPropertyInfo(name);
            if (propertyInfo != null) {
                return propertyInfo;
            }
        }
        return null;
    }

    public void setSimpleProperty(String name, Serializable value)
            throws StorageException {
        SimpleProperty property = getSimpleProperty(name);
        property.setValue(value);
    }

    public void setCollectionProperty(String name, Serializable[] value)
            throws StorageException {
        CollectionProperty property = getCollectionProperty(name);
        property.setValue(value);
    }

    // ----- locking -----

    // ----- lifecycle -----

    // ----- versioning -----

    // ----- activities, baselines, configurations -----

    // ----- shared nodes -----

    // ----- retention -----

    /*
     * ----- equals/hashcode -----
     */

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof Node) {
            return equals((Node) other);
        }
        return false;
    }

    private boolean equals(Node other) {
        return getId() == other.getId();
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(uuid=" + getId() + ", name="
                + getName() + ", primaryType=" + getPrimaryType()
                + ", parentId=" + getParentId() + ")";
    }

}
