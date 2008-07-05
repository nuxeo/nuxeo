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
    private boolean complete;

    private Map<String, SimpleFragment> existing;

    private Set<String> missing;

    public Children(boolean complete) {
        this.complete = complete;
    }

    public Children(Collection<SimpleFragment> fragments, Model model) {
        addComplete(fragments, model);
    }

    private void newExisting() {
        existing = new HashMap<String, SimpleFragment>();
    }

    private void newMissing() {
        missing = new HashSet<String>();
    }

    public boolean isComplete() {
        return complete;
    }

    /**
     * Adds a known child.
     *
     * @param fragment the hierarchy fragment for the child
     * @param name the child name
     * @param pos the child position
     * @throws StorageException
     */
    // TODO unordered for now
    public void add(SimpleFragment fragment, String name, Long pos)
            throws StorageException {
        if (missing != null) {
            missing.remove(name);
        }
        if (existing == null) {
            newExisting();
        } else {
            if (existing.containsKey(name)) {
                throw new StorageException("Duplicate name: " + name);
            }
        }
        existing.put(name, fragment);
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
     * @throws StorageException
     */
    public void remove(String name) throws StorageException {
        if (existing != null) {
            if (existing.remove(name) == null) {
                throw new StorageException("Not existing: " + name);
            }
        }
        if (missing == null) {
            newMissing();
        } else {
            if (missing.contains(name)) {
                throw new StorageException("Already missing: " + name);
            }
        }
        missing.add(name);
    }

    /**
     * Gets a fragment by its name.
     * <p>
     * Returns {@code null} if there is no such child.
     * <p>
     * Returns {@link SimpleFragment#UNKNOWN} if there's no info about it.
     *
     * @param name
     * @return the fragment, or {@code null}, or {@link SimpleFragment#UNKNOWN}
     */
    public SimpleFragment get(String name) {
        if (complete) {
            return existing == null ? null : existing.get(name);
        }
        if (existing != null) {
            SimpleFragment fragment = existing.get(name);
            if (fragment != null) {
                return fragment;
            }
        }
        return missing != null && missing.contains(name) ? null
                : SimpleFragment.UNKNOWN;
    }

    /**
     * Gets all the fragments, for a complete list of children.
     *
     * @return all the fragments
     */
    public Collection<SimpleFragment> getFragments() {
        assert complete;
        return existing.values();
    }
}
