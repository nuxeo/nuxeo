/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.elasticsearch.api;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.nuxeo.elasticsearch.config.ElasticSearchRemoteConfig;

/**
 * This service enables the initialization of the Elasticsearch transport client and his settings
 * 
 * @since 9.1
 */
public interface ESClientInitializationService {

    /**
     * Initialize Elasticsearch client settings
     * 
     * @param config the cluster configuration
     * @return the client settings
     */
    Settings initializeSettings(ElasticSearchRemoteConfig config);

    /**
     * Initialize Elasticsearch client
     * 
     * @param settings the client settings
     * @return the client
     */
    TransportClient initializeClient(Settings settings);

    /**
     * Get username if authentication is required
     * 
     * @return the username
     */
    String getUsername();

    /**
     * Set username for authentication
     * 
     * @param username the username
     */
    void setUsername(String username);

    /**
     * Get password if authentication is required
     * 
     * @return the password
     */
    String getPassword();

    /**
     * Set password for authentication
     * 
     * @param password the password
     */
    void setPassword(String password);

}
