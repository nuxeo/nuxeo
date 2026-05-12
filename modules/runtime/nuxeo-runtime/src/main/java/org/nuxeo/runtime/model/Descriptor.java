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

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.MapUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Descriptors implementing this interface will automatically be registered within the default registry in
 * {@code DefaultComponent}.
 *
 * @since 10.3
 */
public interface Descriptor {

    String UNIQUE_DESCRIPTOR_ID = "";

    /**
     * The descriptor id, descriptors with same id are merged.
     * <p>
     * To forbid multiple descriptors use UNIQUE_DESCRIPTOR_ID.
     * <p>
     * To forbid merge use a unique value, non-overridden {@code toString()} for example.
     */
    String getId();

    /**
     * Returns the descriptor id to copy for the current descriptor.
     * <p>
     * The method returns null by default, this disables the copy mechanism.
     *
     * @return the descriptor id to copy
     * @since 2025.18
     */
    default String getCopyId() {
        return null;
    }

    /**
     * @param other the descriptor to copy, its id is the one returned by {@link #getCopyId()} of the current descriptor
     * @return a descriptor representing {@code other} copied into {@code this}
     * @since 2025.18
     * @implNote The default implementation delegates to {@link #merge(Descriptor)} by calling
     *           {@code other.merge(this)}. Since {@link #merge(Descriptor)} treats its argument as taking precedence
     *           over the receiver, this uses {@code other} as the base and overlays {@code this} on top. As a
     *           consequence, {@link #merge(Descriptor)} implementations must handle the id field (e.g. with
     *           {@code getIfNull}) so the returned descriptor retains {@code this}'s id rather than {@code other}'s.
     */
    default Descriptor copy(Descriptor other) {
        return other.merge(this);
    }

    /**
     * Returns a descriptor representing {@code other} merged into {@code this}
     * <p>
     * Default implementation returns {@code other}.
     *
     * @return the merged descriptor
     */
    default Descriptor merge(Descriptor other) {
        return other;
    }

    /**
     * During merge if a descriptor whose doesRemove() returns true is encountered, the merge chain is reset and started
     * again on next descriptor.
     * <p>
     * If the last descriptor of same id doesRemove() return true, the descriptor for this id will be {@code null}.
     */
    default boolean doesRemove() {
        return false;
    }

    /**
     * Merges two lists of {@link Descriptor}.
     *
     * @since 2025.18
     */
    @SuppressWarnings("unchecked")
    static <D extends Descriptor> List<D> merge(List<D> other, List<D> current) {
        var map = new LinkedHashMap<String, D>();
        emptyIfNull(current).forEach(descriptor -> map.put(descriptor.getId(), descriptor));
        emptyIfNull(other).forEach(descriptor -> map.merge(descriptor.getId(), descriptor, (v1, v2) -> {
            if (v2.doesRemove()) {
                return null;
            } else {
                return (D) v1.merge(v2);
            }
        }));
        return new ArrayList<>(map.values());
    }

    /**
     * Merges two maps of {@link Descriptor}.
     *
     * @since 2025.18
     */
    @SuppressWarnings("unchecked")
    static <D extends Descriptor> Map<String, D> merge(Map<String, D> other, Map<String, D> current) {
        var map = new LinkedHashMap<>(emptyIfNull(current));
        emptyIfNull(other).forEach((key, descriptor) -> map.merge(key, descriptor, (v1, v2) -> {
            if (v2.doesRemove()) {
                return null;
            } else {
                return (D) v1.merge(v2);
            }
        }));
        return map;
    }

    /**
     * In an equivalent way as {@code ObjectUtils.getIfNull} with empty support.
     *
     * @since 2025.20
     */
    static <O> List<O> getIfEmpty(List<O> other, List<O> current) {
        return emptyIfNull(other).isEmpty() ? current : other;
    }
}
