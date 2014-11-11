/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api;

import org.junit.BeforeClass;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import static org.nuxeo.ecm.core.api.Constants.CORE_BUNDLE;
import static org.nuxeo.ecm.core.api.Constants.CORE_FACADE_TESTS_BUNDLE;

/**
 * @author Florent Guillaume
 */
public class TestLocalAPIWithCustomVersioning extends TestLocalAPI {

    static {
        usingCustomVersioning = true;
    }

    @BeforeClass
    public static void startRuntime() throws Exception {
        runtime = new NXRuntimeTestCase() {};
        runtime.setUp();

        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/CoreService.xml");
        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/SecurityService.xml");
        runtime.deployContrib(CORE_BUNDLE, "OSGI-INF/RepositoryService.xml");

        runtime.deployBundle("org.nuxeo.ecm.core.event");

        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "TypeService.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "permissions-contrib.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "test-CoreExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "CoreTestExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "DemoRepository.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "LifeCycleService.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "LifeCycleServiceExtensions.xml");
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "DocumentAdapterService.xml");

        // Adding this one.
        runtime.deployContrib(CORE_FACADE_TESTS_BUNDLE, "CustomVersioningService.xml");
    }

}
