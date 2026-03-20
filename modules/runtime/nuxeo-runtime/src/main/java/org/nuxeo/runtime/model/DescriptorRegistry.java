/*
 * (C) Copyright 2018-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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

    private static final Logger log = LogManager.getLogger(DescriptorRegistry.class);

    // target -> xp -> id -> list of descriptors
    protected Map<String, Map<String, Map<String, List<Descriptor>>>> descriptors = new HashMap<>();

    public <T extends Descriptor> T getDescriptor(String target, String xp, String id) {
        Function<String, T> descriptorFetcher = //
                descriptorId -> (T) merge(descriptors.getOrDefault(target, Map.of())
                                                     .getOrDefault(xp, Map.of())
                                                     .getOrDefault(descriptorId, List.of()));
        T descriptor = descriptorFetcher.apply(id);
        return copy(target, xp, descriptor, descriptorFetcher);
    }

    public <T extends Descriptor> List<T> getDescriptors(String target, String xp) {
        Map<String, T> descriptors = this.descriptors.getOrDefault(target, Map.of())
                                                     .getOrDefault(xp, Map.of())
                                                     .values()
                                                     .stream()
                                                     .map(this::merge)
                                                     .map(descriptor -> (T) descriptor)
                                                     .filter(Objects::nonNull)
                                                     .collect(toMap(Descriptor::getId, Function.identity(),
                                                             (v1, v2) -> v1, LinkedHashMap::new));
        return descriptors.values()
                          .stream()
                          .map(descriptor -> copy(target, xp, descriptor, descriptors::get))
                          // deprecated since 2021.x, some code path made modification to the returned list
                          // use Collectors.toList for that, on deprecation removal switch the call to toList
                          .collect(toList());
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
        return descriptors.getOrDefault(target, Map.of())
                          .getOrDefault(xp, Map.of())
                          .getOrDefault(descriptor.getId(), List.of())
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

    protected <T extends Descriptor> T copy(String target, String xp, T descriptor, Function<String, T> getDescriptor) {
        if (descriptor == null || descriptor.getCopyId() == null) {
            log.trace("Descriptor: {} on {}/{} does not copy any descriptor",
                    () -> descriptor == null ? "null" : descriptor.getId(), () -> target, () -> xp);
            return descriptor;
        } else if (Objects.equals(descriptor.getId(), descriptor.getCopyId())) {
            log.warn("Descriptor: {} on {}/{} is copying itself", descriptor.getId(), target, xp);
            return descriptor;
        } else {
            T toCopyDescriptor = getDescriptor.apply(descriptor.getCopyId());
            if (toCopyDescriptor == null) {
                log.warn("Descriptor: {} on {}/{} copy the descriptor: {} which does not exist", descriptor.getId(),
                        target, xp, descriptor.getCopyId());
                return descriptor;
            } else {
                log.trace("Descriptor: {} on {}/{} is copying descriptor: {}", descriptor.getId(), target, xp,
                        descriptor.getCopyId());
                return (T) descriptor.copy(toCopyDescriptor);
            }
        }
    }

    public void clear() {
        descriptors.clear();
    }

}
