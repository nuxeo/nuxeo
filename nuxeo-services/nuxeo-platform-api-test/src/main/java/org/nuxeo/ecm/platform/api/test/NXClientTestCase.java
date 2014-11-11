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

package org.nuxeo.ecm.platform.api.test;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

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
public abstract class NXClientTestCase extends TestCase {

    protected static RuntimeService runtime;

    private static final Log log = LogFactory.getLog(NXClientTestCase.class);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        initializeRT();
    }

    @Override
    protected void tearDown() throws Exception {
        shutdownRT();
        super.tearDown();
    }

    /**
     * Subclasses may override this method to create a repository at a specific
     * location.
     *
     * @return null
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
