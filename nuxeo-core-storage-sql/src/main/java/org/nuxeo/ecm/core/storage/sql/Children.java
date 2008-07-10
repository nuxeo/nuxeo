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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    /**
     * This is {@code true} when complete information about the children is
     * known.
     * <p>
     * This is the case when a query to the database has been made to fetch all
     * children, or when a new parent node with no children has been created.
     */
    private boolean completeRegular;

    private Map<String, SimpleFragment> existingRegular;

    private Collection<SimpleFragment> allRegular;

    private Set<String> missingRegular;

    private boolean completeProperties;

    private Map<String, SimpleFragment> existingProperties;

    private Collection<SimpleFragment> allProperties;

    private Set<String> missingProperties;

    public Children(boolean complete) {
        this.completeRegular = complete;
        this.completeProperties = complete;
    }

    private void newExistingRegular() {
        existingRegular = new HashMap<String, SimpleFragment>();
    }

    private void newExistingProperties() {
        existingProperties = new HashMap<String, SimpleFragment>();
    }

    private void newMissingRegular() {
        missingRegular = new HashSet<String>();
    }

    private void newMissingProperties() {
        missingProperties = new HashSet<String>();
    }

    /**
     * Adds a known child.
     *
     * @param fragment the hierarchy fragment for the child
     * @param name the child name
     * @param pos the child position
     * @param model the model
     * @throws StorageException
     */
    // TODO unordered for now
    public void add(SimpleFragment fragment, String name, Long pos, Model model)
            throws StorageException {
        if (model.HIER_CHILD_POS_COMPLEX.equals(pos)) {
            if (missingProperties != null) {
                missingProperties.remove(name);
            }
            if (existingProperties == null) {
                newExistingProperties();
            } else {
                if (existingProperties.containsKey(name)) {
                    throw new StorageException(
                            "Duplicate complex property name: " + name);
                }
            }
            existingProperties.put(name, fragment);
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
     * @param model
     */
    public void addComplete(Collection<SimpleFragment> fragments, Model model) {
        assert !complete;
        if (existing == null) {
            newExisting();
        }
        String name_key = model.HIER_CHILD_NAME_KEY;
        for (SimpleFragment fragment : fragments) {
            existing.put(fragment.getString(name_key), fragment);
        }
        missing = null; // could check coherence with existing
        complete = true;
    }

    /**
     * Removes a known child.
     *
     * @param name the child name
     * @param pos the pos
     * @param model the model
     * @throws StorageException
     */
    public void remove(String name, Long pos, Model model)
            throws StorageException {
        if (model.HIER_CHILD_POS_COMPLEX.equals(pos)) {
            if (existingProperties != null) {
                if (existingProperties.remove(name) == null) {
                    throw new StorageException(
                            "Nonexistent complex property: " + name);
                }
            }
            if (missingProperties == null) {
                newMissingProperties();
            } else {
                if (missingProperties.contains(name)) {
                    throw new StorageException(
                            "Already missing complex property: " + name);
                }
            }
            missingProperties.add(name);
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
     * @param complex {@code true} if a complex property is searched, {@code
     *            false} otherwise
     * @return the fragment, or {@code null}, or {@link SimpleFragment#UNKNOWN}
     */
    public SimpleFragment get(String name, boolean complex) {
        if (complex) {
            if (completeProperties) {
                return existingProperties == null ? null
                        : existingProperties.get(name);
            }
            if (existingProperties != null) {
                SimpleFragment fragment = existingProperties.get(name);
                if (fragment != null) {
                    return fragment;
                }
            }
            return missingProperties != null &&
                    missingProperties.contains(name) ? null
                    : SimpleFragment.UNKNOWN;
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
     * @param complex whether to get complex properties or real children, or
     *            both
     * @return all the fragments, or {@code null}
     */
    public Collection<SimpleFragment> getFragments(Boolean complex) {
        if (!complete) {
            return null;
        }
        XXX(complex);
        return existing.values();
    }
}
