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

package org.nuxeo.elasticsearch.shield;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.shield.ShieldPlugin;
import org.nuxeo.elasticsearch.config.ElasticSearchRemoteConfig;
import org.nuxeo.elasticsearch.core.ESClientInitializationServiceImpl;

/**
 * @since 9.1
 */
public class ShieldInitializationService extends ESClientInitializationServiceImpl {

    @Override
    protected Settings.Builder initializeSettingsBuilder(ElasticSearchRemoteConfig config) {

        Settings.Builder builder = super.initializeSettingsBuilder(config);

        builder.put("shield.user", getUsername() + ":" + getPassword());

        return builder;
    }

    @Override
    protected TransportClient.Builder initializeClientBuilder() {
        return super.initializeClientBuilder().addPlugin(ShieldPlugin.class);
    }
}
