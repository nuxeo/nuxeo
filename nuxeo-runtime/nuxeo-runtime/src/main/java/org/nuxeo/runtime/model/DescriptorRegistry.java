/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.runtime.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default generic descriptor registry.
 * <p>
 * It handles (un)registering and merged retrieval.
 * <p>
 * Merge algorithm depends on {@code Descriptor} implementations.
 * <p>
 *
 * @since 10.3
 */
@SuppressWarnings("unchecked")
public class DescriptorRegistry {

    private static final Logger log = LogManager.getLogger();

    // target -> xp -> id -> list of descriptors
    protected Map<String, Map<String, Map<String, List<Descriptor>>>> descriptors = new HashMap<>();

    public <T extends Descriptor> T getDescriptor(String target, String xp, String id) {
        return (T) merge(descriptors.getOrDefault(target, Collections.emptyMap())
                                    .getOrDefault(xp, Collections.emptyMap())
                                    .getOrDefault(id, Collections.emptyList()));

    }

    public <T extends Descriptor> List<T> getDescriptors(String target, String xp) {
        return (List<T>) descriptors.getOrDefault(target, Collections.emptyMap())
                                    .getOrDefault(xp, Collections.emptyMap())
                                    .values()
                                    .stream()
                                    .map(this::merge)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
    }

    public boolean register(String target, String xp, Descriptor descriptor) {
        log.debug("Register {} to {}/{}", descriptor.getId(), target, xp);
        return descriptors.computeIfAbsent(target, k -> new HashMap<>())
                          .computeIfAbsent(xp, k -> new LinkedHashMap<>())
                          .computeIfAbsent(descriptor.getId(), k -> new ArrayList<>())
                          .add(descriptor);

    }

    public boolean unregister(String target, String xp, Descriptor descriptor) {
        log.debug("Unregister {} from {}/{}", descriptor.getId(), target, xp);
        return descriptors.getOrDefault(target, Collections.emptyMap())
                          .getOrDefault(xp, Collections.emptyMap())
                          .getOrDefault(descriptor.getId(), Collections.emptyList())
                          .remove(descriptor);
    }

    protected <T extends Descriptor> T merge(Collection<T> descriptors) {
        T descriptor = null;
        for (T d : descriptors) {
            if (d.doesRemove()) {
                descriptor = null;
            } else {
                descriptor = descriptor == null ? d : (T) descriptor.merge(d);
            }
        }
        return descriptor;
    }

    public void clear() {
        descriptors.clear();
    }

}
