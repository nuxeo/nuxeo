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
package org.nuxeo.webengine.management.adapters;

import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementServiceImpl;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourceFactoryDescriptor;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author matic
 * 
 */
public class TestRuntimeMbeanAdapterFactory extends RepositoryOSGITestCase {

    public void setUp() throws Exception {
        super.setUp();

        deployContrib(ManagementServiceImpl.NAME.getName(),
                "OSGI-INF/management-contrib.xml");

        managementService = (ManagementServiceImpl) Framework.getRuntime().getComponent(
                ManagementServiceImpl.NAME);
    }

    protected ManagementServiceImpl managementService;

    @Override
    public void tearDown() throws Exception {
        Framework.getRuntime().stop();
        super.tearDown();
    }

    public void testRegisterFactories() throws Exception {
        doRegisterInventoryFactory();
        doRegisterRepositorySessionMetricFactory();
        Thread.sleep(1000000);
    }

    public void doRegisterInventoryFactory() throws Exception {
        ResourceFactoryDescriptor factoryDescriptor = new ResourceFactoryDescriptor(
                RuntimeInventoryMBeanAdapterFactory.class);
        managementService.registerContribution(factoryDescriptor, "factories",
                null);
    }
    
    public void doRegisterRepositorySessionMetricFactory() throws Exception {
        ResourceFactoryDescriptor factoryDescriptor = new ResourceFactoryDescriptor(
                RepositorySessionMetricMBeanAdapterFactory.class);
        managementService.registerContribution(factoryDescriptor, "factories",
                null);
    }

}
