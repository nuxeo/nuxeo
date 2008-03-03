/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import java.net.URL;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.TestRuntime;


/**
 *
 * @author <a href="mailto:dms@nuxeo.com">Dragos Mihalache</a>
 */
public class TestApiHeavyLoad_local extends TestApiHeavyLoad {

    protected TestRuntime runtime;

    @Override
    protected CoreSession getCoreSession() {
        return remote;
    }

    @Override
    public void setUp() throws Exception {
        runtime = new TestRuntime();
        Framework.initialize(runtime);

        deploy("EventService.xml");

        deploy("CoreService.xml");
        deploy("TypeService.xml");
        deploy("SecurityService.xml");
        deploy("RepositoryService.xml");
        deploy("test-CoreExtensions.xml");
        deploy("CoreTestExtensions.xml");
        deploy("DemoRepository.xml");
        deploy("LifeCycleService.xml");
        deploy("LifeCycleServiceExtensions.xml");
        deploy("CoreEventListenerService.xml");
        deploy("DocumentAdapterService.xml");

        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        Framework.shutdown();
    }

    public void deploy(String bundle) {
        URL url = getResource(bundle);
        assertNotNull("Test resource not found " + bundle, url);
        try {
            runtime.deploy(url);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to deploy bundle " + bundle);
        }
    }

    public void undeploy(String bundle) {
        URL url = getResource(bundle);
        assertNotNull("Test resource not found " + bundle, url);
        try {
            runtime.undeploy(url);
        } catch (Exception e) {
            fail("Failed to undeploy bundle " + bundle);
        }
    }

    public static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader()
            .getResource(resource);
    }

}
