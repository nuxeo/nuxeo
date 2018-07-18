/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.consumers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;

import net.oauth.OAuth;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import net.oauth.signature.RSA_SHA1;
import net.oauth.signature.pem.PEMReader;

/**
 * Represents a application that uses OAuth to consume a Web Service from Nuxeo. This class holds informations such and
 * keys and name for a consumer application. The simple mapping to DocumentModel is also provided to make storage in SQL
 * Directory easier.
 *
 * @author tiry
 */
public class NuxeoOAuthConsumer extends OAuthConsumer {

    public static final String ALLOW_SIGNEDFETCH = "allowSignedFetch";

    public static final String SIGNEDFETCH_NONE = "none";

    public static final String SIGNEDFETCH_OPENSOCIAL_VIEWER = "opensocial:viewer";

    public static final String SIGNEDFETCH_OPENSOCIAL_OWNER = "opensocial:owner";

    public static final String SIGNEDFETCH_DEDICATED_USER = "nuxeo:user";

    public static final String SCHEMA = "oauthConsumer";

    protected static final Log log = LogFactory.getLog(NuxeoOAuthConsumer.class);

    private static final long serialVersionUID = 1L;

    protected String publicKey;

    protected String description;

    protected String signedFetchSupport = SIGNEDFETCH_NONE;

    protected String dedicatedLogin;

    protected boolean enabled = true;

    protected boolean allowBypassVerifier = false;

    public static NuxeoOAuthConsumer createFromDirectoryEntry(DocumentModel entry, String keyType)
            {
        String callbackURL = (String) entry.getProperty(SCHEMA, "callbackURL");
        String consumerKey = (String) entry.getProperty(SCHEMA, "consumerKey");
        String consumerSecret = (String) entry.getProperty(SCHEMA, "consumerSecret");
        String rsaKey = (String) entry.getProperty(SCHEMA, "publicKey");

        NuxeoOAuthConsumer consumer = new NuxeoOAuthConsumer(callbackURL, consumerKey, consumerSecret, null);

        if (OAuth.RSA_SHA1.equals(keyType)) {
            if (rsaKey != null) {
                if (rsaKey.contains(PEMReader.PUBLIC_X509_MARKER)) {
                    consumer.setProperty(RSA_SHA1.PUBLIC_KEY, rsaKey);
                } else {
                    consumer.setProperty(RSA_SHA1.X509_CERTIFICATE, rsaKey);
                }
            }
        }
        consumer.publicKey = rsaKey;
        consumer.description = (String) entry.getProperty(SCHEMA, "description");
        consumer.signedFetchSupport = (String) entry.getProperty(SCHEMA, "signedFetchSupport");
        consumer.dedicatedLogin = (String) entry.getProperty(SCHEMA, "dedicatedLogin");

        Boolean enabledFlag = (Boolean) entry.getProperty(SCHEMA, "enabled");
        if (Boolean.FALSE.equals(enabledFlag)) {
            consumer.enabled = false;
        }

        Boolean allowBypassVerifierFlag = (Boolean) entry.getProperty(SCHEMA, "allowBypassVerifier");
        if (Boolean.TRUE.equals(allowBypassVerifierFlag)) {
            consumer.allowBypassVerifier = true;
        }

        return consumer;
    }

    public NuxeoOAuthConsumer(String callbackURL, String consumerKey, String consumerSecret,
            OAuthServiceProvider serviceProvider) {
        super(callbackURL, consumerKey, consumerSecret, serviceProvider);
    }

    protected DocumentModel asDocumentModel(DocumentModel entry) {
        entry.setProperty(SCHEMA, "callbackURL", callbackURL);
        entry.setProperty(SCHEMA, "consumerKey", consumerKey);
        entry.setProperty(SCHEMA, "consumerSecret", consumerSecret);

        entry.setProperty(SCHEMA, "publicKey", publicKey);
        entry.setProperty(SCHEMA, "description", description);
        entry.setProperty(SCHEMA, "signedFetchSupport", signedFetchSupport);
        entry.setProperty(SCHEMA, "dedicatedLogin", dedicatedLogin);
        entry.setProperty(SCHEMA, "enabled", Boolean.valueOf(enabled));
        entry.setProperty(SCHEMA, "allowBypassVerifier", Boolean.valueOf(allowBypassVerifier));
        return entry;
    }

    public String getCallbackURL() {
        return callbackURL;
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

    public boolean allowSignedFetch() {
        if (signedFetchSupport == null || SIGNEDFETCH_NONE.equals(signedFetchSupport)) {
            return false;
        }
        if (SIGNEDFETCH_DEDICATED_USER.equals(signedFetchSupport) && dedicatedLogin == null) {
            return false;
        }
        return true;
    }

    public String getSignedFetchUser() {
        if (!allowSignedFetch()) {
            return null;
        }
        if (signedFetchSupport.startsWith(SIGNEDFETCH_DEDICATED_USER)) {
            return dedicatedLogin;
        } else {
            return signedFetchSupport;
        }
    }

    public String getDescription() {
        return description;
    }

    public String getSecret(String type) {
        if (type == null || OAuth.HMAC_SHA1.equals(type)) {
            return consumerSecret;
        } else if (OAuth.RSA_SHA1.equals(type)) {
            return "";
        } else {
            log.error("Unknown type of key :" + type);
            return null;
        }
    }

    public boolean allowBypassVerifier() {
        return allowBypassVerifier;
    }

}
