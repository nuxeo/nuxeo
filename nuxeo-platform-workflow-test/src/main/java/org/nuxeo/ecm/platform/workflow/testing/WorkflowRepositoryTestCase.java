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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: WorkflowRepositoryTestCase.java 29517 2008-01-22 12:41:23Z atchertchian $
 */

package org.nuxeo.ecm.platform.workflow.testing;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.jbpm.JbpmConfiguration;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;
import org.nuxeo.ecm.platform.workflow.impl.WAPIImpl;
import org.nuxeo.ecm.platform.workflow.jbpm.JbpmWorkflowExecutionContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Test case with repository and workflow service setup
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public abstract class WorkflowRepositoryTestCase extends RepositoryOSGITestCase {

    protected WAPI wapi;

    // test jbpm configuration, overriding the default one
    protected static JbpmConfiguration jbpmConfiguration;

    protected EntityManagerFactory emf;

    protected EntityManager em;

    private static MockWAPIService getMockWapiService() {
        return (MockWAPIService) Framework.getRuntime().getComponent(
                MockWAPIService.COMPONENT_NAME);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wapi = new WAPIImpl();
        // bundles
        deployBundle("nuxeo-platform-workflow-core");
        deployBundle("nuxeo-platform-workflow-jbpm");
        JbpmWorkflowExecutionContext.setConfigurationFile("jbpm.cfg.test.xml");
        jbpmConfiguration = JbpmWorkflowExecutionContext.getJbpmConfiguration();
        // contribute a mock component that will provide bean services on
        // the runtime layer for the test wapi instance
        deployBundle("nuxeo-platform-workflow-test");
        // set wapi on the mock wapi service
        MockWAPIService wapiServ = getMockWapiService();
        wapiServ.setWAPI(wapi);
        // override the jbpm engine configuration
        jbpmConfiguration.createSchema();
        createEntityManager();
    }

    @Override
    protected void tearDown() throws Exception {
        jbpmConfiguration.dropSchema();
        wapi = null;
        removeEntityManager();
        super.tearDown();
    }

    // db management

    public EntityManager createEntityManager() throws Exception {
        emf = Persistence.createEntityManagerFactory("NXWorkflowDocument");
        em = emf.createEntityManager();
        EntityTransaction et = em.getTransaction();
        if (!et.isActive()) {
            em.getTransaction().begin();
        }
        // set entity manager on the wapi service
        MockWAPIService wapiServ = getMockWapiService();
        wapiServ.setEntityManager(em);
        return em;
    }

    public void removeEntityManager() throws Exception {
        if (em != null) {
            // remove tests data
            EntityTransaction et = em.getTransaction();
            if (!et.isActive()) {
                em.getTransaction().begin();
            }
            em.clear();
            em.getTransaction().commit();
        }
        if (emf != null) {
            emf.close();
        }
    }

}
