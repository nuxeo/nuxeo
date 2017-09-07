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
// import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.nuxeo.elasticsearch.core.ESDefaultClientProvider;

/**
 * @since 9.1
 */
public class ESSshieldClientProvider extends ESDefaultClientProvider {

    @Override
    public Settings.Builder getSetting() {
        Settings.Builder builder = super.getSetting();
        builder.put("shield.user", clientConfig.getUsername() + ":" + clientConfig.getPassword());
        String sslKeystorePath = clientConfig.getSslKeystorePath();
        String sslKeystorePassword = clientConfig.getSslKeystorePassword();
        if (sslKeystorePath != null && sslKeystorePassword != null) {
            builder.put("shield.ssl.keystore.path", sslKeystorePath)
                    .put("shield.ssl.keystore.password", sslKeystorePassword)
                    .put("shield.transport.ssl", "true");
        }
        return super.getSetting();
    }

    @Override
    public TransportClient getClient() {
        // TransportClient client = new PreBuiltTransportClient(getSetting().build(), ShieldPlugin.class);
        // return client;
        return null;
    }
}
