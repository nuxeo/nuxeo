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
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.nuxeo.elasticsearch.api.ESClientProvider;
import org.nuxeo.elasticsearch.config.ESClientInitializationDescriptor;
import org.nuxeo.elasticsearch.config.ElasticSearchRemoteConfig;

/**
 * @since 9.1
 */
public class ESDefaultClientProvider implements ESClientProvider {

    protected ESClientInitializationDescriptor clientConfig;
    protected ElasticSearchRemoteConfig serverConfig;

    @Override
    public void setClientConfig(ESClientInitializationDescriptor contribution) {
        this.clientConfig = contribution;
    }

    @Override
    public void setServerConfig(ElasticSearchRemoteConfig config) {
        this.serverConfig = config;
    }

    @Override
    public TransportClient getClient() {
        Settings settings = getSetting().build();
        TransportClient client = new PreBuiltTransportClient(settings);
        return client;
    }

    @Override
    public Settings.Builder getSetting() {
        return Settings.builder()
                .put("cluster.name", serverConfig.getClusterName())
                .put("client.transport.nodes_sampler_interval", serverConfig.getSamplerInterval())
                .put("client.transport.ping_timeout", serverConfig.getPingTimeout())
                .put("client.transport.ignore_cluster_name", serverConfig.isIgnoreClusterName())
                .put("client.transport.sniff", serverConfig.isClusterSniff());
    }
}
