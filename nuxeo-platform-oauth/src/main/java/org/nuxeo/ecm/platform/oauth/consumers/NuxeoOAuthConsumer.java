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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

import net.oauth.OAuthConsumer;
import net.oauth.OAuthServiceProvider;

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

    public static final String DESCRIPTION = "description";

    public static final String ENABLED = "enabled";

    protected static final String SCHEMA = "oauthConsumer";

    public static NuxeoOAuthConsumer createFromDirectoryEntry(
            DocumentModel entry) throws ClientException {
        String callbackURL = (String) entry.getProperty(SCHEMA, "callbackURL");
        String consumerKey = (String) entry.getProperty(SCHEMA, "consumerKey");
        String consumerSecret = (String) entry.getProperty(SCHEMA,
                "consumerSecret");
        // XXX
        return new NuxeoOAuthConsumer(callbackURL, consumerKey, consumerSecret,
                null);
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
        // XXX

        return entry;
    }

    public boolean allowSignedFetch() {
        Object prop = getProperty(ALLOW_SIGNEDFETCH);
        if (prop == null) {
            return false;
        } else {
            return (Boolean) prop;
        }
    }

    public String getDescription() {
        Object prop = getProperty(DESCRIPTION);
        if (prop == null) {
            return null;
        } else {
            return (String) prop;
        }
    }

}
