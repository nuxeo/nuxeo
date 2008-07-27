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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@code Node} implementation. The actual data is stored in contained objects
 * that are {@link Fragment}s.
 *
 * @author Florent Guillaume
 */
public class Node {

    /**
     * The "core" type. This is kept in the node so that when a SQLDocument or a
     * complex property is created for it we are able to get proper core type
     * information for the document and its properties.
     */
    protected final Type type;

    /** The persistence context used. */
    protected final PersistenceContext context;

    protected final Model model;

    /** The main row. */
    protected final SimpleFragment mainFragment;

    /** The hierarchy row, if applicable. */
    protected final SimpleFragment hierFragment;

    /** Fragment information for each additional mixin or inherited row. */
    protected final FragmentsMap fragments;

    /**
     * Cache of property objects already retrieved. They are dumb objects, just
     * providing an indirection to an underlying {@link Fragment}.
     *
     * TODO make this a memory-sensitive cache.
     */
    protected transient final Map<String, BaseProperty> propertyCache;

    /**
     * Creates a Node.
     *
     * @param type the node type
     * @param session the session
     * @param context the persistence context
     * @param rowGroup the group of rows for the node
     */
    protected Node(Type type, Session session,
            PersistenceContext context, FragmentGroup rowGroup) {
        this.type = type;
        this.context = context;
        model = session.getModel();
        mainFragment = rowGroup.main;
        hierFragment = rowGroup.hier; // may be null
        if (rowGroup.fragments == null) {
            fragments = new FragmentsMap();
        } else {
            fragments = rowGroup.fragments;
        }
        this.propertyCache = new HashMap<String, BaseProperty>();
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
        return hierFragment.getString(model.HIER_CHILD_NAME_KEY);
    }

    public Type getType() {
        return type;
    }

    // ----- modification -----

    /**
     * Removes this node. Called by the session.
     *
     * @throws StorageException
     */
    protected void remove() throws StorageException {
        context.remove(mainFragment);
        context.remove(hierFragment);
        for (Fragment fragment : fragments.values()) {
            context.remove(fragment);
            // XXX TODO must remove all fragments, even unfetched ones
        }
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
        if (name == null || name.contains("/") || name.equals(".") ||
                name.equals("..")) {
            // XXX real parsing
            throw new StorageException("Illegal name: " + name);
        }

        // XXX namespace transformations

        SimpleProperty property = (SimpleProperty) propertyCache.get(name);
        if (property == null) {
            PropertyType propType;
            Fragment row;
            String fragmentKey;
            boolean readonly;
            if (model.MAIN_PRIMARY_TYPE_PROP.equals(name)) {
                propType = PropertyType.STRING;
                fragmentKey = model.MAIN_PRIMARY_TYPE_KEY;
                row = mainFragment;
                readonly = true;
            } else {
                propType = model.getPropertyType(name);
                if (propType == null) {
                    throw new IllegalArgumentException("Unknown field: " + name);
                }
                String tableName = model.getFragmentName(name);
                fragmentKey = model.getFragmentKey(name);
                // localName = field.getName().getLocalName();
                // String schemaName = field.getDeclaringType().getName();
                row = fragments.get(tableName);
                if (row == null) {
                    // lazy fragment, fetch from context
                    row = context.get(tableName, getId(), true);
                    fragments.put(tableName, row);
                }
                readonly = false;
            }
            property = new SimpleProperty(name, propType, readonly,
                    (SimpleFragment) row, fragmentKey);
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
        if (name == null || name.contains("/") || name.equals(".") ||
                name.equals("..")) {
            // XXX real parsing
            throw new StorageException("Illegal name: " + name);
        }

        // XXX namespace transformations

        CollectionProperty property = (CollectionProperty) propertyCache.get(name);
        if (property == null) {
            PropertyType propType = model.getPropertyType(name);
            if (propType == null) {
                throw new IllegalArgumentException("Unknown field: " + name);
            }
            String tableName = model.getFragmentName(name);
            Fragment fragment = context.get(tableName, getId(), true);
            property = new CollectionProperty(name, propType, false,
                    (CollectionFragment) fragment);
            propertyCache.put(name, property);
        }
        return property;
    }

    public BaseProperty getProperty(String name) throws StorageException {
        BaseProperty property = propertyCache.get(name);
        if (property != null) {
            return property;
        }
        PropertyType propType = model.getPropertyType(name);
        if (propType == null) {
            throw new IllegalArgumentException("Unknown field: " + name);
        }
        if (propType.isArray()) {
            return getCollectionProperty(name);
        } else {
            return getSimpleProperty(name);
        }
    }

    public void setSingleProperty(String name, Serializable value)
            throws StorageException {
        SimpleProperty property = (SimpleProperty) getSimpleProperty(name);
        property.setValue(value);
    }

    public void setCollectionProperty(String name, Serializable[] value)
            throws StorageException {
        CollectionProperty property = (CollectionProperty) getCollectionProperty(name);
        property.setValue(value);
    }

    // ----- locking -----

    // ----- lifecycle -----

    // ----- versioning -----

    // ----- activities, baselines, configurations -----

    // ----- shared nodes -----

    // ----- retention -----

}
