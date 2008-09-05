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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(Children.class);

    /**
     * The fragments we know about. This list is not ordered, and may not be
     * complete.
     */
    protected List<SimpleFragment> existing;

    /**
     * This is {@code true} when complete information about the children is
     * known.
     * <p>
     * This is the case when a query to the database has been made to fetch all
     * children, or when a new parent node with no children has been created.
     */
    protected boolean complete;

    public Children(boolean complete) {
        this.complete = complete;
    }

    protected List<SimpleFragment> getExisting() {
        if (existing == null) {
            existing = new LinkedList<SimpleFragment>();
        }
        return existing;
    }

    /**
     * Adds a known child.
     *
     * @param fragment the hierarchy fragment for the child
     * @param name the child name
     * @throws StorageException
     */
    // TODO unordered for now
    public void add(SimpleFragment fragment, String name)
            throws StorageException {
        getExisting().add(fragment);
    }

    /**
     * Add fragments actually read from the backend, and mark this complete.
     * <p>
     * Fragments already known are not erased, as they may be new.
     *
     * @param fragments the fragments (must be mutable)
     */
    public void addComplete(List<SimpleFragment> fragments) {
        assert !complete;
        complete = true;
        if (existing == null || existing.size() == 0) {
            existing = fragments; // don't copy, for efficiency
        } else {
            // assumes that the list of fragments already known is small
            // collect already know ids
            Set<Serializable> ids = new HashSet<Serializable>();
            for (SimpleFragment fragment : existing) {
                ids.add(fragment.getId());
            }
            // add complete set, note the ids we knew
            List<SimpleFragment> newExisting = new LinkedList<SimpleFragment>();
            for (SimpleFragment fragment : fragments) {
                ids.remove(fragment.getId());
                newExisting.add(fragment);
            }
            // finish with the created ones
            for (SimpleFragment fragment : existing) {
                if (ids.contains(fragment.getId())) {
                    newExisting.add(fragment);
                }
            }
            // replace existing
            existing = newExisting;
        }
    }

    /**
     * Invalidates children after some have been added.
     */
    public void invalidateAdded() {
        complete = false;
    }

    /**
     * Removes a known child.
     *
     * @param fragment the fragment to remove
     * @throws StorageException
     */
    public void remove(SimpleFragment fragment) throws StorageException {
        if (existing == null) {
            return;
        }
        if (!existing.remove(fragment)) {
            throw new StorageException("Nonexistent complex property: " +
                    fragment);
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
     * @param nameKey the key to use to filter by name
     * @return the fragment, or {@code null}, or {@link SimpleFragment#UNKNOWN}
     */
    public SimpleFragment get(String name, String nameKey) {
        // TODO optimize by removing WARN checks
        SimpleFragment found = null;
        if (existing != null) {
            for (SimpleFragment fragment : existing) {
                try {
                    if (name.equals(fragment.getString(nameKey))) {
                        if (found == null) {
                            found = fragment;
                            continue; // to check further WARN
                        } else {
                            log.warn("Get by name with several children: " +
                                    name);
                            break;
                        }
                    }
                } catch (StorageException e) {
                    // cannot happen, failed refetch
                    // pass
                }
            }
            if (found != null) {
                return found;
            }
        }
        return complete ? null : SimpleFragment.UNKNOWN;
    }

    /**
     * Gets all the fragments, for a complete list of children.
     *
     * @param name the name, or {@code null} for all children
     * @param nameKey the key to use to filter by name
     * @return all the fragments, or {@code null} if the list is not known to be
     *         complete
     */
    public List<SimpleFragment> getAll(String name, String nameKey) {
        if (!complete) {
            return null;
        }
        if (existing == null) {
            return Collections.emptyList();
        }
        if (name == null) {
            return existing;
        } else {
            // filter by name
            List<SimpleFragment> filtered = new LinkedList<SimpleFragment>();
            for (SimpleFragment fragment : existing) {
                try {
                    if (name.equals(fragment.getString(nameKey))) {
                        filtered.add(fragment);
                    }
                } catch (StorageException e) {
                    // cannot happen, failed refetch
                    // pass
                }
            }
            return filtered;
        }
    }
}
