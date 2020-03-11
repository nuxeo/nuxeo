/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.server.ServerComponent;

/**
 * Runs an embedded servlet container.
 */
@Deploy("org.nuxeo.runtime.server")
@Features(RuntimeFeature.class)
public class ServletContainerFeature implements RunnerFeature {

    private static final Logger log = LogManager.getLogger(ServletContainerFeature.class);

    protected static final int RETRIES = 1000;

    protected int port;

    @SuppressWarnings("deprecation")
    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        ServletContainer conf = runner.getConfig(ServletContainer.class);
        int port = conf == null ? 0 : conf.port();
        if (port <= 0) {
            port = findFreePort();
        }
        this.port = port;
        System.setProperty(ServerComponent.PORT_SYSTEM_PROP, String.valueOf(port));
    }

    protected int findFreePort() {
        for (int i = 0; i < RETRIES; i++) {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            } catch (IOException e) {
                log.trace("Failed to allocate port", e);
            }
        }
        throw new RuntimeException("Unable to find free port after " + RETRIES + " retries");
    }

    /**
     * Returns the port allocated for this servlet container.
     *
     * @since 10.10
     */
    public int getPort() {
        return port;
    }

}
