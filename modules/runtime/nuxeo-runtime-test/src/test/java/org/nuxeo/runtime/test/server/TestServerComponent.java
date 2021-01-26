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
package org.nuxeo.runtime.test.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.server.FilterDescriptor;
import org.nuxeo.runtime.server.ServerComponent;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

/**
 * Test {@link ServerComponent} extension points.
 *
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(ServletContainerFeature.class)
public class TestServerComponent {

    protected static final String COMPONENT_NAME = "org.nuxeo.runtime.server";

    protected static final Map<String, String> INIT_PARAMS = Map.of("fontsize", "14", "foo", "bar");

    protected static final Map<String, String> INIT_PARAMS2 = Map.of("fontsize", "14");

    @Inject
    protected ServerComponent service;

    protected MapRegistry getRegistry(String point) {
        return (MapRegistry) Framework.getRuntime()
                                      .getComponentManager()
                                      .getExtensionPointRegistry(COMPONENT_NAME, point)
                                      .orElseThrow(() -> new IllegalArgumentException("registry not found"));
    }

    protected void checkSampleFilter(FilterDescriptor filter) {
        assertEquals("DummyFilter", filter.getName());
        assertEquals("/", filter.getContext());
        assertEquals(INIT_PARAMS, filter.getInitParams());
    }

    protected void checkSampleFilter2(FilterDescriptor filter) {
        assertEquals("DummyFilter", filter.getName());
        assertEquals("/", filter.getContext());
        assertEquals(INIT_PARAMS2, filter.getInitParams());
    }

    protected void checkSampleFilterCompat(FilterDescriptor filter) {
        assertEquals("DummyFilterWithCompatInitParams", filter.getName());
        assertEquals("/", filter.getContext());
        assertEquals(INIT_PARAMS, filter.getInitParams());
    }

    @Test
    @Deploy("org.nuxeo.runtime.server:test-server-filter-contrib.xml")
    public void testFilterContrib() {
        List<FilterDescriptor> filters = getRegistry(ServerComponent.XP_FILTER).getContributionValues();
        assertNotNull(filters);
        assertEquals(2, filters.size());

        checkSampleFilter(filters.get(0));
        checkSampleFilterCompat(filters.get(1));
    }

    @Test
    @Deploy("org.nuxeo.runtime.server:test-server-filter-contrib.xml")
    @Deploy("org.nuxeo.runtime.server:test-server-filter-contrib2.xml")
    public void testFilterContribOverride() {
        List<FilterDescriptor> filters = getRegistry(ServerComponent.XP_FILTER).getContributionValues();
        assertNotNull(filters);
        assertEquals(2, filters.size());

        checkSampleFilter2(filters.get(0));
        checkSampleFilterCompat(filters.get(1));
    }

}
