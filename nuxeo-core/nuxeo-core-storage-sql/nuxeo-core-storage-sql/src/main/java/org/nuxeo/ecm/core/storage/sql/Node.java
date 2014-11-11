/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.Map;

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
    private final FragmentsMap fragments;

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
     */
    @SuppressWarnings("unchecked")
    protected Node(PersistenceContext context, FragmentGroup fragmentGroup)
            throws StorageException {
        this.context = context;
        model = context.model;
        hierFragment = fragmentGroup.hier;
        if (fragmentGroup.fragments == null) {
            fragments = new FragmentsMap();
        } else {
            fragments = fragmentGroup.fragments;
        }
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

    public String getParentId() {
        try {
            return getHierFragment().getString(model.HIER_PARENT_KEY);
        } catch (StorageException e) {
            // do not propagate this unlikely exception as a checked one
            throw new RuntimeException(e);
        }
    }

    protected SimpleFragment getHierFragment() {
        return hierFragment;
    }

    // cache the isVersion computation
    public boolean isVersion() {
        if (isVersion == null) {
            Serializable versionableId;
            try {
                versionableId = getSimpleProperty(
                        model.VERSION_VERSIONABLE_PROP).getValue();
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
            isVersion = Boolean.valueOf(versionableId != null);
        }
        return isVersion.booleanValue();
    }

    public boolean isProxy() {
        return getPrimaryType().equals(model.PROXY_TYPE);
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
            ModelProperty propertyInfo = model.getPropertyInfo(
                    getPrimaryType(), name);
            if (propertyInfo == null) {
                throw new IllegalArgumentException("Unknown field: " + name);
            }
            String fragmentName = propertyInfo.fragmentName;
            Fragment fragment = fragments.get(fragmentName);
            if (fragment == null) {
                // lazy fragment, fetch from session
                RowId rowId = new RowId(fragmentName, getId());
                fragment = context.get(rowId, true);
                fragments.put(fragmentName, fragment);
            }
            property = new SimpleProperty(name, propertyInfo.propertyType,
                    propertyInfo.readonly, (SimpleFragment) fragment,
                    propertyInfo.fragmentKey);
            propertyCache.put(name, property);
        }
        return property;
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
            ModelProperty propertyInfo = model.getPropertyInfo(
                    getPrimaryType(), name);
            if (propertyInfo == null) {
                throw new IllegalArgumentException("Unknown field: " + name);
            }
            String fragmentName = propertyInfo.fragmentName;
            RowId rowId = new RowId(fragmentName, getId());
            Fragment fragment = context.get(rowId, true);
            property = new CollectionProperty(name, propertyInfo.propertyType,
                    false, (CollectionFragment) fragment);
            propertyCache.put(name, property);
        }
        return property;
    }

    public BaseProperty getProperty(String name) throws StorageException {
        BaseProperty property = propertyCache.get(name);
        if (property != null) {
            return property;
        }
        ModelProperty propertyInfo = model.getPropertyInfo(getPrimaryType(),
                name);
        if (propertyInfo == null) {
            throw new IllegalArgumentException("Unknown field: " + name);
        }
        if (propertyInfo.propertyType.isArray()) {
            return getCollectionProperty(name);
        } else {
            return getSimpleProperty(name);
        }
    }

    public void setSingleProperty(String name, Serializable value)
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

}
