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

package org.nuxeo.ecm.platform.cache.server;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.cache.CacheService;
import org.nuxeo.ecm.platform.cache.CacheServiceException;
import org.nuxeo.ecm.platform.cache.CacheServiceFactory;
import org.nuxeo.ecm.platform.cache.CacheConfiguration.Config;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.TestRuntime;

/**
 * Abstract class initializing a single cache to be used for local
 * caching tests.
 *
 * @author DM
 *
 */
public abstract class TestServerCacheBase extends TestCase {

    private static final Log log = LogFactory.getLog(TestServerCacheBase.class);

    protected CacheService cache;

    protected CoreSession coreSession;

    // Nuxeo core init (runtime + jcr rep)
    private TestRuntime runtime;

    private CoreInstance server;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        initNXCore();

        // Register a CacheConfigurationFactory first
        Class.forName(CacheConfigurationFactoryImpl.class.getName());

        // initialize cache system
        cache = CacheServiceFactory.getCacheService(Config.CFG_REPL_SYNC);
        //cache.initCache(Config.CFG_REPL_SYNC);
    }

    @Override
    protected void tearDown() throws Exception {
        // close the core session
        server.close(coreSession);

        if (runtime != null) {
            Framework.shutdown();
        }

        if (cache != null) {
            cache.stopService();
        }

        super.tearDown();
    }

    protected void startCacheService() throws CacheServiceException {

        // at this point if no peer service is being detected
        // an internal exception is traced out - but not thrown
        // and the thread continue (unwisely?) ...
        cache.startService();
    }

    private void initNXCore() throws Exception {
        // initialize the Nuxeo Runtime system - needed for
        // local repository
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

        //coreSession = server.open("demo", null);
        //assertNotNull(coreSession);
        openCoreSession();

        log.info("NXCore Session created.");
        log.info("====================================================");

        server = CoreInstance.getInstance();
        assertNotNull(server);
    }

    /*
     * Test method for 'org.nuxeo.ecm.core.api.CoreInstance.open(String, Map<String,
     * Object>)'
     */
    private void openCoreSession() throws ClientException {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        coreSession = CoreInstance.getInstance().open("demo", ctx);

        assertNotNull(coreSession);
    }

    private static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(
                resource);
    }

    private void deploy(String bundle) {
        URL url = getResource(bundle);
        assertNotNull("Test resource not found " + bundle, url);
        try {
            runtime.deploy(url);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to deploy bundle " + bundle);
        }
    }

}
