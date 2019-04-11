/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generic {@link BaseDirectoryDescriptor} registry holding registered descriptors and instantiated {@link Directory}
 * objects.
 * <p>
 * The directory descriptors have two special boolean flags that control how merge works:
 * <ul>
 * <li>{@code remove="true"}: this removes the definition of the directory. The next definition (if any) will be done
 * from scratch.
 * <li>{@code template="true"}: this defines an abstract descriptor which cannot be directly instantiated as a
 * directory. However another descriptor can extend it through {@code extends="templatename"} to inherit all its
 * properties.
 * </ul>
 *
 * @since 8.2
 */
public class DirectoryRegistry {

    private static final Log log = LogFactory.getLog(DirectoryRegistry.class);

    /** All descriptors registered. */
    // used under synchronization
    protected Map<String, List<BaseDirectoryDescriptor>> allDescriptors = new HashMap<>();

    /** Effective descriptors. */
    // used under synchronization
    protected Map<String, BaseDirectoryDescriptor> descriptors = new HashMap<>();

    /** Effective instantiated directories. */
    // used under synchronization
    protected Map<String, Directory> directories = new HashMap<>();

    public synchronized void addContribution(BaseDirectoryDescriptor contrib) {
        String id = contrib.name;
        if (id.contains("/") && log.isWarnEnabled()) {
            log.warn("Directory " + id + " should not contain forward slashes in its name, as they are not supported."
                    + " Operations with the REST API on this directory won't work.");
        }
        log.info("Registered directory" + (contrib.template ? " template" : "") + ": " + id);
        allDescriptors.computeIfAbsent(id, k -> new ArrayList<>()).add(contrib);
        contributionChanged(contrib);
    }

    public synchronized void removeContribution(BaseDirectoryDescriptor contrib) {
        String id = contrib.name;
        log.info("Unregistered directory" + (contrib.template ? " template" : "") + ": " + id);
        allDescriptors.getOrDefault(id, Collections.emptyList()).remove(contrib);
        contributionChanged(contrib);
    }

    protected void contributionChanged(BaseDirectoryDescriptor contrib) {
        LinkedList<String> todo = new LinkedList<>();
        todo.add(contrib.name);
        Set<String> done = new HashSet<>();
        while (!todo.isEmpty()) {
            String id = todo.removeFirst();
            if (!done.add(id)) {
                // already done, avoid loops
                continue;
            }
            BaseDirectoryDescriptor desc = recomputeDescriptor(id);
            // recompute dependencies
            if (desc != null) {
                for (List<BaseDirectoryDescriptor> list : allDescriptors.values()) {
                    for (BaseDirectoryDescriptor d : list) {
                        if (id.equals(d.extendz)) {
                            todo.add(d.name);
                            break;
                        }
                    }
                }
            }
        }
    }

    protected void removeDirectory(String id) {
        Directory dir = directories.remove(id);
        if (dir != null) {
            shutdownDirectory(dir);
        }
    }

    /** Recomputes the effective descriptor for a directory id. */
    protected BaseDirectoryDescriptor recomputeDescriptor(String id) {
        removeDirectory(id);
        // compute effective descriptor
        List<BaseDirectoryDescriptor> list = allDescriptors.getOrDefault(id, Collections.emptyList());
        BaseDirectoryDescriptor contrib = null;
        for (BaseDirectoryDescriptor next : list) {
            String extendz = next.extendz;
            if (extendz != null) {
                // merge from base
                BaseDirectoryDescriptor base = descriptors.get(extendz);
                if (base != null && base.template) {
                    // merge generic base descriptor into specific one from the template
                    contrib = base.clone();
                    contrib.template = false;
                    contrib.name = next.name;
                    contrib.merge(next);
                } else {
                    log.debug("Directory " + id + " extends non-existing directory template: " + extendz);
                    contrib = null;
                }
            } else if (next.remove) {
                contrib = null;
            } else if (contrib == null) {
                // first descriptor or first one after a remove
                contrib = next.clone();
            } else if (contrib.getClass() == next.getClass()) {
                contrib.merge(next);
            } else {
                log.warn("Directory " + id + " redefined with different factory");
                contrib = next.clone();
            }
        }
        if (contrib == null) {
            descriptors.remove(id);
        } else {
            descriptors.put(id, contrib);
        }
        return contrib;
    }

    /**
     * Gets the effective directory descriptor with the given id.
     * <p>
     * Templates are not returned.
     *
     * @param id the directory id
     * @return the effective directory descriptor, or {@code null} if not found
     */
    public synchronized BaseDirectoryDescriptor getDirectoryDescriptor(String id) {
        BaseDirectoryDescriptor descriptor = descriptors.get(id);
        return descriptor.template ? null : descriptor;
    }

    /**
     * Gets the directory with the given id.
     *
     * @param id the directory id
     * @return the directory, or {@code null} if not found
     */
    public synchronized Directory getDirectory(String id) {
        Directory dir = directories.get(id);
        if (dir == null) {
            BaseDirectoryDescriptor descriptor = descriptors.get(id);
            if (descriptor != null && !descriptor.template) {
                dir = descriptor.newDirectory();
                directories.put(id, dir);
            }
        }
        return dir;
    }

    /**
     * Gets all the directory ids.
     *
     * @return the directory ids
     */
    public synchronized List<String> getDirectoryIds() {
        List<String> list = new ArrayList<>();
        for (BaseDirectoryDescriptor descriptor : descriptors.values()) {
            if (descriptor.template) {
                continue;
            }
            list.add(descriptor.name);
        }
        return list;
    }

    /**
     * Gets all the directories.
     *
     * @return the directories
     */
    public synchronized List<Directory> getDirectories() {
        List<Directory> list = new ArrayList<>();
        for (BaseDirectoryDescriptor descriptor : descriptors.values()) {
            if (descriptor.template) {
                continue;
            }
            list.add(getDirectory(descriptor.name));
        }
        return list;
    }

    /**
     * Shuts down all directories and clears the registry.
     */
    public synchronized void shutdown() {
        for (Directory dir : directories.values()) {
            shutdownDirectory(dir);
        }
        allDescriptors.clear();
        descriptors.clear();
        directories.clear();
    }

    /**
     * Shuts down the given directory and catches any {@link DirectoryException}.
     *
     * @param dir the directory
     */
    protected static void shutdownDirectory(Directory dir) {
        try {
            dir.shutdown();
        } catch (DirectoryException e) {
            log.error("Error while shutting down directory:" + dir.getName(), e);
        }
    }

}
