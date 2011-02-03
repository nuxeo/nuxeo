/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Implements the {@link OAuthServerKeyManager} interface.
 * Manage on extensition point to configure RSA Key Pair.
 * Shindig/Nuxeo HMAC shared secret is dynamically generated at startup time
 * (and shared between Nuxeo OAUth Filter and Shindig directly in memory)
 *
 * @author tiry
 *
 */
public class OAuthServerKeyManagerImpl extends DefaultComponent implements OAuthServerKeyManager {

    protected ServerKeyDescriptor serverKeyDescriptor=null;

    public static final String XP_SERVER_KEY ="serverKeyPair";

    protected NuxeoOAuthConsumer consumer =null;

    protected String internalKey=null;

    protected String internalSecret=null;

    @Override
    public void activate(ComponentContext context) throws Exception {
        // generate the random secret used between Shindig and Nuxeo
        internalKey = "nuxeo4shindig-"+ UUID.randomUUID().toString();
        internalSecret = UUID.randomUUID().toString();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {

        if (XP_SERVER_KEY.equals(extensionPoint)) {
            serverKeyDescriptor = (ServerKeyDescriptor) contribution;
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {

        if (XP_SERVER_KEY.equals(extensionPoint)) {
            serverKeyDescriptor = null;
        }
    }

    @Override
    public String getPublicKeyCertificate() {
        if (serverKeyDescriptor!=null) {
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
        if (serverKeyDescriptor!=null) {
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
        if (serverKeyDescriptor!=null) {
            return serverKeyDescriptor.externalPrivateKeyName;
        }
        return null;
    }

    protected String stripOpenSSL(String key) {
        if (key==null) {
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
        if (consumer==null) {
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

