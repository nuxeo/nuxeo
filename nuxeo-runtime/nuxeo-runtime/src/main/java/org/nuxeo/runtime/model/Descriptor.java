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

/**
 * Descriptors implementing this interface will automatically be registered within the default registry in
 * {@code DefaultComponent}.
 *
 * @since 10.3
 */
public interface Descriptor {

    public static final String UNIQUE_DESCRIPTOR_ID = "";

    /**
     * The descriptor id, descriptors with same id are merged.
     * <p>
     * To forbid multiple descriptors use UNIQUE_DESCRIPTOR_ID.
     * <p>
     * To forbid merge use a unique value, non-overriden {@code toString()} for exemple.
     */
    String getId();

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

}
