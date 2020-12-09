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

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.common.Environment.DISTRIBUTION_HOTFIX;
import static org.nuxeo.common.Environment.DISTRIBUTION_NAME;
import static org.nuxeo.common.Environment.DISTRIBUTION_SERVER;
import static org.nuxeo.common.Environment.DISTRIBUTION_VERSION;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 11.5
 */
public class CapabilitiesServiceImpl extends DefaultComponent implements CapabilitiesService {

    public static final String CAPABILITY_SERVER = "server";

    protected final Map<String, Supplier<Map<String, Object>>> capabilitiesSuppliers = new HashMap<>();

    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
        new ComponentManager.Listener() {
            @Override
            public void beforeStart(ComponentManager mgr, boolean isResume) {
                capabilitiesSuppliers.clear();
            }
        }.install();
    }

    @Override
    public int getApplicationStartedOrder() {
        // very early as other services depend on us
        return ComponentStartOrders.CAPABILITIES;
    }

    @Override
    public void start(ComponentContext context) {
        registerCapabilities(CAPABILITY_SERVER, this::getServerCapabilities);
    }

    protected Map<String, Object> getServerCapabilities() {
        var serverCapabilities = new LinkedHashMap<String, Object>();
        serverCapabilities.put("distributionName", Framework.getProperty(DISTRIBUTION_NAME));
        serverCapabilities.put("distributionVersion", Framework.getProperty(DISTRIBUTION_VERSION));
        serverCapabilities.put("distributionServer", Framework.getProperty(DISTRIBUTION_SERVER));
        var hotfixVersion = Framework.getProperty(DISTRIBUTION_HOTFIX);
        if (isNotBlank(hotfixVersion)) {
            serverCapabilities.put("hotfixVersion", hotfixVersion);
        }
        return serverCapabilities;
    }

    // ------------------------
    // Service implementations
    // ------------------------

    @Override
    public void registerCapabilities(String name, Map<String, Object> map) {
        registerCapabilities(name, () -> map);
    }

    @Override
    public void registerCapabilities(String name, Supplier<Map<String, Object>> supplier) {
        capabilitiesSuppliers.put(name, supplier);
    }

    @Override
    public Capabilities getCapabilities() {
        return capabilitiesSuppliers.entrySet()
                                    .stream()
                                    .collect(collectingAndThen(toMap(Map.Entry::getKey, e -> e.getValue().get()),
                                            Capabilities::new));
    }
}
