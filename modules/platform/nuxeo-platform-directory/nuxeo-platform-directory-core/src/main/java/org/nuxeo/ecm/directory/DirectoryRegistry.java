/*
 * (C) Copyright 2012-2021 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.runtime.RuntimeMessage;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeMessage.Source;
import org.nuxeo.runtime.api.Framework;

/**
 * Generic {@link BaseDirectoryDescriptor} registry holding registered descriptors and instantiated {@link Directory}
 * objects.
 * <p>
 * The directory descriptors has a special boolean flag that control how merge works:
 * <ul>
 * <li>{@code template="true"}: this defines an abstract descriptor which cannot be directly instantiated as a
 * directory. However another descriptor can extend it through {@code extends="templatename"} to inherit all its
 * properties.
 * </ul>
 * <p>
 * Modified as of 11.5 to implement {@link Registry}.
 *
 * @since 8.2
 */
public class DirectoryRegistry extends MapRegistry {

    private static final Logger log = LogManager.getLogger(DirectoryRegistry.class);

    /** Effective computed descriptors. */
    protected Map<String, BaseDirectoryDescriptor> descriptors = new ConcurrentHashMap<>();

    /** Effective instantiated directories. */
    protected Map<String, Directory> directories = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        shutdown();
        directories.clear();
        descriptors.clear();
        super.initialize();
        // compute effective descriptors for all contributions
        for (String id : getContributions().keySet()) {
            BaseDirectoryDescriptor desc = computeFinalDescriptor(id, new HashSet<>());
            if (desc != null && !desc.template) {
                descriptors.put(id, desc);
            }
        }
    }

    protected BaseDirectoryDescriptor computeFinalDescriptor(String id, Set<String> done) {
        BaseDirectoryDescriptor desc = getBareDirectoryDescriptor(id);
        if (done.contains(id)) {
            // avoid loops
            return desc;
        }
        done.add(id);
        if (desc == null) {
            return null;
        }
        String extendz = desc.extendz;
        if (extendz == null) {
            return desc;
        } else {
            // merge from base
            BaseDirectoryDescriptor base = computeFinalDescriptor(extendz, done);
            if (base == null || !base.template) {
                String message = String.format("Directory '%s' extends non-existing directory template: '%s'", id,
                        extendz);
                log.error(message);
                Framework.getRuntime()
                         .getMessageHandler()
                         .addMessage(new RuntimeMessage(Level.ERROR, message, Source.COMPONENT,
                                 DirectoryServiceImpl.COMPONENT_NAME));
                return null;
            } else {
                // merge generic base descriptor into specific one from the template
                BaseDirectoryDescriptor finalDesc = base.clone();
                finalDesc.template = false;
                finalDesc.name = id;
                finalDesc.merge(desc);
                return finalDesc;
            }
        }
    }

    /**
     * Gets the effective directory descriptor with the given id.
     * <p>
     * Templates are not returned.
     *
     * @param id the directory id
     * @return the effective directory descriptor, or {@code null} if a template or not found
     */
    public BaseDirectoryDescriptor getDirectoryDescriptor(String id) {
        checkInitialized();
        BaseDirectoryDescriptor descriptor = descriptors.get(id);
        if (descriptor == null) {
            return null;
        }
        return descriptor.template ? null : descriptor;
    }

    /**
     * Gets the directory with the given id.
     *
     * @param id the directory id
     * @return the directory, or {@code null} if a template or not found
     */
    public Directory getDirectory(String id) {
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
     * Returns all the directory ids.
     * <p>
     * Templates are not returned.
     */
    public List<String> getDirectoryIds() {
        return descriptors.values()
                          .stream()
                          .filter(desc -> !desc.template)
                          .map(desc -> desc.name)
                          .collect(Collectors.toList());
    }

    /**
     * Gets all the directories.
     *
     * @return the directories
     */
    public List<Directory> getDirectories() {
        return descriptors.values()
                          .stream()
                          .filter(desc -> !desc.template)
                          .map(desc -> getDirectory(desc.name))
                          .collect(Collectors.toList());
    }

    /**
     * Shuts down all computed directories and catches any {@link DirectoryException}.
     */
    public synchronized void shutdown() {
        for (Directory dir : directories.values()) {
            try {
                dir.shutdown();
            } catch (DirectoryException e) {
                log.error("Error while shutting down directory: {}", dir.getName(), e);
            }
        }
    }

    protected BaseDirectoryDescriptor getBareDirectoryDescriptor(String name) {
        return this.<DirectoryContributor> getContribution(name)
                   .map(c -> getTargetRegistry(c.target, c.point))
                   .flatMap(r -> r.<BaseDirectoryDescriptor> getContribution(name))
                   .orElse(null);
    }

    protected MapRegistry getTargetRegistry(String component, String point) {
        return Framework.getRuntime()
                        .getComponentManager()
                        .<MapRegistry> getExtensionPointRegistry(component, point)
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("Unknown registry for extension point '%s--%s'", component, point)));
    }

}
