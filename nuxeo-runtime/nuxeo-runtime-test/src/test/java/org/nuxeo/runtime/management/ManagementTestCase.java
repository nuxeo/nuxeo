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
 *     matic
 */
package org.nuxeo.runtime.management;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author matic
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.management:OSGI-INF/management-server-locator-service.xml")
@Deploy("org.nuxeo.runtime.management:OSGI-INF/management-resource-publisher-service.xml")
@Deploy("org.nuxeo.runtime.test:isolated-server.xml")
public abstract class ManagementTestCase {

    protected ResourcePublisherService publisherService;

    protected ServerLocatorService locatorService;

    @Before
    public void setUp() throws Exception {
        locatorService = (ServerLocatorService) Framework.getService(ServerLocator.class);
        publisherService = (ResourcePublisherService) Framework.getService(ResourcePublisher.class);
    }

    /**
     * NXP-22534 This fix has to be discussed
     */
    @After
    public void after() {
        Framework.getRuntime().getComponentManager().reset();
        Framework.getRuntime().getComponentManager().start();
    }

    protected Set<ObjectName> doQuery(String name) {
        String qualifiedName = ObjectNameFactory.getQualifiedName(name);
        ObjectName objectName = ObjectNameFactory.getObjectName(qualifiedName);
        MBeanServer server = locatorService.lookupServer(objectName.getDomain());
        return server.queryNames(objectName, null);
    }

}
