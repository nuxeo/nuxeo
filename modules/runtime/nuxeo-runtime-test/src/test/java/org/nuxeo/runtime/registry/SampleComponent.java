/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.runtime.registry;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.common.xmap.registry.SingleRegistry;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 11.5
 */
public class SampleComponent extends DefaultComponent {

    public static final String SINGLE_XP = "single";

    public static final String MAP_XP = "map";

    public static final String CUSTOM_XP = "custom";

    public static final String LEGACY_XP = "legacy";

    protected boolean registryContributionRegistered;

    protected boolean registryContributionUnregistered;

    protected Map<String, SampleLegacyDescriptor> legacyRegistry = new HashMap<>();

    public SingleRegistry getSingleRegistry() {
        return getExtensionPointRegistry(SINGLE_XP);
    }

    public MapRegistry getMapRegistry() {
        return getExtensionPointRegistry(MAP_XP);
    }

    public SampleRegistry getCustomRegistry() {
        return getExtensionPointRegistry(CUSTOM_XP);
    }

    public MapRegistry getLegacyRegistry() {
        return getExtensionPointRegistry(LEGACY_XP);
    }

    @Override
    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        if (LEGACY_XP.equals(xp)) {
            SampleLegacyDescriptor desc = (SampleLegacyDescriptor) contribution;
            legacyRegistry.put(desc.name, desc);
        } else {
            registryContributionRegistered = true;
        }
    }

    // not called in tests anyway
    @Override
    public void unregisterContribution(Object contribution, String xp, ComponentInstance component) {
        if (LEGACY_XP.equals(xp)) {
            SampleLegacyDescriptor desc = (SampleLegacyDescriptor) contribution;
            legacyRegistry.remove(desc.name);
        } else {
            registryContributionUnregistered = true;
        }
    }

}
