/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id:TestSearchEnginePluginRegistration.java 13121 2007-03-01 18:07:58Z janguenot $
 */

package org.nuxeo.ecm.core.search.backend.compass;

import org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.ecm.core.search.backend.testing.SharedTestDataBuilder;
import org.nuxeo.ecm.core.search.threading.IndexingThreadPool;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestMultiThreadingFSDir extends NXRuntimeTestCase {

    protected SearchService service;

    static protected final String ENGINE_NAME = "compass";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        // deploy("ServiceManagement.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "LoginComponent.xml");

        deployContrib("org.nuxeo.runtime", "OSGI-INF/EventService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "CoreService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "SecurityService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "RepositoryService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "test-CoreExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "LifeCycleService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "LifeCycleServiceExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "CoreEventListenerService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "PlatformService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "DefaultPlatform.xml");

        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "nxtransform-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "nxtransform-platform-contrib.xml");

        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-framework.xml");
        service = SearchServiceDelegate.getRemoteSearchService();
        assertNotNull(service);
        deployContrib("org.nuxeo.ecm.platform.search.test",
                "nxsearch-backendtest-contrib.xml");
        assertEquals("barcode",
                getSearchServiceInternals().getIndexableDataConfFor(
                        "bk:barcode").getIndexingName());

        // Deploy filesystem based configuration
        deployContrib("org.nuxeo.ecm.platform.search.compass-plugin.tests",
                "nxsearch-" + ENGINE_NAME + "-test-fs-contrib.xml");
    }

    private SearchServiceInternals getSearchServiceInternals() {
        return (SearchServiceInternals) service;
    }

    public SearchEngineBackend getBackend() {
        return getSearchServiceInternals().getSearchEngineBackendByName(
                ENGINE_NAME);
    }

    public void testMultiThreading() throws Exception {
        ResolvedResources resources = SharedTestDataBuilder.makeAboutLifeAggregated();
        IndexingThreadPool.setSearchService(service);

        final int NB_DOCS = 102;

        SimpleTimer timer = new SimpleTimer();
        timer.start();

        for (int i = 0; i < NB_DOCS; i++) {
            System.out.println("Launchin' a new indexing task :: => " + i);
            //IndexingThreadPool.index(resources);
            service.indexInThread(resources);
        }

        while (IndexingThreadPool.getActiveIndexingTasks() > 0) {
            System.out.println("Tasks done="
                    + IndexingThreadPool.getTotalCompletedIndexingTasks()
                    + " "
                    + timer.getSpent("in"));
            Thread.sleep(300);
        }

        getSearchServiceInternals().saveAllSessions();

        timer.stop();
        System.out.println(timer);
        System.out.println((NB_DOCS * 1000) / timer.getDuration() + " resources/sec");

        System.out.println("DONE............................... !");
    }

}
