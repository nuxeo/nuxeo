/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.runtime.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.runtime.test.runner.HotDeployer;

public class TestResourcePublisherService extends ManagementTestCase {

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    public void testRegisterResource() {
        publisherService.registerResource("dummy", "org.nuxeo:name=dummy", DummyMBean.class, new DummyService());
        publisherService.bindResources();
        Set<ObjectName> registeredNames = doQuery("org.nuxeo:name=dummy");
        assertNotNull(registeredNames);
        assertEquals(1, registeredNames.size());
    }

    @Test
    @Ignore
    public void testRegisterFactory() throws Exception {
        ResourceFactoryDescriptor descriptor = new ResourceFactoryDescriptor(DummyFactory.class);
        publisherService.registerContribution(descriptor, "factories", null);
        Set<ObjectName> registeredNames = doQuery("org.nuxeo:name=dummy");
        assertNotNull(registeredNames);
        assertEquals(registeredNames.size(), 1);
    }

    @Test
    public void testServerLocator() throws Exception {
        MBeanServer testServer = MBeanServerFactory.createMBeanServer("test");
        ObjectName testName = new ObjectName("test:test=test");
        publisherService.bindForTest(testServer, testName, new DummyService(), DummyMBean.class);
        publisherService.bindResources();
        locatorService.registerLocator("test", true);
        MBeanServer locatedServer = locatorService.lookupServer(testName);
        assertNotNull(locatedServer);
        assertTrue(locatedServer.isRegistered(testName));
    }

    @Test
    public void testXMLConfiguration() throws Exception {
        Set<String> shortcutsName = publisherService.getShortcutsName();
        int initialSize = shortcutsName.size();

        hotDeployer.deploy("org.nuxeo.runtime.test.tests:management-tests-service.xml",
                "org.nuxeo.runtime.test.tests:management-tests-contrib.xml");
        setUp();

        publisherService.bindResources();

        assertEquals(4, doQuery(ObjectNameFactory.formatTypeQuery("service")).size());

        shortcutsName = publisherService.getShortcutsName();
        assertEquals(initialSize + 4, shortcutsName.size());
        assertTrue(shortcutsName.contains("dummy"));
    }

}
