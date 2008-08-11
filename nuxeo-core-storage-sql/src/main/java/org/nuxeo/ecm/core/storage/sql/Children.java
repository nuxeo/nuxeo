/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * Holds information about the children of a given parent node.
 * <p>
 * There are two kinds of children, the "regular" ones, which correspond to
 * child documents and have unique names, and the "properties", which correspond
 * to complex properties and may have non-unique names (which allows for lists
 * of complex properties).
 * <p>
 * Information about children may be complete, or just partial if only a few
 * individual children have been retrieved.
 * <p>
 * This class is not thread-safe and should be used only from a single-threaded
 * session.
 * <p>
 * TODO unordered for now
 *
 * @author Florent Guillaume
 */
public class Children {

    private static final Log log = LogFactory.getLog(Children.class);

    /**
     * This is {@code true} when complete information about the children is
     * known.
     * <p>
     * This is the case when a query to the database has been made to fetch all
     * children, or when a new parent node with no children has been created.
     */
    private boolean completeRegular;

    private Map<String, SimpleFragment> existingRegular;

    private Set<String> missingRegular;

    private boolean completeProperties;

    private Map<String, List<SimpleFragment>> existingProperties;

    public Children(boolean complete) {
        this.completeRegular = complete;
        this.completeProperties = complete;
    }

    private void newExistingRegular() {
        existingRegular = new HashMap<String, SimpleFragment>();
    }

    private void newExistingProperties() {
        existingProperties = new HashMap<String, List<SimpleFragment>>();
    }

    private void newMissingRegular() {
        missingRegular = new HashSet<String>();
    }

    /**
     * Adds a known child.
     *
     * @param fragment the hierarchy fragment for the child
     * @param name the child name
     * @param complexProp whether the child is a complex property or a regular
     *            child
     * @throws StorageException
     */
    // TODO unordered for now
    public void add(SimpleFragment fragment, String name, boolean complexProp)
            throws StorageException {
        if (complexProp) {
            if (existingProperties == null) {
                newExistingProperties();
            }
            List<SimpleFragment> list = existingProperties.get(name);
            if (list == null) {
                list = new LinkedList<SimpleFragment>();
                existingProperties.put(name, list);
            }
            list.add(fragment);
        } else {
            if (missingRegular != null) {
                missingRegular.remove(name);
            }
            if (existingRegular == null) {
                newExistingRegular();
            } else {
                if (existingRegular.containsKey(name)) {
                    throw new StorageException("Duplicate child name: " + name);
                }
            }
            existingRegular.put(name, fragment);
        }
    }

    /**
     * Add fragments actually read from the backend, and mark this complete.
     * <p>
     * Fragments already known are not erased, as they may be new.
     *
     * @param fragments
     * @param complexProp whether the children are complex properties or regular
     *            children
     * @param model
     */
    public void addComplete(Collection<SimpleFragment> fragments,
            boolean complexProp, Model model) {
        String name_key = model.HIER_CHILD_NAME_KEY;
        if (complexProp) {
            assert !completeProperties;
            if (existingProperties == null) {
                newExistingProperties();
            }
            for (SimpleFragment fragment : fragments) {
                String name;
                try {
                    name = fragment.getString(name_key);
                } catch (StorageException e) {
                    // cannot happen, row is pristine
                    name = "ACCESSFAILED";
                }
                List<SimpleFragment> list = existingProperties.get(name);
                if (list == null) {
                    list = new LinkedList<SimpleFragment>();
                    existingProperties.put(name, list);
                }
                list.add(fragment);
            }
            completeProperties = true;
        } else {
            assert !completeRegular;
            if (existingRegular == null) {
                newExistingRegular();
            }
            for (SimpleFragment fragment : fragments) {
                String name;
                try {
                    name = fragment.getString(name_key);
                } catch (StorageException e) {
                    // cannot happen, row is pristine
                    name = "ACCESSFAILED";
                }
                existingRegular.put(name, fragment);
            }
            missingRegular = null; // could check coherence with existing
            completeRegular = true;
        }
    }

    /**
     * Removes a known child.
     *
     * @param fragment the fragment to remove
     * @param complexProp whether the child is a complex property or a regular
     *            child
     * @param model the model
     * @throws StorageException
     */
    public void remove(SimpleFragment fragment, boolean complexProp, Model model)
            throws StorageException {
        String name;
        try {
            name = fragment.getString(model.HIER_CHILD_NAME_KEY);
        } catch (StorageException e) {
            // cannot happen, row is pristine
            name = "ACCESSFAILED";
        }
        if (complexProp) {
            if (existingProperties != null) {
                List<SimpleFragment> list = existingProperties.get(name);
                if (list == null) {
                    throw new StorageException(
                            "Nonexistent complex property: " + name);
                }
                if (!list.remove(fragment)) {
                    throw new StorageException(
                            "Nonexistent complex property: " + fragment);
                }
            }
        } else {
            if (existingRegular != null) {
                if (existingRegular.remove(name) == null) {
                    throw new StorageException("Nonexistent child: " + name);
                }
            }
            if (missingRegular == null) {
                newMissingRegular();
            } else {
                if (missingRegular.contains(name)) {
                    throw new StorageException("Already missing child: " + name);
                }
            }
            missingRegular.add(name);
        }
    }

    /**
     * Gets a fragment by its name.
     * <p>
     * Returns {@code null} if there is no such child.
     * <p>
     * Returns {@link SimpleFragment#UNKNOWN} if there's no info about it.
     *
     * @param name the name
     * @param complexProp {@code true} if a complex property is searched,
     *            {@code false} for a regular child
     * @return the fragment, or {@code null}, or {@link SimpleFragment#UNKNOWN}
     */
    public SimpleFragment get(String name, boolean complexProp) {
        if (complexProp) {
            if (existingProperties == null) {
                return completeProperties ? null : SimpleFragment.UNKNOWN;
            }
            List<SimpleFragment> list = existingProperties.get(name);
            if (list == null || list.size() == 0) {
                return completeProperties ? null : SimpleFragment.UNKNOWN;
            }
            if (list.size() > 1) {
                log.warn("Get by name with several children: " + name);
            }
            return list.get(0);
        } else {
            if (completeRegular) {
                return existingRegular == null ? null
                        : existingRegular.get(name);
            }
            if (existingRegular != null) {
                SimpleFragment fragment = existingRegular.get(name);
                if (fragment != null) {
                    return fragment;
                }
            }
            return missingRegular != null && missingRegular.contains(name) ? null
                    : SimpleFragment.UNKNOWN;
        }
    }

    /**
     * Gets all the fragments, for a complete list of children.
     * <p>
     * If the list is not complete, returns {@code null}.
     *
     * @param name the name of the children, or {@code null} for all
     * @param complexProp {@code true} for complex properties, {@code false} for
     *            regular children
     * @return all the fragments, or {@code null}
     */
    public Collection<SimpleFragment> getFragments(String name,
            boolean complexProp) {
        if (complexProp) {
            if (!completeProperties) {
                return null;
            }
            if (existingProperties == null) {
                return Collections.emptyList();
            }
            if (name == null) {
                List<SimpleFragment> fragments = new LinkedList<SimpleFragment>();
                for (List<SimpleFragment> list : existingProperties.values()) {
                    fragments.addAll(list);
                }
                return fragments;
            } else {
                List<SimpleFragment> list = existingProperties.get(name);
                return list == null ? Collections.<SimpleFragment> emptyList()
                        : list;
            }
        } else {
            if (!completeRegular) {
                return null;
            }
            return existingRegular == null ? Collections.<SimpleFragment> emptyList()
                    : existingRegular.values();
        }
    }
}
