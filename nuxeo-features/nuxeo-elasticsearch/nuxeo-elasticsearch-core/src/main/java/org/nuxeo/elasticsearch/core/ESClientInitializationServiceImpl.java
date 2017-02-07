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

package org.nuxeo.elasticsearch.core;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.nuxeo.elasticsearch.api.ESClientInitializationService;
import org.nuxeo.elasticsearch.config.ElasticSearchRemoteConfig;

/**
 * @since 9.1
 */
public class ESClientInitializationServiceImpl implements ESClientInitializationService {

    protected String username;

    protected String password;

    @Override
    public Settings initializeSettings(ElasticSearchRemoteConfig config) {
        return initializeSettingsBuilder(config).build();
    }

    @Override
    public TransportClient initializeClient(Settings settings) {
        return initializeClientBuilder().settings(settings).build();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    protected Settings.Builder initializeSettingsBuilder(ElasticSearchRemoteConfig config) {
        Settings.Builder builder = Settings.settingsBuilder()
                                           .put("cluster.name", config.getClusterName())
                                           .put("client.transport.nodes_sampler_interval", config.getSamplerInterval())
                                           .put("client.transport.ping_timeout", config.getPingTimeout())
                                           .put("client.transport.ignore_cluster_name", config.isIgnoreClusterName())
                                           .put("client.transport.sniff", config.isClusterSniff());
        return builder;
    }

    protected TransportClient.Builder initializeClientBuilder() {
        return TransportClient.builder();
    }
}
