/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.server;

import org.nuxeo.common.server.WebApplication;

/**
 * Configuration interface for an embedded server implementation.
 *
 * @since 10.2
 */
public interface ServerConfigurator {

    /**
     * Initializes this server configurator
     *
     * @param port the HTTP port to use
     * @return the actual port used
     */
    int initialize(int port);

    /**
     * Closes this server configurator and releases resources.
     */
    void close();

    /**
     * Starts the server.
     */
    void start();

    /**
     * Stops the server.
     */
    void stop();

    /**
     * Adds a web app to the server.
     *
     * @param descriptor the descriptor
     */
    void addWepApp(WebApplication descriptor);

    /**
     * Adds a filter to the server.
     *
     * @param descriptor the descriptor
     */
    void addFilter(FilterDescriptor descriptor);

    /**
     * Adds a servlet to the server.
     *
     * @param descriptor the descriptor
     */
    void addServlet(ServletDescriptor descriptor);

    /**
     * Adds a lifecycle listener to the server.
     *
     * @param descriptor the descriptor
     */
    void addLifecycleListener(ServletContextListenerDescriptor descriptor);

}
