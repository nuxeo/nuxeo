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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import java.net.URL;

import org.nuxeo.ecm.platform.workflow.NXWorkflow;
import org.nuxeo.ecm.platform.workflow.service.WorkflowService;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.TestRuntime;

/**
 * Abstract JbpmRuntime test case.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractJbpmRuntimeTestCase extends AbstractJbmTestCase {

    protected static final String ENGINE_NAME = "jbpm";

    protected static final String ACTION_HANDLER_NAME = "jbpmActionHandler";

    protected WorkflowService workflowService;

    protected RuntimeService runtime;

    public URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }

    public void deploy(String bundle) {
        URL url = getResource(bundle);
        assertNotNull("Test resource not found " + bundle, url);
        try {
            Framework.getRuntime().getContext().deploy(url);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to deploy bundle " + bundle);
        }
    }

    public void undeploy(String bundle) {
        URL url = getResource(bundle);
        assertNotNull("Test resource not found " + bundle, url);
        try {
            Framework.getRuntime().getContext().undeploy(url);
        } catch (Exception e) {
            fail("Failed to undeploy bundle " + bundle);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        runtime = new TestRuntime();
        Framework.initialize(runtime);
        deploy("EventService.xml");

        deploy("WorkflowService.xml");
        deploy("WorkflowEngine.xml");
        deploy("WorkflowDefinitions.xml");
        deploy("WorkflowActionHandlers.xml");

        // Actions
        FakeJbpmWorkflowActionHandler.isExecuted = false;

        workflowService = NXWorkflow.getWorkflowService();
        assertNotNull(workflowService);
    }

    @Override
    public void tearDown() throws Exception {
        Framework.shutdown();
        super.tearDown();
    }

}
