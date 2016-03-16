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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Generic {@link BaseDirectoryDescriptor} registry holding registered descriptors and instantiated {@link Directory}
 * objects.
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
        log.info("Registered directory: " + id);
        allDescriptors.computeIfAbsent(id, k -> new ArrayList<>()).add(contrib);
        recomputeDescriptors(id);
    }

    public synchronized void removeContribution(BaseDirectoryDescriptor contrib) {
        String id = contrib.name;
        log.info("Unregistered directory: " + id);
        allDescriptors.getOrDefault(id, Collections.emptyList()).remove(contrib);
        recomputeDescriptors(id);
    }

    /** Recomputes the effective descriptor for a directory id. */
    protected void recomputeDescriptors(String id) {
        Directory dir = directories.remove(id);
        if (dir != null) {
            shutdownDirectory(dir);
        }
        // compute effective descriptor
        List<BaseDirectoryDescriptor> list = allDescriptors.getOrDefault(id, Collections.emptyList());
        BaseDirectoryDescriptor contrib = null;
        for (BaseDirectoryDescriptor next : list) {
            if (next.remove) {
                contrib = null;
            } else {
                if (contrib == null) {
                    contrib = next.clone();
                } else {
                    if (contrib.getClass() == next.getClass()) {
                        // if same factory then merge
                        contrib.merge(next);
                    } else {
                        // else no possible merge, just use the new contrib
                        contrib = next.clone();
                    }
                }
            }
        }
        descriptors.put(id, contrib);
    }

    /**
     * Gets the effective directory descriptor with the given id.
     *
     * @param id the directory id
     * @return the effective directory descriptor, or {@code null} if not found
     */
    public synchronized BaseDirectoryDescriptor getDirectoryDescriptor(String id) {
        return descriptors.get(id);
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
            if (descriptor != null) {
                dir = descriptor.newDirectory();
                directories.put(id,  dir);
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
        return new ArrayList<>(descriptors.keySet());
    }

    /**
     * Gets all the directories.
     *
     * @return the directories
     */
    public synchronized List<Directory> getDirectories() {
        List<Directory> list = new ArrayList<>();
        for (String id : descriptors.keySet()) {
            list.add(getDirectory(id));
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
