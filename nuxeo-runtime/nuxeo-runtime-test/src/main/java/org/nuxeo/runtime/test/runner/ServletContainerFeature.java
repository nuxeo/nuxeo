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
import java.lang.reflect.Field;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.server.ServerComponent;

import sun.net.www.http.HttpClient;

/**
 * Runs an embedded servlet container.
 * <p>
 * Note that at initialization the feature disables the {@code retryPostProp} property of
 * {@link sun.net.www.http.HttpClient}, the underlying HTTP client used by {@link com.sun.jersey.api.client.Client}.
 * <p>
 * This is to prevent the JDK's default behavior kept for backward compatibility: an unsuccessful HTTP POST request is
 * automatically resent to the server, unsuccessful in this case meaning the server did not send a valid HTTP response
 * or an {@code IOException} occurred. Yet in the tests using the Jersey client to make calls to Nuxeo we don't want
 * this as it can hide errors occurring in the HTTP communication that should prevent an appropriate response from being
 * sent by the server.
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
        disableSunHttpClientRetryPostProp();

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

    /**
     * Prevents the JDK's default behavior of resending an unsuccessful HTTP POST request automatically to the server by
     * disabling the the {@code retryPostProp} property of {@link sun.net.www.http.HttpClient}.
     * <p>
     * This can also be achieved by setting the {@code sun.net.http.retryPost} system property to {@code false}.
     *
     * @since 9.3
     */
    public static void disableSunHttpClientRetryPostProp()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = HttpClient.class.getDeclaredField("retryPostProp");
        field.setAccessible(true);
        field.setBoolean(null, false);
    }

}
