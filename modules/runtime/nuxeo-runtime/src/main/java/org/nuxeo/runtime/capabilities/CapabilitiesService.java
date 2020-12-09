/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime.capabilities;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Service holding the server capabilities.
 *
 * @since 11.5
 */
public interface CapabilitiesService {

    /**
     * Registers capabilities under the given {@code name}.
     *
     * @see #registerCapabilities(String, Supplier)
     */
    void registerCapabilities(String name, Map<String, Object> map);

    /**
     * Registers capabilities under the given {@code name}.
     * <p>
     * The given {@link Supplier} is called each time {@link #getCapabilities()} is called.
     * <p>
     * Capabilities might be registered during the Component start step.
     */
    void registerCapabilities(String name, Supplier<Map<String, Object>> supplier);

    /**
     * Returns the capabilities.
     */
    Capabilities getCapabilities();

}
