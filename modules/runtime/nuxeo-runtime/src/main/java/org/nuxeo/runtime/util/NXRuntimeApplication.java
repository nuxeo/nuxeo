/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */
package org.nuxeo.runtime.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class NXRuntimeApplication {

    protected static RuntimeService runtime;

    private static final Log log = LogFactory.getLog(NXRuntimeApplication.class);

    protected final File home;

    protected NXRuntimeApplication(File home) {
        this.home = home;
    }

    protected NXRuntimeApplication() {
        this(null);
    }

    public void start() throws InterruptedException {
        start(new String[0]);
    }

    public void start(String[] args) throws InterruptedException {
        initialize(args);
        run();
        shutdown();
    }

    public void initialize(String[] args) {
        runtime = new SimpleRuntime(home);
        Framework.initialize(runtime);
        deployAll();
    }

    public void shutdown() throws InterruptedException {
        Framework.shutdown();
    }

    public void deploy(String bundle) {
        URL url = getResource(bundle);
        // could be more than core design flaw: assert url != null;
        if (url == null) {
            log.error("Cannot locate resource for deploying bundle " + bundle);
            return;
        }
        try {
            Framework.getRuntime().getContext().deploy(url);
        } catch (IOException e) {
            throw new RuntimeServiceException("Cannot deploy: " + url, e);
        }
    }

    public void undeploy(String bundle) {
        URL url = getResource(bundle);
        assert url != null;
        try {
            Framework.getRuntime().getContext().undeploy(url);
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    public URL getResource(String resource) {
        return runtime.getContext().getResource(resource);
    }

    protected void deployAll() {
        // deploy("RemotingService.xml");
        deploy("EventService.xml");
    }

    protected abstract void run();

}
