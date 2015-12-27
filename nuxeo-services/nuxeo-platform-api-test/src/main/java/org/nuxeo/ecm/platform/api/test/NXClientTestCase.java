/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.test;

import java.io.File;
import java.net.URL;

import org.junit.Before;
import org.junit.After;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.util.SimpleRuntime;

/**
 * Base class for remote unit testing.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public abstract class NXClientTestCase {

    protected static RuntimeService runtime;

    private static final Log log = LogFactory.getLog(NXClientTestCase.class);

    @Before
    protected void setUp() throws Exception {
        initializeRT();
    }

    @After
    protected void tearDown() throws Exception {
        shutdownRT();
    }

    /**
     * Subclasses may override this method to create a repository at a specific location.
     */
    protected File getHomeDir() {
        return null;
    }

    private void initializeRT() throws Exception {
        final File home = getHomeDir();
        runtime = new SimpleRuntime(home);
        Framework.initialize(runtime);
        deployAll();
    }

    private static void shutdownRT() throws Exception {
        Framework.shutdown();
    }

    protected void deploy(String bundle) {
        URL url = getResource(bundle);
        if (null == url) {
            log.error("cannot deploy bundle: " + bundle + ". not found");
            Thread.dumpStack();
            return;
        }
        try {
            Framework.getRuntime().getContext().deploy(url);
        } catch (Exception e) {
            log.error("cannot deploy bundle: " + bundle, e);
        }
    }

    protected void undeploy(String bundle) {
        URL url = getResource(bundle);
        assert url != null;
        try {
            Framework.getRuntime().getContext().undeploy(url);
        } catch (Exception e) {
            log.error("cannot undeploy bundle: " + bundle, e);
        }
    }

    protected URL getResource(String resource) {
        return runtime.getContext().getResource(resource);
    }

    protected void deployAll() {
        // deploy("RemotingService.xml");
        deploy("EventService.xml");
    }

}
