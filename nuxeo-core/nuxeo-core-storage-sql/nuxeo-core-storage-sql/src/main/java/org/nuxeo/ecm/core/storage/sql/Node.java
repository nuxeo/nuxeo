/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.core.storage.sql.Model.PropertyInfo;

/**
 * A {@code Node} implementation. The actual data is stored in contained objects
 * that are {@link Fragment}s.
 *
 * @author Florent Guillaume
 */
public class Node {

    /** The persistence context used. */
    private final PersistenceContext context;

    private final Model model;

    /** The main row. */
    protected final SimpleFragment mainFragment;

    /** The hierarchy row, if applicable. */
    private final SimpleFragment hierFragment;

    /** Fragment information for each additional mixin or inherited row. */
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
     * @param session the session
     * @param context the persistence context
     * @param rowGroup the group of rows for the node
     * @throws StorageException
     */
    @SuppressWarnings("unchecked")
    protected Node(Session session, PersistenceContext context,
            FragmentGroup rowGroup) throws StorageException {
        this.context = context;
        model = session.getModel();
        mainFragment = rowGroup.main;
        hierFragment = rowGroup.hier; // may be null
        if (rowGroup.fragments == null) {
            fragments = new FragmentsMap();
        } else {
            fragments = rowGroup.fragments;
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
        return mainFragment.getId();
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
            return mainFragment.getString(model.MAIN_PRIMARY_TYPE_KEY);
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
        return hierFragment == null ? mainFragment : hierFragment;
    }

    // cache the isVersion computation
    public boolean isVersion() {
        if (isVersion == null) {
            SimpleFragment hier = getHierFragment();
            Serializable parentId;
            String name;
            try {
                parentId = hier.get(model.HIER_PARENT_KEY);
                name = hier.getString(model.HIER_CHILD_NAME_KEY);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
            isVersion = Boolean.valueOf(parentId == null && name != null
                    && name.length() > 0);
        }
        return isVersion.booleanValue();
    }

    public boolean isProxy() {
        return getPrimaryType().equals(model.PROXY_TYPE);
    }

    // ----- modification -----

    /**
     * Removes this node. Called by the session.
     *
     * @throws StorageException
     */
    protected void remove() throws StorageException {
        context.remove(getId());
    }

    // ----- properties -----

    /**
     * Gets a simple property from the node, given its name.
     *
     * @param name the property name
     * @return the property
     * @throws IllegalArgumentException if the name is invalid
     * @throws StorageException
     */
    public SimpleProperty getSimpleProperty(String name)
            throws StorageException {
        SimpleProperty property = (SimpleProperty) propertyCache.get(name);
        if (property == null) {
            PropertyInfo propertyInfo = model.getPropertyInfo(getPrimaryType(),
                    name);
            if (propertyInfo == null) {
                throw new IllegalArgumentException("Unknown field: " + name);
            }
            String fragmentName = propertyInfo.fragmentName;
            Fragment row = fragments.get(fragmentName);
            if (row == null) {
                // lazy fragment, fetch from context
                row = context.get(fragmentName, getId(), true);
                fragments.put(fragmentName, row);
            }
            property = new SimpleProperty(name, propertyInfo.propertyType,
                    propertyInfo.readonly, (SimpleFragment) row,
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
     * @throws StorageException
     */
    public CollectionProperty getCollectionProperty(String name)
            throws StorageException {
        CollectionProperty property = (CollectionProperty) propertyCache.get(name);
        if (property == null) {
            PropertyInfo propertyInfo = model.getPropertyInfo(getPrimaryType(),
                    name);
            if (propertyInfo == null) {
                throw new IllegalArgumentException("Unknown field: " + name);
            }
            String fragmentName = propertyInfo.fragmentName;
            Fragment fragment = context.get(fragmentName, getId(), true);
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
        PropertyInfo propertyInfo = model.getPropertyInfo(getPrimaryType(),
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
