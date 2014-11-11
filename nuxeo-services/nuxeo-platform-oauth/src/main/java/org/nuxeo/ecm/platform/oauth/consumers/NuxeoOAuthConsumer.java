/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.oauth.consumers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

import net.oauth.OAuth;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;
import net.oauth.signature.RSA_SHA1;
import net.oauth.signature.pem.PEMReader;

/**
 * Represents a application that uses OAuth to consume a Web Service from Nuxeo.
 * This class holds informations such and keys and name for a consumer
 * application. The simple mapping to DocumentModel is also provided to make
 * storage in SQL Directory easier.
 *
 * @author tiry
 *
 */
public class NuxeoOAuthConsumer extends OAuthConsumer {

    private static final long serialVersionUID = 1L;

    public static final String ALLOW_SIGNEDFETCH = "allowSignedFetch";

    public static final String SIGNEDFETCH_NONE = "none";
    public static final String SIGNEDFETCH_OPENSOCIAL_VIEWER = "opensocial:viewer";
    public static final String SIGNEDFETCH_OPENSOCIAL_OWNER = "opensocial:owner";
    public static final String SIGNEDFETCH_DEDICATED_USER = "nuxeo:user";

    protected String publicKey = null;
    protected String description = null;
    protected String signedFetchSupport = SIGNEDFETCH_NONE;
    protected String dedicatedLogin = null;

    protected boolean enabled = true;

    protected boolean allowBypassVerifier = false;

    public static final String SCHEMA = "oauthConsumer";

    protected static final Log log = LogFactory.getLog(NuxeoOAuthConsumer.class);

    public static NuxeoOAuthConsumer createFromDirectoryEntry(
            DocumentModel entry, String keyType) throws ClientException {
        String callbackURL = (String) entry.getProperty(SCHEMA, "callbackURL");
        String consumerKey = (String) entry.getProperty(SCHEMA, "consumerKey");
        String consumerSecret = (String) entry.getProperty(SCHEMA,
                "consumerSecret");
        String rsaKey = (String) entry.getProperty(SCHEMA, "publicKey");

        NuxeoOAuthConsumer consumer = new NuxeoOAuthConsumer(callbackURL, consumerKey, consumerSecret,null);

        if (OAuth.RSA_SHA1.equals(keyType)) {
            if (rsaKey!=null) {
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
            consumer.enabled=false;
        }

        Boolean allowBypassVerifierFlag = (Boolean) entry.getProperty(SCHEMA, "allowBypassVerifier");
        if (Boolean.TRUE.equals(allowBypassVerifierFlag)) {
            consumer.allowBypassVerifier=true;
        }

        return consumer;
    }

    public NuxeoOAuthConsumer(String callbackURL, String consumerKey,
            String consumerSecret, OAuthServiceProvider serviceProvider) {
        super(callbackURL, consumerKey, consumerSecret, serviceProvider);
    }

    protected DocumentModel asDocumentModel(DocumentModel entry)
            throws ClientException {
        entry.setProperty(SCHEMA, "callbackURL", callbackURL);
        entry.setProperty(SCHEMA, "consumerKey", consumerKey);
        entry.setProperty(SCHEMA, "consumerSecret", consumerSecret);

        entry.setProperty(SCHEMA, "publicKey", publicKey);
        entry.setProperty(SCHEMA, "description", description);
        entry.setProperty(SCHEMA, "signedFetchSupport", signedFetchSupport);
        entry.setProperty(SCHEMA, "dedicatedLogin", dedicatedLogin);
        entry.setProperty(SCHEMA, "enabled", Boolean.valueOf(enabled));
        entry.setProperty(SCHEMA, "allowBypassVerifier",
                Boolean.valueOf(allowBypassVerifier));
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

        if (signedFetchSupport==null || SIGNEDFETCH_NONE.equals(signedFetchSupport)) {
            return false;
        }
        if (SIGNEDFETCH_DEDICATED_USER.equals(signedFetchSupport) && dedicatedLogin==null) {
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
        if (type==null || OAuth.HMAC_SHA1.equals(type)) {
            return consumerSecret;
        } else if (OAuth.RSA_SHA1.equals(type)) {
            return "";
        } else {
            log.error("Unknonw type of key :" + type);
            return null;
        }
    }

    public boolean allowBypassVerifier() {
        return allowBypassVerifier;
    }

}
