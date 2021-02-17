/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.runtime.model.registry;

import org.nuxeo.common.xmap.registry.MapRegistry;

/**
 * {@link MapRegistry} with additional runtime-related features.
 *
 * @since 11.5
 */
public class MapRuntimeRegistry extends AbstractRuntimeRegistry<MapRegistry> {

    public MapRuntimeRegistry(String component, String point, MapRegistry registry) {
        super(component, point, registry);
    }

    // TODO: add runtime message on warn log...

}
