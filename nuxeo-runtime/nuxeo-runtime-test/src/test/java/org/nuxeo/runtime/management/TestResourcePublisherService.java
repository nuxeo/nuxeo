/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;

public class TestResourcePublisherService extends ManagementTestCase {

    @Test
    public void testRegisteredService() throws Exception {
        assertNotNull(Framework.getService(ResourcePublisher.class));
    }

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
        int size = shortcutsName.size();
        deployTestContrib(OSGI_BUNDLE_NAME, "management-tests-service.xml");
        deployTestContrib(OSGI_BUNDLE_NAME, "management-tests-contrib.xml");

        publisherService.bindResources();
        String qualifiedName = ObjectNameFactory.formatTypeQuery("service");

        Set<ObjectName> registeredNames = doQuery(qualifiedName);
        assertNotNull(registeredNames);
        assertEquals(4, registeredNames.size());

        shortcutsName = publisherService.getShortcutsName();
        assertNotNull(shortcutsName);
        assertEquals(size + 4, shortcutsName.size());
        assertTrue(shortcutsName.contains("dummy"));
    }

}
