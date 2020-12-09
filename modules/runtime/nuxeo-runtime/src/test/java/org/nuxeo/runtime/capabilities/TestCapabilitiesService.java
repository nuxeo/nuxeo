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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * @since 11.5
 */
public class TestCapabilitiesService {

    @Test
    public void testRegistration() {
        var service = new CapabilitiesServiceImpl();
        service.registerCapabilities("testRegistration", Map.of("key", "value"));

        assertCapabilities(service.getCapabilities(), "value");
    }

    @Test
    public void testRegistrationDynamic() {
        var counter = new AtomicInteger();

        var service = new CapabilitiesServiceImpl();
        service.registerCapabilities("testRegistration", () -> Map.of("key", counter.getAndIncrement()));

        assertCapabilities(service.getCapabilities(), 0);
        assertCapabilities(service.getCapabilities(), 1);
        assertCapabilities(service.getCapabilities(), 2);
    }

    protected void assertCapabilities(Capabilities capabilities, Object expectedKeyValue) {
        assertNotNull(capabilities);
        var testRegistration = capabilities.get("testRegistration");
        assertNotNull(testRegistration);
        assertEquals(1, testRegistration.size());
        assertEquals(expectedKeyValue, testRegistration.get("key"));
    }
}
