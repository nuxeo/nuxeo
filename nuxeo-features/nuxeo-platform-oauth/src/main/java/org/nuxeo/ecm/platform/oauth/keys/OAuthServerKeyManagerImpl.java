/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.oauth.keys;

import java.util.UUID;

import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implements the {@link OAuthServerKeyManager} interface. Manages an extention point to configure RSA Key Pair.
 * Shindig/Nuxeo HMAC shared secret is dynamically generated at startup time (and shared between Nuxeo OAUth Filter and
 * Shindig directly in memory).
 *
 * @author tiry
 */
public class OAuthServerKeyManagerImpl extends DefaultComponent implements OAuthServerKeyManager {

    protected ServerKeyDescriptor serverKeyDescriptor;

    public static final String XP_SERVER_KEY = "serverKeyPair";

    protected NuxeoOAuthConsumer consumer;

    protected String internalKey;

    protected String internalSecret;

    @Override
    public void activate(ComponentContext context) {
        // generate the random secret used between Shindig and Nuxeo
        internalKey = "nuxeo4shindig-" + UUID.randomUUID().toString();
        internalSecret = UUID.randomUUID().toString();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (XP_SERVER_KEY.equals(extensionPoint)) {
            serverKeyDescriptor = (ServerKeyDescriptor) contribution;
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {

        if (XP_SERVER_KEY.equals(extensionPoint)) {
            serverKeyDescriptor = null;
        }
    }

    @Override
    public String getPublicKeyCertificate() {
        if (serverKeyDescriptor != null) {
            return serverKeyDescriptor.externalPublicCertificate;
        }
        return null;
    }

    @Override
    public String getBarePublicCertificate() {
        return stripOpenSSL(getPublicKeyCertificate());
    }

    @Override
    public String getPrivateKey() {
        if (serverKeyDescriptor != null) {
            return serverKeyDescriptor.externalPrivateKey;
        }
        return null;
    }

    @Override
    public String getBarePrivateKey() {
        return stripOpenSSL(getPrivateKey());
    }

    @Override
    public String getKeyName() {
        if (serverKeyDescriptor != null) {
            return serverKeyDescriptor.externalPrivateKeyName;
        }
        return null;
    }

    protected String stripOpenSSL(String key) {
        if (key == null) {
            return null;
        }
        return key.replaceAll("-----[A-Z ]*-----", "").replace("\n", "");
    }

    @Override
    public String getInternalKey() {
        return internalKey;
    }

    @Override
    public String getInternalSecret() {
        return internalSecret;
    }

    @Override
    public NuxeoOAuthConsumer getInternalConsumer() {
        if (consumer == null) {
            consumer = new InternalNuxeoOAuthConsumer(internalKey, internalSecret);
        }
        return consumer;
    }

    protected class InternalNuxeoOAuthConsumer extends NuxeoOAuthConsumer {

        private static final long serialVersionUID = 1L;

        public InternalNuxeoOAuthConsumer(String consumerKey, String consumerSecret) {
            super(null, consumerKey, consumerSecret, null);
            signedFetchSupport = NuxeoOAuthConsumer.SIGNEDFETCH_OPENSOCIAL_VIEWER;
        }
    }
}
