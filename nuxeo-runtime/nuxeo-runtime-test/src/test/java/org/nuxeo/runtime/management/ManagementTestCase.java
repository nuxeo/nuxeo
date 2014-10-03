/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.runtime.management;

import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.After;
import org.junit.Before;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author matic
 *
 */
public abstract class ManagementTestCase extends NXRuntimeTestCase {

    protected static final String OSGI_BUNDLE_NAME = "org.nuxeo.runtime.management";


    protected ResourcePublisherService publisherService;

    protected ServerLocatorService locatorService;


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployContrib(OSGI_BUNDLE_NAME,
                "OSGI-INF/management-server-locator-service.xml");
        deployContrib(OSGI_BUNDLE_NAME,
                "OSGI-INF/management-resource-publisher-service.xml");
        deployTestContrib(OSGI_BUNDLE_NAME,
                "isolated-server.xml");

        fireFrameworkStarted();

        locatorService = (ServerLocatorService) Framework.getLocalService(ServerLocator.class);
        publisherService = (ResourcePublisherService) Framework.getLocalService(ResourcePublisher.class);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        Framework.getRuntime().stop();
        super.tearDown();
    }

    protected void doBindResources() throws InstanceNotFoundException,
            ReflectionException, MBeanException {
        String qualifiedName = ObjectNameFactory.formatQualifiedName(ResourcePublisherService.NAME);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        MBeanServer server = locatorService.lookupServer(objectName.getDomain());
        server.invoke(objectName, "bindResources", null, null);
    }

    protected Set<ObjectName> doQuery(String name) {
        String qualifiedName = ObjectNameFactory.getQualifiedName(name);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        MBeanServer server = locatorService.lookupServer(objectName.getDomain());
        return server.queryNames(objectName, null);
    }

}
