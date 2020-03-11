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

package org.nuxeo.ecm.platform.oauth.providers;

import org.nuxeo.ecm.core.api.DocumentModel;

import net.oauth.OAuthServiceProvider;

public class NuxeoOAuthServiceProvider extends OAuthServiceProvider {

    private static final long serialVersionUID = 1L;

    public static final String SCHEMA = "oauthServiceProvider";

    protected String gadgetUrl;

    protected String serviceName;

    protected String keyType;

    protected String consumerKey;

    protected String consumerSecret;

    protected String publicKey;

    protected String description;

    protected boolean enabled = true;

    protected boolean readOnly = false;

    protected Long id;

    public NuxeoOAuthServiceProvider(String requestTokenURL, String userAuthorizationURL, String accessTokenURL) {
        super(requestTokenURL, userAuthorizationURL, accessTokenURL);
    }

    public NuxeoOAuthServiceProvider(Long id, String gadgetUrl, String serviceName, String consumerKey,
            String consumerSecret, String publicKey) {
        super(null, null, null);
        this.id = id;
        this.gadgetUrl = gadgetUrl;
        this.serviceName = serviceName;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
        this.publicKey = publicKey;
        this.readOnly = true;
    }

    public static NuxeoOAuthServiceProvider createFromDirectoryEntry(DocumentModel entry) {

        String requestTokenURL = (String) entry.getProperty(SCHEMA, "requestTokenURL");
        String userAuthorizationURL = (String) entry.getProperty(SCHEMA, "userAuthorizationURL");
        String accessTokenURL = (String) entry.getProperty(SCHEMA, "accessTokenURL");

        NuxeoOAuthServiceProvider provider = new NuxeoOAuthServiceProvider(requestTokenURL, userAuthorizationURL,
                accessTokenURL);

        provider.consumerKey = (String) entry.getProperty(SCHEMA, "consumerKey");
        provider.consumerSecret = (String) entry.getProperty(SCHEMA, "consumerSecret");
        provider.gadgetUrl = (String) entry.getProperty(SCHEMA, "gadgetUrl");
        provider.id = (Long) entry.getProperty(SCHEMA, "id");
        provider.keyType = (String) entry.getProperty(SCHEMA, "keyType");
        provider.publicKey = (String) entry.getProperty(SCHEMA, "publicKey");
        provider.serviceName = (String) entry.getProperty(SCHEMA, "serviceName");
        Boolean enabledFlag = (Boolean) entry.getProperty(SCHEMA, "enabled");
        if (Boolean.FALSE.equals(enabledFlag)) {
            provider.enabled = false;
        }
        return provider;
    }

    protected DocumentModel asDocumentModel(DocumentModel entry) {

        entry.setProperty(SCHEMA, "gadgetUrl", gadgetUrl);
        entry.setProperty(SCHEMA, "serviceName", serviceName);
        entry.setProperty(SCHEMA, "keyType", keyType);
        entry.setProperty(SCHEMA, "consumerKey", consumerKey);
        entry.setProperty(SCHEMA, "consumerSecret", consumerSecret);
        entry.setProperty(SCHEMA, "publicKey", publicKey);
        entry.setProperty(SCHEMA, "requestTokenURL", requestTokenURL);
        entry.setProperty(SCHEMA, "accessTokenURL", accessTokenURL);
        entry.setProperty(SCHEMA, "userAuthorizationURL", userAuthorizationURL);
        entry.setProperty(SCHEMA, "enabled", Boolean.valueOf(enabled));
        return entry;
    }

    public String getGadgetUrl() {
        return gadgetUrl;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public Long getId() {
        return id;
    }

    public String getRequestTokenUR() {
        return requestTokenURL;
    }

    public String getUserAuthorizationURL() {
        return userAuthorizationURL;
    }

    public String getAccessTokenURL() {
        return accessTokenURL;
    }

    public String getDescription() {
        return description;
    }

    public boolean isReadOnly() {
        return readOnly;
    }
}
